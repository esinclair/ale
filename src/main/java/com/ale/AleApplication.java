package com.ale;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;

@SpringBootApplication
@EnableCaching
public class AleApplication {

    /** Maximum time-to-live for cached DEKs (seconds). */
    private static final long DEK_CACHE_TTL_SECONDS = 20;

    public static void main(String[] args) {
        SpringApplication.run(AleApplication.class, args);
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("dek-cache");
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(DEK_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                .maximumSize(1000)
        );
        return manager;
    }
}
