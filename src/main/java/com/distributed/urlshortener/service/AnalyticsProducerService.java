package com.distributed.urlshortener.service;

import com.distributed.urlshortener.domain.event.ClickEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous Event Producer for Click Analytics.
 * Decouples click event ingestion completely from the user redirect critical path.
 */
@Service
public class AnalyticsProducerService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsProducerService.class);

    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;
    private final String clickTopic;
    private final AnalyticsConsumerService directFallbackConsumer;
    private final boolean kafkaEnabled;
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4);

    @Autowired
    public AnalyticsProducerService(
            @Autowired(required = false) KafkaTemplate<String, ClickEvent> kafkaTemplate,
            @Autowired(required = false) AnalyticsConsumerService directFallbackConsumer,
            @Value("${app.kafka.click-topic:url-clicks}") String clickTopic,
            @Value("${spring.kafka.enabled:false}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.directFallbackConsumer = directFallbackConsumer;
        this.clickTopic = clickTopic;
        this.kafkaEnabled = kafkaEnabled;
    }

    public AnalyticsProducerService(
            KafkaTemplate<String, ClickEvent> kafkaTemplate,
            AnalyticsConsumerService directFallbackConsumer,
            String clickTopic) {
        this(kafkaTemplate, directFallbackConsumer, clickTopic, false);
    }

    /**
     * Extracts client analytics context and dispatches click event asynchronously to Kafka.
     */
    public void recordClickAsync(String shortCode, String originalUrl, HttpServletRequest request) {
        // Extract metadata non-blockingly
        String ip = extractClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";
        String referer = request != null ? request.getHeader("Referer") : "Direct";
        String country = request != null ? request.getHeader("CF-IPCountry") : null;
        if (country == null && request != null) {
            country = request.getHeader("X-Country-Code");
        }
        if (country == null) {
            country = determineCountryFromIp(ip);
        }

        String deviceType = detectDeviceType(userAgent);
        String browser = detectBrowser(userAgent);
        String os = detectOs(userAgent);

        ClickEvent event = new ClickEvent(
                UUID.randomUUID().toString(),
                shortCode,
                originalUrl,
                Instant.now(),
                ip,
                userAgent,
                referer,
                country,
                "Unknown",
                deviceType,
                browser,
                os
        );

        // Asynchronously publish to Kafka if Kafka is enabled
        if (kafkaEnabled && kafkaTemplate != null) {
            try {
                CompletableFuture<SendResult<String, ClickEvent>> future = kafkaTemplate.send(clickTopic, shortCode, event);
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish click event to Kafka for shortCode '{}': {}", shortCode, ex.getMessage());
                        dispatchToDirectConsumer(event);
                    } else {
                        log.debug("Published click event to Kafka partition {} for shortCode '{}'",
                                result.getRecordMetadata().partition(), shortCode);
                    }
                });
                return;
            } catch (Exception e) {
                log.warn("KafkaTemplate send exception: {}. Using fallback consumer.", e.getMessage());
            }
        }

        // Direct async fallback consumer if Kafka broker is not present
        dispatchToDirectConsumer(event);
    }

    private void dispatchToDirectConsumer(ClickEvent event) {
        if (directFallbackConsumer != null) {
            asyncExecutor.submit(() -> {
                try {
                    directFallbackConsumer.processClickEvent(event);
                } catch (Exception ex) {
                    log.error("Fallback click processing error: {}", ex.getMessage());
                }
            });
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private String detectDeviceType(String userAgent) {
        if (userAgent == null) return "Desktop";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        }
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        }
        if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")) {
            return "Bot";
        }
        return "Desktop";
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null) return "Other";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/") && !ua.contains("edg/")) return "Chrome";
        if (ua.contains("safari/") && !ua.contains("chrome/")) return "Safari";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("curl/") || ua.contains("postman")) return "API Client";
        return "Other";
    }

    private String detectOs(String userAgent) {
        if (userAgent == null) return "Other";
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("macintosh") || ua.contains("mac os x")) return "macOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("linux")) return "Linux";
        return "Other";
    }

    private String determineCountryFromIp(String ip) {
        if (ip.startsWith("127.") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return "Local";
        }
        // Deterministic hash assignment for synthetic/test IP geo simulation
        int hash = Math.abs(ip.hashCode()) % 5;
        return switch (hash) {
            case 0 -> "US";
            case 1 -> "IN";
            case 2 -> "DE";
            case 3 -> "GB";
            default -> "CA";
        };
    }
}
