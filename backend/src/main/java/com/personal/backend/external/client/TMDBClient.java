package com.personal.backend.external.client;

import com.personal.backend.external.model.MovieMetadata;
import com.personal.backend.external.model.TMDBResponse;
import com.personal.backend.external.model.TVShowMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import com.personal.backend.service.PerformanceMetricsService;

@Component
@Slf4j
public class TMDBClient {
    
    private final WebClient webClient;
    private final PerformanceMetricsService performanceMetricsService;
    
    @Value("${tmdb.api.base.url:https://api.themoviedb.org/3}")
    private String baseUrl;
    
    @Value("${tmdb.api.key:}")
    private String apiKey;
    
    @Value("${tmdb.api.timeout:10000}")
    private int timeoutMs;
    
    public TMDBClient(WebClient.Builder webClientBuilder, PerformanceMetricsService performanceMetricsService) {
        this.performanceMetricsService = performanceMetricsService;
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }
    
    public Optional<MovieMetadata> searchMovie(String title) {
        if (title == null || title.trim().isEmpty()) {
            log.warn("Cannot search for movie with empty title");
            return Optional.empty();
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("TMDB API key not configured, skipping movie search");
            return Optional.empty();
        }
        
        long startTime = System.currentTimeMillis();
        boolean success = false;
        int statusCode = 200;
        
        try {
            log.debug("Searching TMDB for movie title: {}", title);
            
            String encodedTitle = URLEncoder.encode(title.trim(), StandardCharsets.UTF_8);
            String url = String.format("%s/search/movie?api_key=%s&query=%s", baseUrl, apiKey, encodedTitle);
            
            TMDBResponse<MovieMetadata> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<TMDBResponse<MovieMetadata>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            if (response != null && response.hasResults()) {
                MovieMetadata movie = response.getFirstResult();
                log.info("Found movie metadata for title: {} -> {}", title, movie.getTitle());
                success = true;
                return Optional.of(movie);
            } else {
                log.info("No movie found for title: {}", title);
                success = true; // API call succeeded, just no results
                return Optional.empty();
            }
            
        } catch (WebClientResponseException e) {
            statusCode = e.getStatusCode().value();
            log.error("Error searching TMDB for movie title: {}", title, e);
            return Optional.empty();
        } catch (Exception e) {
            statusCode = 500;
            log.error("Error searching TMDB for movie title: {}", title, e);
            return Optional.empty();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetricsService.recordExternalApiCall(
                "tmdb", "search_movie", Duration.ofMillis(duration), success, statusCode);
        }
    }
    
    public Optional<TVShowMetadata> searchTVShow(String title) {
        if (title == null || title.trim().isEmpty()) {
            log.warn("Cannot search for TV show with empty title");
            return Optional.empty();
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("TMDB API key not configured, skipping TV show search");
            return Optional.empty();
        }
        
        long startTime = System.currentTimeMillis();
        boolean success = false;
        int statusCode = 200;
        
        try {
            log.debug("Searching TMDB for TV show title: {}", title);
            
            String encodedTitle = URLEncoder.encode(title.trim(), StandardCharsets.UTF_8);
            String url = String.format("%s/search/tv?api_key=%s&query=%s", baseUrl, apiKey, encodedTitle);
            
            TMDBResponse<TVShowMetadata> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<TMDBResponse<TVShowMetadata>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            if (response != null && response.hasResults()) {
                TVShowMetadata tvShow = response.getFirstResult();
                log.info("Found TV show metadata for title: {} -> {}", title, tvShow.getName());
                success = true;
                return Optional.of(tvShow);
            } else {
                log.info("No TV show found for title: {}", title);
                success = true;
                return Optional.empty();
            }
            
        } catch (WebClientResponseException e) {
            statusCode = e.getStatusCode().value();
            log.error("Error searching TMDB for TV show title: {}", title, e);
            return Optional.empty();
        } catch (Exception e) {
            statusCode = 500;
            log.error("Error searching TMDB for TV show title: {}", title, e);
            return Optional.empty();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetricsService.recordExternalApiCall(
                "tmdb", "search_tv", Duration.ofMillis(duration), success, statusCode);
        }
    }
    
    public Optional<MovieMetadata> getMovieDetails(Long movieId) {
        if (movieId == null) {
            log.warn("Cannot get movie details with null ID");
            return Optional.empty();
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("TMDB API key not configured, skipping movie details");
            return Optional.empty();
        }
        
        long startTime = System.currentTimeMillis();
        boolean success = false;
        int statusCode = 200;
        
        try {
            log.debug("Getting TMDB movie details for ID: {}", movieId);
            
            String url = String.format("%s/movie/%d?api_key=%s", baseUrl, movieId, apiKey);
            
            MovieMetadata movie = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(MovieMetadata.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            if (movie != null) {
                log.info("Found movie details for ID: {} -> {}", movieId, movie.getTitle());
                success = true;
                return Optional.of(movie);
            } else {
                log.info("No movie found for ID: {}", movieId);
                success = true;
                return Optional.empty();
            }
            
        } catch (WebClientResponseException e) {
            statusCode = e.getStatusCode().value();
            log.error("Error getting TMDB movie details for ID: {}", movieId, e);
            return Optional.empty();
        } catch (Exception e) {
            statusCode = 500;
            log.error("Error getting TMDB movie details for ID: {}", movieId, e);
            return Optional.empty();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetricsService.recordExternalApiCall(
                "tmdb", "movie_details", Duration.ofMillis(duration), success, statusCode);
        }
    }
    
    public Optional<TVShowMetadata> getTVShowDetails(Long tvShowId) {
        if (tvShowId == null) {
            log.warn("Cannot get TV show details with null ID");
            return Optional.empty();
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("TMDB API key not configured, skipping TV show details");
            return Optional.empty();
        }
        
        long startTime = System.currentTimeMillis();
        boolean success = false;
        int statusCode = 200;
        
        try {
            log.debug("Getting TMDB TV show details for ID: {}", tvShowId);
            
            String url = String.format("%s/tv/%d?api_key=%s", baseUrl, tvShowId, apiKey);
            
            TVShowMetadata tvShow = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(TVShowMetadata.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            if (tvShow != null) {
                log.info("Found TV show details for ID: {} -> {}", tvShowId, tvShow.getName());
                success = true;
                return Optional.of(tvShow);
            } else {
                log.info("No TV show found for ID: {}", tvShowId);
                success = true;
                return Optional.empty();
            }
            
        } catch (WebClientResponseException e) {
            statusCode = e.getStatusCode().value();
            log.error("Error getting TMDB TV show details for ID: {}", tvShowId, e);
            return Optional.empty();
        } catch (Exception e) {
            statusCode = 500;
            log.error("Error getting TMDB TV show details for ID: {}", tvShowId, e);
            return Optional.empty();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetricsService.recordExternalApiCall(
                "tmdb", "tv_details", Duration.ofMillis(duration), success, statusCode);
        }
    }
    
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            int statusCode = ex.getStatusCode().value();
            // Retry on server errors (5xx) and rate limiting (429)
            // TMDB free tier has rate limits
            return statusCode >= 500 || statusCode == 429;
        }
        // Retry on network errors
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.util.concurrent.TimeoutException;
    }
    
    public boolean isHealthy() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("TMDB API key not configured");
            return false;
        }
        
        try {
            String url = String.format("%s/configuration?api_key=%s", baseUrl, apiKey);
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            return response != null && response.contains("images");
            
        } catch (Exception e) {
            log.warn("TMDB health check failed", e);
            return false;
        }
    }
    
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    // Helper method to search for both movies and TV shows
    public Optional<Object> searchContent(String title, String type) {
        if ("movie".equalsIgnoreCase(type)) {
            return searchMovie(title).map(movie -> (Object) movie);
        } else if ("tv".equalsIgnoreCase(type) || "tvshow".equalsIgnoreCase(type)) {
            return searchTVShow(title).map(tvShow -> (Object) tvShow);
        } else {
            // Try both if type is not specified
            Optional<MovieMetadata> movie = searchMovie(title);
            if (movie.isPresent()) {
                return movie.map(m -> (Object) m);
            }
            return searchTVShow(title).map(tv -> (Object) tv);
        }
    }
}