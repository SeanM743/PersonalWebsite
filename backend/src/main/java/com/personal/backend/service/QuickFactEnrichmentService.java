package com.personal.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.backend.model.QuickFact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuickFactEnrichmentService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${openlibrary.api.base.url:https://openlibrary.org}")
    private String openLibraryBaseUrl;
    
    @Value("${tmdb.api.key:}")
    private String tmdbApiKey;
    
    @Value("${tmdb.api.base.url:https://api.themoviedb.org/3}")
    private String tmdbBaseUrl;
    
    public QuickFact enrichQuickFact(QuickFact quickFact) {
        if (quickFact.getIsEnriched() != null && quickFact.getIsEnriched()) {
            log.debug("QuickFact {} is already enriched, skipping", quickFact.getKey());
            return quickFact;
        }
        
        try {
            switch (quickFact.getCategory().toLowerCase()) {
                case "reading":
                case "book":
                    return enrichBookData(quickFact);
                case "recently watched":
                case "movie":
                case "film":
                    return enrichMovieData(quickFact);
                case "listening to":
                case "music":
                    return enrichMusicData(quickFact);
                case "next trip":
                case "location":
                    return enrichLocationData(quickFact);
                default:
                    log.debug("No enrichment available for category: {}", quickFact.getCategory());
                    return quickFact;
            }
        } catch (Exception e) {
            log.error("Error enriching quick fact {}: {}", quickFact.getKey(), e.getMessage());
            return quickFact;
        }
    }
    
    private QuickFact enrichBookData(QuickFact quickFact) {
        try {
            String value = quickFact.getValue();
            WebClient webClient = webClientBuilder.build();
            JsonNode response = null;
            
            // Strategy 1: Try "Title by Author" format first
            String[] parts = value.split(" by ", 2);
            if (parts.length >= 2) {
                String title = parts[0].trim();
                String author = parts[1].trim();
                
                log.debug("Searching with title='{}' and author='{}'", title, author);
                String searchUrl = openLibraryBaseUrl + "/search.json?title=" + 
                                 title.replace(" ", "+") + "&author=" + author.replace(" ", "+") + "&limit=1";
                
                response = webClient.get()
                        .uri(searchUrl)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();
            }
            
            // Strategy 2: If no results or no "by" format, try general search
            if (response == null || !response.has("docs") || response.get("docs").size() == 0) {
                log.debug("Trying general search for: '{}'", value);
                String searchUrl = openLibraryBaseUrl + "/search.json?q=" + 
                                 value.replace(" ", "+") + "&limit=3";
                
                response = webClient.get()
                        .uri(searchUrl)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();
            }
            
            // Strategy 3: If still no results, try title-only search
            if (response == null || !response.has("docs") || response.get("docs").size() == 0) {
                log.debug("Trying title-only search for: '{}'", value);
                String searchUrl = openLibraryBaseUrl + "/search.json?title=" + 
                                 value.replace(" ", "+") + "&limit=3";
                
                response = webClient.get()
                        .uri(searchUrl)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();
            }
            
            if (response != null && response.has("docs") && response.get("docs").size() > 0) {
                JsonNode book = response.get("docs").get(0);
                
                Map<String, Object> metadata = new HashMap<>();
                
                // Extract title and author from the result (more reliable than user input)
                String resultTitle = book.has("title") ? book.get("title").asText() : value;
                String resultAuthor = "";
                if (book.has("author_name") && book.get("author_name").size() > 0) {
                    resultAuthor = book.get("author_name").get(0).asText();
                }
                
                metadata.put("title", resultTitle);
                if (!resultAuthor.isEmpty()) {
                    metadata.put("author", resultAuthor);
                }
                
                // Update the quickFact value with the properly formatted result
                if (!resultAuthor.isEmpty()) {
                    quickFact.setValue(resultTitle + " by " + resultAuthor);
                } else {
                    quickFact.setValue(resultTitle);
                }
                
                if (book.has("isbn")) {
                    String isbn = book.get("isbn").get(0).asText();
                    quickFact.setExternalId(isbn);
                    metadata.put("isbn", isbn);
                    
                    // Get cover image
                    quickFact.setImageUrl("https://covers.openlibrary.org/b/isbn/" + isbn + "-M.jpg");
                }
                
                if (book.has("first_publish_year")) {
                    metadata.put("publishYear", book.get("first_publish_year").asInt());
                }
                
                if (book.has("publisher")) {
                    metadata.put("publisher", book.get("publisher").get(0).asText());
                }
                
                if (book.has("key")) {
                    String bookKey = book.get("key").asText();
                    quickFact.setSourceUrl(openLibraryBaseUrl + bookKey);
                }
                
                quickFact.setMetadata(objectMapper.writeValueAsString(metadata));
                quickFact.setIsEnriched(true);
                
                log.debug("Successfully enriched book data for: {} -> {}", value, quickFact.getValue());
            } else {
                log.debug("No book data found for: {}", value);
            }
            
        } catch (Exception e) {
            log.error("Error enriching book data: {}", e.getMessage());
        }
        
        return quickFact;
    }
    
    private QuickFact enrichMovieData(QuickFact quickFact) {
        try {
            if (tmdbApiKey == null || tmdbApiKey.trim().isEmpty()) {
                log.debug("TMDB API key not configured, skipping movie enrichment");
                return quickFact;
            }
            
            String movieTitle = quickFact.getValue();
            
            // Search TMDB API
            WebClient webClient = webClientBuilder.build();
            String searchUrl = tmdbBaseUrl + "/search/movie?api_key=" + tmdbApiKey + 
                             "&query=" + movieTitle.replace(" ", "+");
            
            JsonNode response = webClient.get()
                    .uri(searchUrl)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            if (response != null && response.has("results") && response.get("results").size() > 0) {
                JsonNode movie = response.get("results").get(0);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("title", movieTitle);
                
                if (movie.has("id")) {
                    quickFact.setExternalId(movie.get("id").asText());
                }
                
                if (movie.has("poster_path") && !movie.get("poster_path").isNull()) {
                    quickFact.setImageUrl("https://image.tmdb.org/t/p/w300" + movie.get("poster_path").asText());
                }
                
                if (movie.has("release_date")) {
                    metadata.put("releaseDate", movie.get("release_date").asText());
                }
                
                if (movie.has("overview")) {
                    metadata.put("overview", movie.get("overview").asText());
                    quickFact.setDescription(movie.get("overview").asText());
                }
                
                if (movie.has("vote_average")) {
                    metadata.put("rating", movie.get("vote_average").asDouble());
                }
                
                quickFact.setSourceUrl("https://www.themoviedb.org/movie/" + movie.get("id").asText());
                quickFact.setMetadata(objectMapper.writeValueAsString(metadata));
                quickFact.setIsEnriched(true);
                
                log.debug("Enriched movie data for: {}", movieTitle);
            }
            
        } catch (Exception e) {
            log.error("Error enriching movie data: {}", e.getMessage());
        }
        
        return quickFact;
    }
    
    private QuickFact enrichMusicData(QuickFact quickFact) {
        try {
            // For music, we'll parse "Song by Artist" format
            String value = quickFact.getValue();
            String[] parts = value.split(" by ", 2);
            
            Map<String, Object> metadata = new HashMap<>();
            
            if (parts.length >= 2) {
                String song = parts[0].trim();
                String artist = parts[1].trim();
                metadata.put("song", song);
                metadata.put("artist", artist);
            } else {
                // Assume it's just an artist name
                metadata.put("artist", value);
            }
            
            // For now, we'll just structure the data without external API calls
            // You could integrate with Spotify API, Last.fm, etc. here
            quickFact.setMetadata(objectMapper.writeValueAsString(metadata));
            quickFact.setIsEnriched(true);
            
            log.debug("Enriched music data for: {}", value);
            
        } catch (Exception e) {
            log.error("Error enriching music data: {}", e.getMessage());
        }
        
        return quickFact;
    }
    
    private QuickFact enrichLocationData(QuickFact quickFact) {
        try {
            String location = quickFact.getValue();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("location", location);
            
            // For now, we'll just structure the data
            // You could integrate with Google Places API, weather APIs, etc. here
            quickFact.setMetadata(objectMapper.writeValueAsString(metadata));
            quickFact.setIsEnriched(true);
            
            log.debug("Enriched location data for: {}", location);
            
        } catch (Exception e) {
            log.error("Error enriching location data: {}", e.getMessage());
        }
        
        return quickFact;
    }
}