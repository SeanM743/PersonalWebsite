package com.personal.backend.controller;

import com.personal.backend.dto.*;
import com.personal.backend.model.ActivityStatus;
import com.personal.backend.model.MediaType;
import com.personal.backend.model.QuickFact;
import com.personal.backend.model.TripStatus;
import com.personal.backend.model.TripType;
import com.personal.backend.service.MediaActivityService;
import com.personal.backend.service.QuickFactService;
import com.personal.backend.service.SocialMediaPostService;
import com.personal.backend.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {
    
    private final SocialMediaPostService postService;
    private final QuickFactService quickFactService;
    private final MediaActivityService mediaActivityService;
    private final TripService tripService;
    
    // Social Media Post endpoints
    @PostMapping("/posts")
    public ResponseEntity<ContentResponse<SocialMediaPostResponse>> createPost(
            @RequestBody SocialMediaPostRequest request) {
        ContentResponse<SocialMediaPostResponse> response = postService.createPost(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @PostMapping("/posts/ai")
    public ResponseEntity<ContentResponse<SocialMediaPostResponse>> createPostWithAI(
            @RequestParam String prompt) {
        ContentResponse<SocialMediaPostResponse> response = postService.createPostWithAI(prompt);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @GetMapping("/posts")
    public ResponseEntity<ContentResponse<List<SocialMediaPostResponse>>> getAllPosts() {
        ContentResponse<List<SocialMediaPostResponse>> response = postService.getAllPosts();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/posts/paginated")
    public ResponseEntity<ContentResponse<Page<SocialMediaPostResponse>>> getPostsPaginated(Pageable pageable) {
        ContentResponse<Page<SocialMediaPostResponse>> response = postService.getPostsPaginated(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/posts/{id}")
    public ResponseEntity<ContentResponse<SocialMediaPostResponse>> getPost(@PathVariable Long id) {
        ContentResponse<SocialMediaPostResponse> response = postService.getPost(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/posts/{id}")
    public ResponseEntity<ContentResponse<SocialMediaPostResponse>> updatePost(
            @PathVariable Long id, @RequestBody SocialMediaPostRequest request) {
        ContentResponse<SocialMediaPostResponse> response = postService.updatePost(id, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ContentResponse<Void>> deletePost(@PathVariable Long id) {
        ContentResponse<Void> response = postService.deletePost(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/posts/recent")
    public ResponseEntity<ContentResponse<List<SocialMediaPostResponse>>> getRecentPosts(
            @RequestParam(defaultValue = "24") int hours) {
        ContentResponse<List<SocialMediaPostResponse>> response = postService.getRecentPosts(hours);
        return ResponseEntity.ok(response);
    }
    
    // Quick Facts endpoints
    @PostMapping("/facts")
    public ResponseEntity<ContentResponse<QuickFact>> createOrUpdateQuickFact(
            @RequestBody QuickFactRequest request) {
        ContentResponse<QuickFact> response = quickFactService.createOrUpdateQuickFact(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @PutMapping("/facts/{key}")
    public ResponseEntity<ContentResponse<QuickFact>> updateQuickFactValue(
            @PathVariable String key, @RequestParam String value) {
        ContentResponse<QuickFact> response = quickFactService.updateQuickFactValue(key, value);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @GetMapping("/facts")
    public ResponseEntity<ContentResponse<List<QuickFact>>> getAllQuickFacts() {
        ContentResponse<List<QuickFact>> response = quickFactService.getAllQuickFacts();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/facts/{key}")
    public ResponseEntity<ContentResponse<QuickFact>> getQuickFact(@PathVariable String key) {
        ContentResponse<QuickFact> response = quickFactService.getQuickFact(key);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/facts/category/{category}")
    public ResponseEntity<ContentResponse<List<QuickFact>>> getQuickFactsByCategory(@PathVariable String category) {
        ContentResponse<List<QuickFact>> response = quickFactService.getQuickFactsByCategory(category);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/facts/search")
    public ResponseEntity<ContentResponse<List<QuickFact>>> searchQuickFacts(@RequestParam String q) {
        ContentResponse<List<QuickFact>> response = quickFactService.searchQuickFacts(q);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/facts/recent")
    public ResponseEntity<ContentResponse<List<QuickFact>>> getRecentQuickFacts(
            @RequestParam(defaultValue = "24") int hours) {
        ContentResponse<List<QuickFact>> response = quickFactService.getRecentQuickFacts(hours);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/facts/categories")
    public ResponseEntity<ContentResponse<List<String>>> getAllCategories() {
        ContentResponse<List<String>> response = quickFactService.getAllCategories();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/facts/stats")
    public ResponseEntity<ContentResponse<Map<String, Object>>> getQuickFactStats() {
        ContentResponse<Map<String, Object>> response = quickFactService.getQuickFactStats();
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/facts/{key}")
    public ResponseEntity<ContentResponse<Void>> deleteQuickFact(@PathVariable String key) {
        ContentResponse<Void> response = quickFactService.deleteQuickFact(key);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @PostMapping("/facts/bulk")
    public ResponseEntity<ContentResponse<List<QuickFact>>> bulkUpdateQuickFacts(
            @RequestBody Map<String, String> keyValuePairs) {
        ContentResponse<List<QuickFact>> response = quickFactService.bulkUpdateQuickFacts(keyValuePairs);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    // Media Activity endpoints
    @PostMapping("/activities")
    public ResponseEntity<ContentResponse<MediaActivityResponse>> createMediaActivity(
            @RequestBody MediaActivityRequest request) {
        ContentResponse<MediaActivityResponse> response = mediaActivityService.createMediaActivity(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @GetMapping("/activities")
    public ResponseEntity<ContentResponse<List<MediaActivityResponse>>> getAllMediaActivities() {
        ContentResponse<List<MediaActivityResponse>> response = mediaActivityService.getAllMediaActivities();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/activities/paginated")
    public ResponseEntity<ContentResponse<Page<MediaActivityResponse>>> getActivitiesPaginated(Pageable pageable) {
        ContentResponse<Page<MediaActivityResponse>> response = mediaActivityService.getActivitiesPaginated(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/activities/{id}")
    public ResponseEntity<ContentResponse<MediaActivityResponse>> getMediaActivity(@PathVariable Long id) {
        ContentResponse<MediaActivityResponse> response = mediaActivityService.getMediaActivity(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/activities/{id}")
    public ResponseEntity<ContentResponse<MediaActivityResponse>> updateMediaActivity(
            @PathVariable Long id, @RequestBody MediaActivityRequest request) {
        ContentResponse<MediaActivityResponse> response = mediaActivityService.updateMediaActivity(id, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @PutMapping("/activities/{id}/status")
    public ResponseEntity<ContentResponse<MediaActivityResponse>> updateActivityStatus(
            @PathVariable Long id, @RequestParam ActivityStatus status) {
        ContentResponse<MediaActivityResponse> response = mediaActivityService.updateActivityStatus(id, status);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @DeleteMapping("/activities/{id}")
    public ResponseEntity<ContentResponse<Void>> deleteMediaActivity(@PathVariable Long id) {
        ContentResponse<Void> response = mediaActivityService.deleteMediaActivity(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/activities/type/{mediaType}")
    public ResponseEntity<ContentResponse<List<MediaActivityResponse>>> getActivitiesByType(
            @PathVariable MediaType mediaType) {
        ContentResponse<List<MediaActivityResponse>> response = mediaActivityService.getActivitiesByType(mediaType);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/activities/status/{status}")
    public ResponseEntity<ContentResponse<List<MediaActivityResponse>>> getActivitiesByStatus(
            @PathVariable ActivityStatus status) {
        ContentResponse<List<MediaActivityResponse>> response = mediaActivityService.getActivitiesByStatus(status);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/activities/current")
    public ResponseEntity<ContentResponse<List<MediaActivityResponse>>> getCurrentlyEngagedActivities() {
        ContentResponse<List<MediaActivityResponse>> response = mediaActivityService.getCurrentlyEngagedActivities();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/activities/search")
    public ResponseEntity<ContentResponse<List<MediaActivityResponse>>> searchActivities(@RequestParam String q) {
        ContentResponse<List<MediaActivityResponse>> response = mediaActivityService.searchActivities(q);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/activities/recent")
    public ResponseEntity<ContentResponse<List<MediaActivityResponse>>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        ContentResponse<List<MediaActivityResponse>> response = mediaActivityService.getRecentActivities(limit);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/activities/stats")
    public ResponseEntity<ContentResponse<Map<String, Object>>> getActivityStatistics() {
        ContentResponse<Map<String, Object>> response = mediaActivityService.getActivityStatistics();
        return ResponseEntity.ok(response);
    }
    
    // Trip endpoints
    @PostMapping("/trips")
    public ResponseEntity<ContentResponse<TripResponse>> createTrip(
            @RequestBody TripRequest request) {
        ContentResponse<TripResponse> response = tripService.createTrip(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @GetMapping("/trips")
    public ResponseEntity<ContentResponse<List<TripResponse>>> getAllTrips() {
        ContentResponse<List<TripResponse>> response = tripService.getAllTrips();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/paginated")
    public ResponseEntity<ContentResponse<Page<TripResponse>>> getTripsPaginated(Pageable pageable) {
        ContentResponse<Page<TripResponse>> response = tripService.getTripsPaginated(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/{id}")
    public ResponseEntity<ContentResponse<TripResponse>> getTrip(@PathVariable Long id) {
        ContentResponse<TripResponse> response = tripService.getTrip(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/trips/{id}")
    public ResponseEntity<ContentResponse<TripResponse>> updateTrip(
            @PathVariable Long id, @RequestBody TripRequest request) {
        ContentResponse<TripResponse> response = tripService.updateTrip(id, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @PutMapping("/trips/{id}/status")
    public ResponseEntity<ContentResponse<TripResponse>> updateTripStatus(
            @PathVariable Long id, @RequestParam TripStatus status) {
        ContentResponse<TripResponse> response = tripService.updateTripStatus(id, status);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @DeleteMapping("/trips/{id}")
    public ResponseEntity<ContentResponse<Void>> deleteTrip(@PathVariable Long id) {
        ContentResponse<Void> response = tripService.deleteTrip(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/trips/status/{status}")
    public ResponseEntity<ContentResponse<List<TripResponse>>> getTripsByStatus(
            @PathVariable TripStatus status) {
        ContentResponse<List<TripResponse>> response = tripService.getTripsByStatus(status);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/type/{tripType}")
    public ResponseEntity<ContentResponse<List<TripResponse>>> getTripsByType(
            @PathVariable TripType tripType) {
        ContentResponse<List<TripResponse>> response = tripService.getTripsByType(tripType);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/upcoming")
    public ResponseEntity<ContentResponse<List<TripResponse>>> getUpcomingTrips() {
        ContentResponse<List<TripResponse>> response = tripService.getUpcomingTrips();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/current")
    public ResponseEntity<ContentResponse<List<TripResponse>>> getCurrentTrips() {
        ContentResponse<List<TripResponse>> response = tripService.getCurrentTrips();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/past")
    public ResponseEntity<ContentResponse<List<TripResponse>>> getPastTrips() {
        ContentResponse<List<TripResponse>> response = tripService.getPastTrips();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/search")
    public ResponseEntity<ContentResponse<List<TripResponse>>> searchTrips(@RequestParam String q) {
        ContentResponse<List<TripResponse>> response = tripService.searchTrips(q);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/date-range")
    public ResponseEntity<ContentResponse<List<TripResponse>>> getTripsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ContentResponse<List<TripResponse>> response = tripService.getTripsByDateRange(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/conflicts")
    public ResponseEntity<ContentResponse<List<TripResponse>>> checkDateConflicts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long excludeId) {
        ContentResponse<List<TripResponse>> response = tripService.checkDateConflicts(startDate, endDate, excludeId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/recent")
    public ResponseEntity<ContentResponse<List<TripResponse>>> getRecentTrips(
            @RequestParam(defaultValue = "10") int limit) {
        ContentResponse<List<TripResponse>> response = tripService.getRecentTrips(limit);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/trips/stats")
    public ResponseEntity<ContentResponse<Map<String, Object>>> getTripStatistics() {
        ContentResponse<Map<String, Object>> response = tripService.getTripStatistics();
        return ResponseEntity.ok(response);
    }
}