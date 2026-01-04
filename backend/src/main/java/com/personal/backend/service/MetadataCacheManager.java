package com.personal.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personal.backend.external.model.BookMetadata;
import com.personal.backend.external.model.MovieMetadata;
import com.personal.backend.external.model.TVShowMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MetadataCacheManager {
    
    private final Cache<String, BookMetadata> bookMetadataCache;
    private final Cache<String, MovieMetadata> movieMetadataCache;
    private final Cache<String, TVShowMetadata> tvShowMetadataCache;
    private final Cache<String, String> contentCache;
    
    @Value("${content.cache.ttl.hours:24}")
    private int cacheTtlHours;
    
    public MetadataCacheManager(@Value("${content.cache.ttl.hours:24}") int cacheTtlHours) {
        this.cacheTtlHours = cacheTtlHours;
        
        this.bookMetadataCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(cacheTtlHours, TimeUnit.HOURS)
                .recordStats()
                .build();
                
        this.movieMetadataCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(cacheTtlHours, TimeUnit.HOURS)
                .recordStats()
                .build();
                
        this.tvShowMetadataCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(cacheTtlHours, TimeUnit.HOURS)
                .recordStats()
                .build();
                
        this.contentCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
                
        log.info("Initialized metadata cache manager with TTL: {} hours", cacheTtlHours);
    }
    
    // Book metadata caching
    public Optional<BookMetadata> getCachedBookMetadata(String key) {
        BookMetadata cached = bookMetadataCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache hit for book metadata key: {}", key);
            return Optional.of(cached);
        }
        log.debug("Cache miss for book metadata key: {}", key);
        return Optional.empty();
    }
    
    public void cacheBookMetadata(String key, BookMetadata metadata) {
        if (key != null && metadata != null) {
            bookMetadataCache.put(key, metadata);
            log.debug("Cached book metadata for key: {} -> {}", key, metadata.getTitle());
        }
    }
    
    public String generateBookCacheKey(String title, String author, String isbn) {
        StringBuilder keyBuilder = new StringBuilder();
        
        if (title != null && !title.trim().isEmpty()) {
            keyBuilder.append("title:").append(title.trim().toLowerCase());
        }
        
        if (author != null && !author.trim().isEmpty()) {
            if (keyBuilder.length() > 0) keyBuilder.append("|");
            keyBuilder.append("author:").append(author.trim().toLowerCase());
        }
        
        if (isbn != null && !isbn.trim().isEmpty()) {
            if (keyBuilder.length() > 0) keyBuilder.append("|");
            keyBuilder.append("isbn:").append(isbn.trim().replaceAll("[^0-9X]", ""));
        }
        
        return keyBuilder.toString();
    }
    
    // Movie metadata caching
    public Optional<MovieMetadata> getCachedMovieMetadata(String key) {
        MovieMetadata cached = movieMetadataCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache hit for movie metadata key: {}", key);
            return Optional.of(cached);
        }
        log.debug("Cache miss for movie metadata key: {}", key);
        return Optional.empty();
    }
    
    public void cacheMovieMetadata(String key, MovieMetadata metadata) {
        if (key != null && metadata != null) {
            movieMetadataCache.put(key, metadata);
            log.debug("Cached movie metadata for key: {} -> {}", key, metadata.getTitle());
        }
    }
    
    public String generateMovieCacheKey(String title) {
        return title != null ? "movie:" + title.trim().toLowerCase() : null;
    }
    
    // TV Show metadata caching
    public Optional<TVShowMetadata> getCachedTVShowMetadata(String key) {
        TVShowMetadata cached = tvShowMetadataCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache hit for TV show metadata key: {}", key);
            return Optional.of(cached);
        }
        log.debug("Cache miss for TV show metadata key: {}", key);
        return Optional.empty();
    }
    
    public void cacheTVShowMetadata(String key, TVShowMetadata metadata) {
        if (key != null && metadata != null) {
            tvShowMetadataCache.put(key, metadata);
            log.debug("Cached TV show metadata for key: {} -> {}", key, metadata.getName());
        }
    }
    
    public String generateTVShowCacheKey(String title) {
        return title != null ? "tv:" + title.trim().toLowerCase() : null;
    }
    
    // General content caching
    public Optional<String> getCachedContent(String key) {
        String cached = contentCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache hit for content key: {}", key);
            return Optional.of(cached);
        }
        log.debug("Cache miss for content key: {}", key);
        return Optional.empty();
    }
    
    public void cacheContent(String key, String content) {
        if (key != null && content != null) {
            contentCache.put(key, content);
            log.debug("Cached content for key: {}", key);
        }
    }
    
    // Cache management
    public void invalidateBookCache(String key) {
        bookMetadataCache.invalidate(key);
        log.debug("Invalidated book cache for key: {}", key);
    }
    
    public void invalidateMovieCache(String key) {
        movieMetadataCache.invalidate(key);
        log.debug("Invalidated movie cache for key: {}", key);
    }
    
    public void invalidateTVShowCache(String key) {
        tvShowMetadataCache.invalidate(key);
        log.debug("Invalidated TV show cache for key: {}", key);
    }
    
    public void invalidateContentCache(String key) {
        contentCache.invalidate(key);
        log.debug("Invalidated content cache for key: {}", key);
    }
    
    public void clearAllCaches() {
        bookMetadataCache.invalidateAll();
        movieMetadataCache.invalidateAll();
        tvShowMetadataCache.invalidateAll();
        contentCache.invalidateAll();
        log.info("Cleared all metadata caches");
    }
    
    // Cache statistics
    public CacheStats getCacheStats() {
        return CacheStats.builder()
                .bookCacheSize(bookMetadataCache.estimatedSize())
                .movieCacheSize(movieMetadataCache.estimatedSize())
                .tvShowCacheSize(tvShowMetadataCache.estimatedSize())
                .contentCacheSize(contentCache.estimatedSize())
                .bookHitRate(bookMetadataCache.stats().hitRate())
                .movieHitRate(movieMetadataCache.stats().hitRate())
                .tvShowHitRate(tvShowMetadataCache.stats().hitRate())
                .contentHitRate(contentCache.stats().hitRate())
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class CacheStats {
        private long bookCacheSize;
        private long movieCacheSize;
        private long tvShowCacheSize;
        private long contentCacheSize;
        private double bookHitRate;
        private double movieHitRate;
        private double tvShowHitRate;
        private double contentHitRate;
        
        public long getTotalCacheSize() {
            return bookCacheSize + movieCacheSize + tvShowCacheSize + contentCacheSize;
        }
        
        public double getOverallHitRate() {
            return (bookHitRate + movieHitRate + tvShowHitRate + contentHitRate) / 4.0;
        }
    }
}