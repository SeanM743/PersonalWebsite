package com.personal.backend.controller;

import com.personal.backend.model.NewsArticle;
import com.personal.backend.model.NewsCategory;
import com.personal.backend.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    
    // Temp hardcoded user ID until proper Auth context is fully passed
    private static final Long DEFAULT_USER_ID = 1L;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNews() {
        // Trigger refresh if needed
        newsService.refreshNewsForUser(DEFAULT_USER_ID, false);
        
        List<NewsCategory> categories = newsService.getAllCategories(DEFAULT_USER_ID);
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> categoryData = categories.stream().map(cat -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cat.getId());
            map.put("topic", cat.getTopic());
            map.put("tab", cat.getTab());
            map.put("lastFetchedAt", cat.getLastFetchedAt());
            map.put("articles", newsService.getArticlesForCategory(cat.getId()));
            return map;
        }).collect(Collectors.toList());
        
        result.put("categories", categoryData);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<NewsCategory>> listCategories() {
        return ResponseEntity.ok(newsService.getAllCategories(DEFAULT_USER_ID));
    }

    @PostMapping("/categories")
    public ResponseEntity<NewsCategory> addCategory(@RequestBody Map<String, String> payload) {
        String topic = payload.get("topic");
        String tab = payload.get("tab");
        String query = payload.get("query"); // Optional explicit query
        
        if (topic == null || topic.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        NewsCategory category = newsService.addCategory(DEFAULT_USER_ID, topic, tab, query);
        // Removed immediate refresh trigger as per user request
        
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        newsService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<NewsCategory> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String topic = payload.get("topic");
        String tab = payload.get("tab");
        String query = payload.get("query");
        
        NewsCategory updated = newsService.updateCategory(id, topic, tab, query);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        newsService.deleteArticle(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshNews(@RequestParam(required = false, defaultValue = "false") boolean force) {
        newsService.refreshNewsForUser(DEFAULT_USER_ID, force);
        return ResponseEntity.ok().build();
    }
}
