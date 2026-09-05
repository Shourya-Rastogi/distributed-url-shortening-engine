package com.distributed.urlshortener.service;

import com.distributed.urlshortener.domain.document.ClickEventDocument;
import com.distributed.urlshortener.domain.document.UrlAnalyticsDocument;
import com.distributed.urlshortener.domain.dto.UrlAnalyticsResponse;
import com.distributed.urlshortener.repository.ClickEventMongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service providing real-time analytics query views.
 */
@Service
public class AnalyticsQueryService {

    private final AnalyticsConsumerService consumerService;
    private final ClickEventMongoRepository clickEventRepository;

    public AnalyticsQueryService(
            AnalyticsConsumerService consumerService,
            @Autowired(required = false) ClickEventMongoRepository clickEventRepository) {
        this.consumerService = consumerService;
        this.clickEventRepository = clickEventRepository;
    }

    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        UrlAnalyticsDocument doc = consumerService.getAnalytics(shortCode);
        if (doc == null) {
            return null;
        }

        UrlAnalyticsResponse response = new UrlAnalyticsResponse();
        response.setShortCode(doc.getId());
        response.setOriginalUrl(doc.getOriginalUrl());
        response.setTotalClicks(doc.getTotalClicks());
        response.setUniqueVisitors(doc.getUniqueIps() != null ? doc.getUniqueIps().size() : 0);
        response.setClicksByDate(doc.getClicksByDate());
        response.setClicksByHour(doc.getClicksByHour());
        response.setClicksByCountry(doc.getClicksByCountry());
        response.setClicksByDevice(doc.getClicksByDevice());
        response.setClicksByBrowser(doc.getClicksByBrowser());
        response.setClicksByReferer(doc.getClicksByReferer());
        response.setFirstClickedAt(doc.getFirstClickedAt());
        response.setLastClickedAt(doc.getLastClickedAt());

        return response;
    }

    public List<ClickEventDocument> getRecentClicks(String shortCode, int limit) {
        if (clickEventRepository != null) {
            try {
                return clickEventRepository.findByShortCodeOrderByTimestampDesc(shortCode, PageRequest.of(0, limit));
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
