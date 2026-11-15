package com.viecultures.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * In-memory caching configuration backed by Caffeine to reduce round-trips to Supabase.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_JOURNEYS_LIST = "journeysList";
    public static final String CACHE_JOURNEY_BY_ID = "journeyById";
    public static final String CACHE_JOURNEY_OPTIONS = "journeyOptions";
    public static final String CACHE_POST_OPTIONS = "postOptions";
    public static final String CACHE_POSTS_CATALOG = "postsCatalog";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CACHE_JOURNEYS_LIST,
                CACHE_JOURNEY_BY_ID,
                CACHE_JOURNEY_OPTIONS,
                CACHE_POST_OPTIONS,
                CACHE_POSTS_CATALOG
        );
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(500)
        );
        return manager;
    }
}
