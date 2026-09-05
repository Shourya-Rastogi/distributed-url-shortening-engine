package com.distributed.urlshortener.config;

import com.distributed.urlshortener.repository.ClickEventMongoRepository;
import com.distributed.urlshortener.repository.UrlAnalyticsMongoRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB Configuration with repository scoping.
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(
        basePackages = "com.distributed.urlshortener.repository",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {ClickEventMongoRepository.class, UrlAnalyticsMongoRepository.class}
        )
)
public class MongoConfig {
}
