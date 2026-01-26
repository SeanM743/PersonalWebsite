package com.personal.backend.controller;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.GardenNoteRequest;
import com.personal.backend.dto.GardenNoteResponse;
import com.personal.backend.model.GrowthStage;
import com.personal.backend.service.GardenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/garden")
@RequiredArgsConstructor
public class GardenController {
    
    private final GardenService gardenService;
    
    // Basic CRUD operations
    @PostMapping
    public ResponseEntity<ContentResponse<GardenNoteResponse>> createNote(
            @Valid @RequestBody GardenNoteRequest request) {
        ContentResponse<GardenNoteResponse> response = gardenService.createNote(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ContentResponse<GardenNoteResponse>> getNote(@PathVariable Long id) {
        ContentResponse<GardenNoteResponse> response = gardenService.getNoteById(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ContentResponse<GardenNoteResponse>> updateNote(
            @PathVariable Long id, @Valid @RequestBody GardenNoteRequest request) {
        ContentResponse<GardenNoteResponse> response = gardenService.updateNote(id, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ContentResponse<Void>> deleteNote(@PathVariable Long id) {
        ContentResponse<Void> response = gardenService.deleteNote(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    // List and filtering operations
    @GetMapping
    public ResponseEntity<ContentResponse<List<GardenNoteResponse>>> getAllNotes(
            @RequestParam(required = false) GrowthStage stage) {
        ContentResponse<List<GardenNoteResponse>> response = gardenService.getAllNotes(stage);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/paginated")
    public ResponseEntity<ContentResponse<Page<GardenNoteResponse>>> getNotesPaginated(
            Pageable pageable,
            @RequestParam(required = false) GrowthStage stage) {
        ContentResponse<Page<GardenNoteResponse>> response = gardenService.getNotesPaginated(pageable, stage);
        return ResponseEntity.ok(response);
    }
    
    // Filter by growth stage
    @GetMapping("/stage/{stage}")
    public ResponseEntity<ContentResponse<List<GardenNoteResponse>>> getNotesByStage(
            @PathVariable GrowthStage stage) {
        ContentResponse<List<GardenNoteResponse>> response = gardenService.getAllNotes(stage);
        return ResponseEntity.ok(response);
    }
    
    // Search functionality
    @GetMapping("/search")
    public ResponseEntity<ContentResponse<List<GardenNoteResponse>>> searchNotes(
            @RequestParam String q,
            @RequestParam(required = false) GrowthStage stage) {
        ContentResponse<List<GardenNoteResponse>> response = gardenService.searchNotes(q, stage);
        return ResponseEntity.ok(response);
    }
    
    // Recent notes
    @GetMapping("/recent")
    public ResponseEntity<ContentResponse<List<GardenNoteResponse>>> getRecentNotes(
            @RequestParam(defaultValue = "10") int limit) {
        ContentResponse<List<GardenNoteResponse>> response = gardenService.getRecentNotes(limit);
        return ResponseEntity.ok(response);
    }
    
    // Statistics
    @GetMapping("/stats")
    public ResponseEntity<ContentResponse<Map<String, Object>>> getNoteStatistics() {
        ContentResponse<Map<String, Object>> response = gardenService.getNoteStatistics();
        return ResponseEntity.ok(response);
    }
    
    // Linking operations
    @PostMapping("/{id}/link/{lifelogId}")
    public ResponseEntity<ContentResponse<GardenNoteResponse>> linkToLifeLog(
            @PathVariable Long id, @PathVariable Long lifelogId) {
        ContentResponse<GardenNoteResponse> response = gardenService.linkToLifeLog(id, lifelogId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @DeleteMapping("/{id}/link/{lifelogId}")
    public ResponseEntity<ContentResponse<GardenNoteResponse>> unlinkFromLifeLog(
            @PathVariable Long id, @PathVariable Long lifelogId) {
        ContentResponse<GardenNoteResponse> response = gardenService.unlinkFromLifeLog(id, lifelogId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @GetMapping("/linked-to/{lifelogId}")
    public ResponseEntity<ContentResponse<List<GardenNoteResponse>>> getNotesLinkedToEntry(
            @PathVariable Long lifelogId) {
        ContentResponse<List<GardenNoteResponse>> response = gardenService.getNotesLinkedToEntry(lifelogId);
        return ResponseEntity.ok(response);
    }
    
    // Growth stage specific endpoints
    @GetMapping("/seedlings")
    public ResponseEntity<ContentResponse<List<GardenNoteResponse>>> getSeedlings() {
        ContentResponse<List<GardenNoteResponse>> response = gardenService.getAllNotes(GrowthStage.SEEDLING);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/budding")
    public ResponseEntity<ContentResponse<List<GardenNoteResponse>>> getBuddingNotes() {
        ContentResponse<List<GardenNoteResponse>> response = gardenService.getAllNotes(GrowthStage.BUDDING);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/evergreen")
    public ResponseEntity<ContentResponse<List<GardenNoteResponse>>> getEvergreenNotes() {
        ContentResponse<List<GardenNoteResponse>> response = gardenService.getAllNotes(GrowthStage.EVERGREEN);
        return ResponseEntity.ok(response);
    }
}