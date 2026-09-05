package com.distributed.urlshortener.service;

import com.distributed.urlshortener.domain.document.ClickEventDocument;
import com.distributed.urlshortener.domain.document.UrlAnalyticsDocument;
import com.distributed.urlshortener.domain.event.ClickEvent;
import com.distributed.urlshortener.repository.ClickEventMongoRepository;
import com.distributed.urlshortener.repository.UrlAnalyticsMongoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decoupled Kafka Stream Consumer for Click Analytics.
 * Ingests events asynchronously from Kafka topic 'url-clicks' and persists
 * granular time-series and aggregated facet metrics into MongoDB.
 */
@Service
public class AnalyticsConsumerService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumerService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH").withZone(ZoneOffset.UTC);

    private final ClickEventMongoRepository clickEventRepository;
    private final UrlAnalyticsMongoRepository urlAnalyticsRepository;
    private final MongoTemplate mongoTemplate;

    // In-memory fallback map if MongoDB is offline or in standalone embedded mode
    private final Map<String, UrlAnalyticsDocument> localAnalyticsMap = new ConcurrentHashMap<>();

    public AnalyticsConsumerService(
            @Autowired(required = false) ClickEventMongoRepository clickEventRepository,
            @Autowired(required = false) UrlAnalyticsMongoRepository urlAnalyticsRepository,
            @Autowired(required = false) MongoTemplate mongoTemplate) {
        this.clickEventRepository = clickEventRepository;
        this.urlAnalyticsRepository = urlAnalyticsRepository;
        this.mongoTemplate = mongoTemplate;
    }



    /**
     * Process click event into MongoDB time-series and aggregated metrics.
     */
    public void processClickEvent(ClickEvent event) {
        String shortCode = event.getShortCode();
        String dateKey = DATE_FORMATTER.format(event.getTimestamp());
        String hourKey = HOUR_FORMATTER.format(event.getTimestamp());
        String safeReferer = sanitizeKey(event.getReferer());
        String safeBrowser = sanitizeKey(event.getBrowser());
        String safeDevice = sanitizeKey(event.getDeviceType());
        String safeCountry = sanitizeKey(event.getCountry());

        // 1. Persist to MongoDB if available
        if (mongoTemplate != null) {
            try {
                // A. Save Raw Event Document
                ClickEventDocument rawDoc = new ClickEventDocument(
                        event.getShortCode(),
                        event.getOriginalUrl(),
                        event.getTimestamp(),
                        event.getIpAddress(),
                        event.getUserAgent(),
                        event.getReferer(),
                        event.getCountry(),
                        event.getCity(),
                        event.getDeviceType(),
                        event.getBrowser(),
                        event.getOs()
                );
                if (clickEventRepository != null) {
                    clickEventRepository.save(rawDoc);
                } else {
                    mongoTemplate.save(rawDoc);
                }

                // B. Atomic Upsert Aggregated Counters in MongoDB
                Query query = new Query(Criteria.where("_id").is(shortCode));
                Update update = new Update()
                        .setOnInsert("originalUrl", event.getOriginalUrl())
                        .setOnInsert("firstClickedAt", event.getTimestamp())
                        .set("lastClickedAt", event.getTimestamp())
                        .inc("totalClicks", 1)
                        .addToSet("uniqueIps", event.getIpAddress())
                        .inc("clicksByDate." + dateKey, 1)
                        .inc("clicksByHour." + hourKey, 1)
                        .inc("clicksByCountry." + safeCountry, 1)
                        .inc("clicksByDevice." + safeDevice, 1)
                        .inc("clicksByBrowser." + safeBrowser, 1)
                        .inc("clicksByReferer." + safeReferer, 1);

                mongoTemplate.upsert(query, update, UrlAnalyticsDocument.class);
            } catch (Exception e) {
                log.warn("MongoDB persistence error: {}. Updating local analytics map.", e.getMessage());
                updateLocalAnalytics(event, dateKey, hourKey, safeReferer, safeBrowser, safeDevice, safeCountry);
            }
        } else {
            // 2. Local Fallback Analytics Store
            updateLocalAnalytics(event, dateKey, hourKey, safeReferer, safeBrowser, safeDevice, safeCountry);
        }
    }

    private void updateLocalAnalytics(ClickEvent event, String dateKey, String hourKey,
                                      String referer, String browser, String device, String country) {
        localAnalyticsMap.compute(event.getShortCode(), (k, doc) -> {
            if (doc == null) {
                doc = new UrlAnalyticsDocument(event.getShortCode(), event.getOriginalUrl());
                doc.setFirstClickedAt(event.getTimestamp());
            }
            doc.setTotalClicks(doc.getTotalClicks() + 1);
            doc.getUniqueIps().add(event.getIpAddress());
            doc.setLastClickedAt(event.getTimestamp());

            doc.getClicksByDate().merge(dateKey, 1L, Long::sum);
            doc.getClicksByHour().merge(hourKey, 1L, Long::sum);
            doc.getClicksByCountry().merge(country, 1L, Long::sum);
            doc.getClicksByDevice().merge(device, 1L, Long::sum);
            doc.getClicksByBrowser().merge(browser, 1L, Long::sum);
            doc.getClicksByReferer().merge(referer, 1L, Long::sum);

            return doc;
        });
    }

    public UrlAnalyticsDocument getAnalytics(String shortCode) {
        if (mongoTemplate != null) {
            try {
                UrlAnalyticsDocument doc = mongoTemplate.findById(shortCode, UrlAnalyticsDocument.class);
                if (doc != null) {
                    return doc;
                }
            } catch (Exception e) {
                log.debug("MongoDB findById error: {}", e.getMessage());
            }
        }
        return localAnalyticsMap.get(shortCode);
    }

    private String sanitizeKey(String key) {
        if (key == null || key.isBlank()) return "Unknown";
        return key.replace(".", "_").replace("$", "_");
    }
}
