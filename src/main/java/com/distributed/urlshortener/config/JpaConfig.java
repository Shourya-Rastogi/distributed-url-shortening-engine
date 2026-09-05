package com.distributed.urlshortener.config;

import com.distributed.urlshortener.repository.UrlMappingRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration with explicit repository scoping.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.distributed.urlshortener.repository",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {UrlMappingRepository.class}
        )
)
public class JpaConfig {
}
