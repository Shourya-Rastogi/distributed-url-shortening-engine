package com.distributed.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Distributed URL Shortening & Analytics Service.
 * Horizontally scalable stateless Spring Boot application.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
