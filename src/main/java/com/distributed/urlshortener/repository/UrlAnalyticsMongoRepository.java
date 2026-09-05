package com.distributed.urlshortener.repository;

import com.distributed.urlshortener.domain.document.UrlAnalyticsDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB Repository for aggregated URL analytics.
 */
@Repository
public interface UrlAnalyticsMongoRepository extends MongoRepository<UrlAnalyticsDocument, String> {
}
