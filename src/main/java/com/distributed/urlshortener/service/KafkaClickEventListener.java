package com.distributed.urlshortener.service;

import com.distributed.urlshortener.domain.event.ClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Decoupled Kafka Stream Consumer for Click Analytics.
 * Active only when Kafka is enabled (e.g. in Docker/Production profile).
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true")
public class KafkaClickEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaClickEventListener.class);
    private final AnalyticsConsumerService analyticsConsumerService;

    public KafkaClickEventListener(AnalyticsConsumerService analyticsConsumerService) {
        this.analyticsConsumerService = analyticsConsumerService;
    }

    /**
     * Kafka Listener consuming real-time click stream.
     */
    @KafkaListener(
            topics = "${app.kafka.click-topic:url-clicks}",
            groupId = "${spring.kafka.consumer.group-id:url-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeKafkaEvent(ClickEvent event) {
        if (event == null || event.getShortCode() == null) {
            return;
        }
        log.debug("Consumed Kafka click event for shortCode: {}", event.getShortCode());
        analyticsConsumerService.processClickEvent(event);
    }
}
