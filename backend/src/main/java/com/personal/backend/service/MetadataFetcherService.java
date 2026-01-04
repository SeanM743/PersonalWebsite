package com.personal.backend.service;

import com.personal.backend.external.client.OpenLibraryClient;
import com.personal.backend.external.client.TMDBClient;
import com.personal.backend.external.model.BookMetadata;
import com.personal.backend.external.model.MovieMetadata;
import com.personal.backend.external.model.TVShowMetadata;
import com.personal.backend.model.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataFetcherService {
    
    private final OpenLibraryClient openLibraryClient;
    private final TMDBClient tmdbClient;
    private final MetadataCacheManager cacheManager;
    
    public Optional<BookMetadata> fetchBookMetadata(String title, String author, String isbn) {
        log.debug("Fetching book metadata for title: {}, author: {}, isbn: {}", title, author, isbn);
        
        // Generate cache key
        String cacheKey = cacheManager.generateBookCacheKey(title, author, isbn);
        if (cacheKey == null || cacheKey.isEmpty()) {
            log.warn("Cannot generate cache key for book metadata");
            return Optional.empty();
        }
        
        // Check cache first
        Optional<BookMetadata> cached = cacheManager.getCachedBookMetadata(cacheKey);
        if (cached.isPresent()) {
            return cached;
        }
        
        // Try different search strategies
        Optional<BookMetadata> result = Optional.empty();
        
        // 1. Try ISBN search first (most specific)
        if (isbn != null && !isbn.trim().isEmpty()) {
            result = openLibraryClient.searchByISBN(isbn);
        }
        
        // 2. Try title and author search
        if (result.isEmpty() && title != null && author != null) {
            result = openLibraryClient.searchByTitleAndAuthor(title, author);
        }
        
        // 3. Try title-only search
        if (result.isEmpty() && title != null) {
            result = openLibraryClient.searchByTitle(title);
        }
        
        // 4. Try author-only search (less useful but might find something)
        if (result.isEmpty() && author != null) {
            result = openLibraryClient.searchByAuthor(author);
        }
        
        // Cache the result (even if empty to avoid repeated API calls)
        if (result.isPresent()) {
            cacheManager.cacheBookMetadata(cacheKey, result.get());
            log.info("Successfully fetched and cached book metadata: {}", result.get().getTitle());
        } else {
            // Cache a placeholder to indicate "not found" for a shorter time
            log.info("No book metadata found for: title={}, author={}, isbn={}", title, author, isbn);
        }
        
        return result;
    }
    
    public Optional<MovieMetadata> fetchMovieMetadata(String title) {
        log.debug("Fetching movie metadata for title: {}", title);
        
        if (title == null || title.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Generate cache key
        String cacheKey = cacheManager.generateMovieCacheKey(title);
        
        // Check cache first
        Optional<MovieMetadata> cached = cacheManager.getCachedMovieMetadata(cacheKey);
        if (cached.isPresent()) {
            return cached;
        }
        
        // Fetch from TMDB
        Optional<MovieMetadata> result = tmdbClient.searchMovie(title);
        
        // Cache the result
        if (result.isPresent()) {
            cacheManager.cacheMovieMetadata(cacheKey, result.get());
            log.info("Successfully fetched and cached movie metadata: {}", result.get().getTitle());
        } else {
            log.info("No movie metadata found for title: {}", title);
        }
        
        return result;
    }
    
    public Optional<TVShowMetadata> fetchTVShowMetadata(String title) {
        log.debug("Fetching TV show metadata for title: {}", title);
        
        if (title == null || title.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Generate cache key
        String cacheKey = cacheManager.generateTVShowCacheKey(title);
        
        // Check cache first
        Optional<TVShowMetadata> cached = cacheManager.getCachedTVShowMetadata(cacheKey);
        if (cached.isPresent()) {
            return cached;
        }
        
        // Fetch from TMDB
        Optional<TVShowMetadata> result = tmdbClient.searchTVShow(title);
        
        // Cache the result
        if (result.isPresent()) {
            cacheManager.cacheTVShowMetadata(cacheKey, result.get());
            log.info("Successfully fetched and cached TV show metadata: {}", result.get().getName());
        } else {
            log.info("No TV show metadata found for title: {}", title);
        }
        
        return result;
    }
    
    public Optional<Object> fetchMetadataByType(String title, MediaType mediaType, String creator, String externalId) {
        log.debug("Fetching metadata for title: {}, type: {}, creator: {}, externalId: {}", 
                 title, mediaType, creator, externalId);
        
        switch (mediaType) {
            case BOOK:
                return fetchBookMetadata(title, creator, externalId).map(book -> (Object) book);
                
            case MOVIE:
                return fetchMovieMetadata(title).map(movie -> (Object) movie);
                
            case TV_SHOW:
                return fetchTVShowMetadata(title).map(tvShow -> (Object) tvShow);
                
            case PODCAST:
            case SONG:
            case ALBUM:
            case MAGAZINE:
                // These would require additional API integrations (Spotify, etc.)
                // For now, return empty and rely on manual entry
                log.info("Metadata fetching not yet implemented for media type: {}", mediaType);
                return Optional.empty();
                
            default:
                log.warn("Unknown media type: {}", mediaType);
                return Optional.empty();
        }
    }
    
    public boolean isMetadataAvailable(MediaType mediaType) {
        switch (mediaType) {
            case BOOK:
                return openLibraryClient.isHealthy();
            case MOVIE:
            case TV_SHOW:
                return tmdbClient.isHealthy();
            case PODCAST:
            case SONG:
            case ALBUM:
            case MAGAZINE:
                return false; // Not yet implemented
            default:
                return false;
        }
    }
    
    public MetadataServiceStatus getServiceStatus() {
        return MetadataServiceStatus.builder()
                .openLibraryHealthy(openLibraryClient.isHealthy())
                .tmdbHealthy(tmdbClient.isHealthy())
                .tmdbConfigured(tmdbClient.isConfigured())
                .cacheStats(cacheManager.getCacheStats())
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MetadataServiceStatus {
        private boolean openLibraryHealthy;
        private boolean tmdbHealthy;
        private boolean tmdbConfigured;
        private MetadataCacheManager.CacheStats cacheStats;
        
        public boolean isFullyOperational() {
            return openLibraryHealthy && tmdbHealthy && tmdbConfigured;
        }
        
        public boolean isPartiallyOperational() {
            return openLibraryHealthy || (tmdbHealthy && tmdbConfigured);
        }
    }
}