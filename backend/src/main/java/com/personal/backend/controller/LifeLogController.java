package com.personal.backend.controller;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.LifeLogEntryRequest;
import com.personal.backend.dto.LifeLogEntryResponse;
import com.personal.backend.model.EntryStatus;
import com.personal.backend.model.LifeLogType;
import com.personal.backend.service.LifeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lifelog")
@RequiredArgsConstructor
public class LifeLogController {
    
    private final LifeLogService lifeLogService;
    
    // Basic CRUD operations
    @PostMapping
    public ResponseEntity<ContentResponse<LifeLogEntryResponse>> createEntry(
            @Valid @RequestBody LifeLogEntryRequest request) {
        ContentResponse<LifeLogEntryResponse> response = lifeLogService.createEntry(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ContentResponse<LifeLogEntryResponse>> getEntry(@PathVariable Long id) {
        ContentResponse<LifeLogEntryResponse> response = lifeLogService.getEntryById(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ContentResponse<LifeLogEntryResponse>> updateEntry(
            @PathVariable Long id, @Valid @RequestBody LifeLogEntryRequest request) {
        ContentResponse<LifeLogEntryResponse> response = lifeLogService.updateEntry(id, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ContentResponse<Void>> deleteEntry(@PathVariable Long id) {
        ContentResponse<Void> response = lifeLogService.deleteEntry(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    // List and filtering operations
    @GetMapping
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getAllEntries(
            @RequestParam(required = false) LifeLogType type,
            @RequestParam(required = false) EntryStatus status) {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(type, status);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/paginated")
    public ResponseEntity<ContentResponse<Page<LifeLogEntryResponse>>> getEntriesPaginated(
            Pageable pageable,
            @RequestParam(required = false) LifeLogType type,
            @RequestParam(required = false) EntryStatus status) {
        ContentResponse<Page<LifeLogEntryResponse>> response = lifeLogService.getEntriesPaginated(pageable, type, status);
        return ResponseEntity.ok(response);
    }
    
    // Timeline-specific endpoints
    @GetMapping("/timeline")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getTimelineEntries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<LifeLogType> types) {
        
        ContentResponse<List<LifeLogEntryResponse>> response;
        
        if (types != null && !types.isEmpty()) {
            response = lifeLogService.getEntriesByTypes(types, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            response = lifeLogService.getEntriesInDateRange(startDate, endDate);
        } else {
            // Default to current year if no dates specified
            LocalDate yearStart = LocalDate.now().withDayOfYear(1);
            LocalDate yearEnd = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
            response = lifeLogService.getEntriesInDateRange(yearStart, yearEnd);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/active")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getActiveEntries() {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getActiveEntries();
        return ResponseEntity.ok(response);
    }
    
    // Filter by type
    @GetMapping("/type/{type}")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getEntriesByType(
            @PathVariable LifeLogType type) {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(type, null);
        return ResponseEntity.ok(response);
    }
    
    // Filter by status
    @GetMapping("/status/{status}")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getEntriesByStatus(
            @PathVariable EntryStatus status) {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(null, status);
        return ResponseEntity.ok(response);
    }
    
    // Date range queries
    @GetMapping("/date-range")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getEntriesInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getEntriesInDateRange(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    // Search functionality
    @GetMapping("/search")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> searchEntries(
            @RequestParam String q) {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.searchEntries(q);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search-metadata")
    public ResponseEntity<ContentResponse<Object>> searchMetadata(
            @RequestParam String q,
            @RequestParam LifeLogType type) {
        ContentResponse<Object> response = lifeLogService.searchMetadata(q, type);
        return ResponseEntity.ok(response);
    }
    
    // Recent entries
    @GetMapping("/recent")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getRecentEntries(
            @RequestParam(defaultValue = "10") int limit) {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getRecentEntries(limit);
        return ResponseEntity.ok(response);
    }
    
    // Statistics
    @GetMapping("/stats")
    public ResponseEntity<ContentResponse<Map<String, Object>>> getEntryStatistics() {
        ContentResponse<Map<String, Object>> response = lifeLogService.getEntryStatistics();
        return ResponseEntity.ok(response);
    }
    
    // Type-specific endpoints
    @GetMapping("/hobbies")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getHobbies() {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(LifeLogType.HOBBY, null);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/books")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getBooks() {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(LifeLogType.BOOK, null);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/movies")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getMovies() {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(LifeLogType.MOVIE, null);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/shows")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getShows() {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(LifeLogType.SHOW, null);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/albums")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getAlbums() {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(LifeLogType.ALBUM, null);
        return ResponseEntity.ok(response);
    }
    
    // Status-specific endpoints
    @GetMapping("/in-progress")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getInProgressEntries() {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(null, EntryStatus.IN_PROGRESS);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/completed")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getCompletedEntries() {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(null, EntryStatus.COMPLETED);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/planned")
    public ResponseEntity<ContentResponse<List<LifeLogEntryResponse>>> getPlannedEntries() {
        ContentResponse<List<LifeLogEntryResponse>> response = lifeLogService.getAllEntries(null, EntryStatus.PLANNED);
        return ResponseEntity.ok(response);
    }
}