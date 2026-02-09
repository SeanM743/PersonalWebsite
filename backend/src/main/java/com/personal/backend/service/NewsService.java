package com.personal.backend.service;

import com.personal.backend.model.NewsArticle;
import com.personal.backend.model.NewsCategory;
import com.personal.backend.repository.NewsArticleRepository;
import com.personal.backend.repository.NewsCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final NewsCategoryRepository categoryRepository;
    private final NewsArticleRepository articleRepository;
    
    @Value("${news.api.key:}")
    private String newsApiKey;
    
    @Value("${news.gemini.key:}")
    private String geminiApiKey;
    
    // Limits
    private static final int MAX_ARTICLES_PER_CATEGORY = 10;
    private static final long REFRESH_INTERVAL_HOURS = 4;



    @Transactional
    public List<NewsCategory> getAllCategories(Long userId) {
        return categoryRepository.findByUserId(userId);
    }
    
    @Transactional
    public List<NewsArticle> getArticlesForCategory(Long categoryId) {
        return articleRepository.findByCategoryIdOrderByPublishedAtDesc(categoryId);
    }

    @javax.annotation.PostConstruct
    public void init() {
        log.info("NewsService initialized. Gemini Key present: {}", (geminiApiKey != null && !geminiApiKey.isEmpty()));
        log.info("NewsAPI Key present: {}", (newsApiKey != null && !newsApiKey.isEmpty()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        // No longer enforcing static categories
        log.info("Checking for stale news on startup...");
        try {
           refreshNewsForUser(1L, false); 
        } catch (Exception e) {
           log.error("Failed to refresh news on startup", e);
        }
    }

    @Transactional
    public NewsCategory addCategory(Long userId, String topic, String tab, String explicitQuery) {
        String displayTopic = topic;
        String searchQuery = explicitQuery;
        
        // Validation for tab
        if (tab == null || !List.of("Financial", "Sports", "Politics", "Misc").contains(tab)) {
            tab = "Misc"; // Default
        }

        // Only use LLM if query is missing (legacy/fallback mode)
        if ((searchQuery == null || searchQuery.isEmpty()) && geminiApiKey != null && !geminiApiKey.isEmpty()) {
            try {
                String prompt = String.format(
                    "You are a NewsAPI query expert. Convert this user input into a JSON object with two fields:\n" +
                    "1. 'label': A short, clean display name (max 3 words).\n" +
                    "2. 'query': A SIMPLE NewsAPI search query (2-5 keywords max). NewsAPI works best with short keyword queries, NOT long sentences. Use OR for alternatives.\n\n" +
                    "IMPORTANT: Keep the query SHORT. Long queries return 0 results.\n" +
                    "Context: User is adding this to the '%s' tab.\n" +
                    "User Input: \"%s\"\n" +
                    "Example Input: \"Stocks: AMZN, GOOG\"\n" +
                    "Example JSON: {\"label\": \"Tech Stocks\", \"query\": \"AMZN OR GOOG stock\"}", 
                    tab, topic);
                
                String jsonResponse = callGemini(prompt);
                
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                
                 // Clean json text
                 jsonResponse = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
                 int start = jsonResponse.indexOf("{");
                 int end = jsonResponse.lastIndexOf("}");
                 if (start >= 0 && end > start) {
                     jsonResponse = jsonResponse.substring(start, end + 1);
                 }
                 
                Map<String, String> result = mapper.readValue(jsonResponse, Map.class);
                if (result.containsKey("label")) displayTopic = result.get("label");
                if (result.containsKey("query")) searchQuery = result.get("query");
            } catch (Exception e) {
                log.error("Failed to parse topic with LLM", e);
                searchQuery = topic; 
            }
        } else if (searchQuery == null || searchQuery.isEmpty()) {
            searchQuery = topic; 
        }

        if (categoryRepository.existsByUserIdAndTopic(userId, displayTopic)) {
            throw new IllegalArgumentException("Category already exists");
        }
        
        NewsCategory category = NewsCategory.builder()
                .userId(userId)
                .topic(displayTopic)
                .tab(tab)
                .searchQuery(searchQuery) 
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        articleRepository.deleteByCategoryId(categoryId);
        categoryRepository.deleteById(categoryId);
    }

    @Transactional
    public NewsCategory updateCategory(Long categoryId, String topic, String tab, String query) {
        return categoryRepository.findById(categoryId).map(category -> {
            if (topic != null && !topic.trim().isEmpty()) {
                category.setTopic(topic.trim());
            }
            if (tab != null && List.of("Financial", "Sports", "Politics", "Misc").contains(tab)) {
                category.setTab(tab);
            }
            if (query != null) {
                category.setSearchQuery(query.isEmpty() ? category.getTopic() : query);
            }
            // Clear articles since the query/topic may have changed
            articleRepository.deleteByCategoryId(categoryId);
            category.setLastFetchedAt(null); // Force refresh on next load
            return categoryRepository.save(category);
        }).orElse(null);
    }

    @Transactional
    public void deleteArticle(Long articleId) {
        articleRepository.deleteById(articleId);
    }

    @Transactional
    public void refreshNewsForUser(Long userId, boolean force) {
        List<NewsCategory> categories = categoryRepository.findByUserId(userId);
        for (NewsCategory category : categories) {
            if (force || shouldRefresh(category)) {
                fetchAndProcessNews(category);
            }
        }
    }

    private boolean shouldRefresh(NewsCategory category) {
        if (category.getLastFetchedAt() == null) return true;
        return category.getLastFetchedAt().isBefore(LocalDateTime.now().minusHours(REFRESH_INTERVAL_HOURS));
    }

    private void fetchAndProcessNews(NewsCategory category) {
        if (newsApiKey == null || newsApiKey.isEmpty()) {
            log.error("ABORTING FETCH: NEWS_API_KEY is missing!");
            return;
        }

        log.info("Fetching news for: {} ", category.getTopic());
        
        try {
            String queryToUse = category.getSearchQuery();
            
            Map response = WebClient.create("https://newsapi.org")
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/everything")
                            .queryParam("q", queryToUse)
                            .queryParam("apiKey", newsApiKey)
                            .queryParam("language", "en")
                            .queryParam("sortBy", "publishedAt")
                            .queryParam("pageSize", 15)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && "ok".equals(response.get("status"))) {
                List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
                
                log.info("Found {} raw articles for {}", articles != null ? articles.size() : 0, category.getTopic());
                
                // Clear old articles
                articleRepository.deleteByCategoryId(category.getId());
                
                if (articles != null) {
                    int savedCount = 0;
                    for (Map<String, Object> articleData : articles) {
                        if (savedCount >= MAX_ARTICLES_PER_CATEGORY) break;
                        if (processSingleArticle(category, articleData)) {
                            savedCount++;
                        }
                    }
                }
                
                category.setLastFetchedAt(LocalDateTime.now());
                categoryRepository.save(category);
            } else {
                 log.error("NewsAPI error for {}: {}", category.getTopic(), (response != null ? response.get("status") : "null"));
            }
        } catch (Exception e) {
            log.error("Error fetching news for " + category.getTopic(), e);
        }
    }

    private boolean processSingleArticle(NewsCategory category, Map<String, Object> articleData) {
        String title = (String) articleData.get("title");
        if (title == null || title.equals("[Removed]")) return false; // Skip removed articles

        String url = (String) articleData.get("url");
        String description = (String) articleData.get("description");
        String content = (String) articleData.get("content");
        String urlToImage = (String) articleData.get("urlToImage");
        Map<String, Object> sourceMap = (Map<String, Object>) articleData.get("source");
        String sourceName = sourceMap != null ? (String) sourceMap.get("name") : "Unknown";
        String publishedAtStr = (String) articleData.get("publishedAt");

        LocalDateTime publishedAt = LocalDateTime.now();
        try {
            if (publishedAtStr != null) {
                publishedAt = LocalDateTime.parse(publishedAtStr, DateTimeFormatter.ISO_DATE_TIME);
            }
        } catch (Exception ignored) {}

        // LLM Relevance Check & Summarization
        String summary = description;
        String fullWriteup = content;
        
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            try {
                String promptContext = "";
                String promptRefusal = "If the article is not strictly relevant to " + category.getTopic() + ", or if it is a general consumer review (e.g. movie reviews, top 10 lists), return JSON { \"relevant\": false }.";
                
                String tab = category.getTab() != null ? category.getTab() : "Misc";
                
                switch (tab) {
                    case "Financial":
                        promptContext = "This article must DIRECTLY discuss one of: S&P 500, Dow Jones, Nasdaq, Bitcoin/crypto, USD/EUR, Gold prices, OR the specific topic '" + category.getTopic() + "'. " +
                                       "REJECT if it's a product review, 'best deals' list, or general commentary not directly about the topic.";
                        break;
                    case "Sports":
                        promptContext = "This article must DIRECTLY mention the team/entity '" + category.getTopic() + "' - including players, coaches, games, or official team news. " +
                                       "REJECT if it's about a different team from the same city/region, a different sport entirely, or only tangentially mentions the topic.";
                        break;
                    case "Politics":
                        promptContext = "This article must be about US Congress, White House, elections, OR the specific political topic '" + category.getTopic() + "'. " +
                                       "REJECT opinion pieces without news value.";
                        break;
                    case "Entertainment":
                        promptContext = "This article must DIRECTLY discuss '" + category.getTopic() + "' - the specific show, movie, artist, or game. " +
                                       "REJECT 'best of' lists, generic recommendations, or articles that only briefly mention the topic.";
                        break;
                    case "Science":
                        promptContext = "This article should cover scientific breakthroughs, space exploration, research discoveries, climate science, physics, biology, or technology announcements. " +
                                       "If a specific topic is provided ('" + category.getTopic() + "'), prioritize articles directly about it. " +
                                       "REJECT opinion pieces, product reviews, and general tech gadget news unless it involves a genuine scientific advancement.";
                        break;
                    case "Misc":
                    default:
                        promptContext = "Focus on major scientific discoveries, technology breakthroughs, or significant cultural events related to '" + category.getTopic() + "'.";
                        promptRefusal = "Return { \"relevant\": true } unless it is spam."; // Lenient for Misc
                        break;
                }

                String prompt = String.format(
                        "Analyze this news article for the topic '%s' (Category: %s). %s\n\n" +
                        "%s\n\n" +
                        "If relevant, provide a JSON object with: \n" +
                        "1. \"relevant\": true\n" +
                        "2. \"summary\": \"A concise 3-sentence summary highlighting the specific %s impact.\"\n" +
                        "3. \"writeup\": \"A detailed 2-paragraph analysis.\"\n\n" +
                        "Title: %s\nContent: %s\nDescription: %s", 
                        category.getTopic(), tab, promptContext, promptRefusal, category.getTopic(), title, content, description);
                
                String jsonResponse = callGemini(prompt);
                
                if (jsonResponse.isEmpty()) {
                    log.warn("Empty response from Gemini for article: {}, using raw data", title);
                    // Continue with raw data as fallback (do NOT return early)
                } else {

                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> result = mapper.readValue(jsonResponse, Map.class);
                    
                    Boolean relevant = (Boolean) result.get("relevant");
                    if (Boolean.FALSE.equals(relevant)) {
                        log.info("Skipping irrelevant article: {} (Category: {})", title, category.getTopic());
                        return false;
                    }
                    
                    summary = (String) result.getOrDefault("summary", description);
                    fullWriteup = (String) result.getOrDefault("writeup", content);
                }
                
            } catch (Exception e) {
                log.error("Gemini processing failed for article: " + title, e);
                // Fallback to accepting it if LLM fails, or reject? 
                // Accepting is safer to avoid empty feeds on API errors.
            }
        }

        NewsArticle article = NewsArticle.builder()
                .category(category)
                .title(title)
                .url(url)
                .imageUrl(urlToImage)
                .source(sourceName)
                .publishedAt(publishedAt)
                .summary(summary)
                .content(fullWriteup)
                .build();
        
        articleRepository.save(article);
        return true;
    }
    
    private String callGemini(String prompt) {
        // Using Gemini 1.5 Flash
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt + "\n\nOutput strictly valid JSON.")
                ))
            ),
             "generationConfig", Map.of(
                 "response_mime_type", "application/json"
             )
        );

        try {
            Map response = WebClient.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("key", geminiApiKey)
                            .build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
                    
            if (response != null) {
                 List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                 if (candidates != null && !candidates.isEmpty()) {
                     Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                     List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                     return (String) parts.get(0).get("text");
                 }
            }
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("Gemini API Error: {} - Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Gemini Call Failed", e);
        }
        return "";
    }
}    

