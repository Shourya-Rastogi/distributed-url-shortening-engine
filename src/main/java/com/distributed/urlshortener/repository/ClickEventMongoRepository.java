package com.distributed.urlshortener.repository;

import com.distributed.urlshortener.domain.document.ClickEventDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB Repository for raw click events.
 */
@Repository
public interface ClickEventMongoRepository extends MongoRepository<ClickEventDocument, String> {

    List<ClickEventDocument> findByShortCodeOrderByTimestampDesc(String shortCode, Pageable pageable);

    long countByShortCode(String shortCode);
}
