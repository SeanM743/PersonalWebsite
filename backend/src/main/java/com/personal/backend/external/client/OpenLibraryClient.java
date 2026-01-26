package com.personal.backend.external.client;

import com.personal.backend.external.model.BookMetadata;
import com.personal.backend.external.model.OpenLibraryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class OpenLibraryClient {
    
    private final WebClient webClient;
    
    @Value("${openlibrary.api.base.url:https://openlibrary.org}")
    private String baseUrl;
    
    @Value("${openlibrary.api.timeout:10000}")
    private int timeoutMs;
    
    public OpenLibraryClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }
    
    public Optional<BookMetadata> searchByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            log.warn("Cannot search for book with empty title");
            return Optional.empty();
        }
        
        try {
            log.debug("Searching OpenLibrary for book title: {}", title);
            
            String encodedTitle = URLEncoder.encode(title.trim(), StandardCharsets.UTF_8);
            String url = String.format("%s/search.json?title=%s&limit=1", baseUrl, encodedTitle);
            
            OpenLibraryResponse response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(OpenLibraryResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            if (response != null && response.hasResults()) {
                BookMetadata book = response.getFirstResult();
                log.info("Found book metadata for title: {} -> {}", title, book.getTitle());
                return Optional.of(book);

            } else {
                // If specific title search fails, try generic query search which is more forgiving
                log.info("No book found for exact title: {}, trying generic search", title);
                return searchByGeneralQuery(title);
            }
            
        } catch (Exception e) {
            log.error("Error searching OpenLibrary for title: {}", title, e);
            return Optional.empty();
        }
    }
    
    public Optional<BookMetadata> searchByISBN(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            log.warn("Cannot search for book with empty ISBN");
            return Optional.empty();
        }
        
        try {
            log.debug("Searching OpenLibrary for ISBN: {}", isbn);
            
            String cleanIsbn = isbn.replaceAll("[^0-9X]", ""); // Remove hyphens and spaces
            String url = String.format("%s/api/books?bibkeys=ISBN:%s&format=json&jscmd=data", baseUrl, cleanIsbn);
            
            // OpenLibrary ISBN API returns a different format
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            if (response != null && !response.equals("{}")) {
                // Parse the ISBN response format
                BookMetadata book = parseISBNResponse(response, cleanIsbn);
                if (book != null) {
                    log.info("Found book metadata for ISBN: {} -> {}", isbn, book.getTitle());
                    return Optional.of(book);
                }
            }
            
            log.info("No book found for ISBN: {}", isbn);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Error searching OpenLibrary for ISBN: {}", isbn, e);
            return Optional.empty();
        }
    }
    
    public Optional<BookMetadata> searchByAuthor(String author) {
        if (author == null || author.trim().isEmpty()) {
            log.warn("Cannot search for book with empty author");
            return Optional.empty();
        }
        
        try {
            log.debug("Searching OpenLibrary for author: {}", author);
            
            String encodedAuthor = URLEncoder.encode(author.trim(), StandardCharsets.UTF_8);
            String url = String.format("%s/search.json?author=%s&limit=10", baseUrl, encodedAuthor);
            
            OpenLibraryResponse response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(OpenLibraryResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            if (response != null && response.hasResults()) {
                // Return the first book by this author
                BookMetadata book = response.getFirstResult();
                log.info("Found books by author: {} -> first result: {}", author, book.getTitle());
                return Optional.of(book);
            } else {
                log.info("No books found for author: {}", author);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Error searching OpenLibrary for author: {}", author, e);
            return Optional.empty();
        }
    }
    
    public Optional<BookMetadata> searchByTitleAndAuthor(String title, String author) {
        if ((title == null || title.trim().isEmpty()) && (author == null || author.trim().isEmpty())) {
            log.warn("Cannot search for book with empty title and author");
            return Optional.empty();
        }
        
        try {
            log.debug("Searching OpenLibrary for title: {} and author: {}", title, author);
            
            StringBuilder queryBuilder = new StringBuilder();
            
            if (title != null && !title.trim().isEmpty()) {
                queryBuilder.append("title=").append(URLEncoder.encode(title.trim(), StandardCharsets.UTF_8));
            }
            
            if (author != null && !author.trim().isEmpty()) {
                if (queryBuilder.length() > 0) {
                    queryBuilder.append("&");
                }
                queryBuilder.append("author=").append(URLEncoder.encode(author.trim(), StandardCharsets.UTF_8));
            }
            
            String url = String.format("%s/search.json?%s&limit=1", baseUrl, queryBuilder.toString());
            
            OpenLibraryResponse response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(OpenLibraryResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            if (response != null && response.hasResults()) {
                BookMetadata book = response.getFirstResult();
                log.info("Found book metadata for title: {} and author: {} -> {}", title, author, book.getTitle());
                return Optional.of(book);
            } else {
                log.info("No book found for title: {} and author: {}", title, author);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Error searching OpenLibrary for title: {} and author: {}", title, author, e);
            return Optional.empty();
        }
    }
    
    private Optional<BookMetadata> searchByGeneralQuery(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = String.format("%s/search.json?q=%s&limit=1", baseUrl, encodedQuery);
            
            OpenLibraryResponse response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(OpenLibraryResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            if (response != null && response.hasResults()) {
                BookMetadata book = response.getFirstResult();
                log.info("Found book metadata via generic search for: {} -> {}", query, book.getTitle());
                return Optional.of(book);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error exploring generic search for: {}", query, e);
            return Optional.empty();
        }
    }
    
    private BookMetadata parseISBNResponse(String jsonResponse, String isbn) {
        try {
            // This is a simplified parser for the ISBN response format
            // In a production system, you'd want to use a proper JSON parser
            if (jsonResponse.contains("\"title\"")) {
                return BookMetadata.builder()
                        .isbn(isbn)
                        .title("Book found via ISBN") // Placeholder - would need proper JSON parsing
                        .build();
            }
            return null;
        } catch (Exception e) {
            log.error("Error parsing ISBN response", e);
            return null;
        }
    }
    
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            int statusCode = ex.getStatusCode().value();
            // Retry on server errors (5xx) and rate limiting (429)
            return statusCode >= 500 || statusCode == 429;
        }
        // Retry on network errors
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.util.concurrent.TimeoutException;
    }
    
    public boolean isHealthy() {
        try {
            String url = baseUrl + "/search.json?title=test&limit=1";
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            return response != null;
            
        } catch (Exception e) {
            log.warn("OpenLibrary health check failed", e);
            return false;
        }
    }
}