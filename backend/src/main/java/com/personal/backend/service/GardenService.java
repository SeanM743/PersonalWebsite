package com.personal.backend.service;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.GardenNoteRequest;
import com.personal.backend.dto.GardenNoteResponse;
import com.personal.backend.dto.LifeLogEntryResponse;
import com.personal.backend.dto.ValidationResult;
import com.personal.backend.model.GardenNote;
import com.personal.backend.model.GrowthStage;
import com.personal.backend.model.LifeLogEntry;
import com.personal.backend.repository.GardenRepository;
import com.personal.backend.repository.LifeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GardenService {
    
    private final GardenRepository gardenRepository;
    private final LifeLogRepository lifeLogRepository;
    private final ContentValidationService validationService;
    
    @Transactional
    public ContentResponse<GardenNoteResponse> createNote(GardenNoteRequest request) {
        log.debug("Creating garden note: {} - {}", request.getGrowthStage(), request.getTitle());
        
        // Validate the request
        ValidationResult validation = validateGardenNote(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        try {
            // Check for potential duplicates
            List<GardenNote> duplicates = gardenRepository.findPotentialDuplicates(request.getTitle(), -1L);
            if (!duplicates.isEmpty()) {
                log.warn("Potential duplicate found for note: {}", request.getTitle());
                // Continue anyway but log the warning
            }
            
            // Create the note
            GardenNote note = GardenNote.builder()
                    .title(validationService.sanitizeInput(request.getTitle()))
                    .content(validationService.sanitizeInput(request.getContent()))
                    .growthStage(request.getGrowthStage())
                    .linkedEntries(new HashSet<>())
                    .build();
            
            // Link to LifeLog entries if specified
            if (request.getLinkedEntryIds() != null && !request.getLinkedEntryIds().isEmpty()) {
                Set<LifeLogEntry> linkedEntries = new HashSet<>();
                for (Long entryId : request.getLinkedEntryIds()) {
                    Optional<LifeLogEntry> entry = lifeLogRepository.findById(entryId);
                    if (entry.isPresent()) {
                        linkedEntries.add(entry.get());
                    } else {
                        log.warn("LifeLog entry not found for linking: {}", entryId);
                    }
                }
                note.setLinkedEntries(linkedEntries);
            }
            
            GardenNote savedNote = gardenRepository.save(note);
            
            log.info("Created garden note with ID: {} - {} ({})", 
                    savedNote.getId(), savedNote.getTitle(), savedNote.getGrowthStage());
            
            return ContentResponse.success(
                    mapToResponse(savedNote),
                    "Garden note created successfully"
            );
            
        } catch (Exception e) {
            log.error("Error creating garden note", e);
            return ContentResponse.error("Failed to create garden note: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<GardenNoteResponse> updateNote(Long id, GardenNoteRequest request) {
        log.debug("Updating garden note with ID: {}", id);
        
        // Validate the request
        ValidationResult validation = validateGardenNote(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        Optional<GardenNote> existingNote = gardenRepository.findById(id);
        if (existingNote.isEmpty()) {
            return ContentResponse.error("Garden note not found with ID: " + id);
        }
        
        try {
            GardenNote note = existingNote.get();
            
            // Update basic fields
            note.setTitle(validationService.sanitizeInput(request.getTitle()));
            note.setContent(validationService.sanitizeInput(request.getContent()));
            note.setGrowthStage(request.getGrowthStage());
            
            // Update linked entries
            Set<LifeLogEntry> linkedEntries = new HashSet<>();
            if (request.getLinkedEntryIds() != null) {
                for (Long entryId : request.getLinkedEntryIds()) {
                    Optional<LifeLogEntry> entry = lifeLogRepository.findById(entryId);
                    if (entry.isPresent()) {
                        linkedEntries.add(entry.get());
                    } else {
                        log.warn("LifeLog entry not found for linking: {}", entryId);
                    }
                }
            }
            note.setLinkedEntries(linkedEntries);
            
            GardenNote updatedNote = gardenRepository.save(note);
            
            log.info("Updated garden note with ID: {} - {}", updatedNote.getId(), updatedNote.getTitle());
            
            return ContentResponse.success(
                    mapToResponse(updatedNote),
                    "Garden note updated successfully"
            );
            
        } catch (Exception e) {
            log.error("Error updating garden note", e);
            return ContentResponse.error("Failed to update garden note: " + e.getMessage());
        }
    }
    
    public ContentResponse<GardenNoteResponse> getNoteById(Long id) {
        Optional<GardenNote> note = gardenRepository.findById(id);
        
        if (note.isEmpty()) {
            return ContentResponse.error("Garden note not found with ID: " + id);
        }
        
        return ContentResponse.success(mapToResponse(note.get()));
    }
    
    public ContentResponse<List<GardenNoteResponse>> getAllNotes(GrowthStage stage) {
        try {
            List<GardenNote> notes;
            
            if (stage != null) {
                notes = gardenRepository.findByGrowthStageOrderByUpdatedAtDesc(stage);
            } else {
                notes = gardenRepository.findAll();
            }
            
            List<GardenNoteResponse> responses = notes.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving garden notes", e);
            return ContentResponse.error("Failed to retrieve garden notes: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<GardenNoteResponse>> searchNotes(String searchTerm, GrowthStage stage) {
        try {
            List<GardenNote> notes;
            
            if (stage != null) {
                notes = gardenRepository.findByGrowthStageAndSearchTerm(stage, searchTerm);
            } else {
                notes = gardenRepository.searchNotes(searchTerm);
            }
            
            List<GardenNoteResponse> responses = notes.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error searching garden notes", e);
            return ContentResponse.error("Failed to search garden notes: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<GardenNoteResponse>> getNotesLinkedToEntry(Long lifeLogId) {
        try {
            List<GardenNote> notes = gardenRepository.findNotesLinkedToEntry(lifeLogId);
            List<GardenNoteResponse> responses = notes.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving notes linked to entry: {}", lifeLogId, e);
            return ContentResponse.error("Failed to retrieve linked notes: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<GardenNoteResponse> linkToLifeLog(Long noteId, Long lifeLogId) {
        log.debug("Linking garden note {} to LifeLog entry {}", noteId, lifeLogId);
        
        Optional<GardenNote> noteOpt = gardenRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return ContentResponse.error("Garden note not found with ID: " + noteId);
        }
        
        Optional<LifeLogEntry> entryOpt = lifeLogRepository.findById(lifeLogId);
        if (entryOpt.isEmpty()) {
            return ContentResponse.error("LifeLog entry not found with ID: " + lifeLogId);
        }
        
        try {
            GardenNote note = noteOpt.get();
            LifeLogEntry entry = entryOpt.get();
            
            // Add the link
            note.getLinkedEntries().add(entry);
            GardenNote updatedNote = gardenRepository.save(note);
            
            log.info("Linked garden note {} to LifeLog entry {}", noteId, lifeLogId);
            
            return ContentResponse.success(
                    mapToResponse(updatedNote),
                    "Note linked successfully"
            );
            
        } catch (Exception e) {
            log.error("Error linking note to entry", e);
            return ContentResponse.error("Failed to link note: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<GardenNoteResponse> unlinkFromLifeLog(Long noteId, Long lifeLogId) {
        log.debug("Unlinking garden note {} from LifeLog entry {}", noteId, lifeLogId);
        
        Optional<GardenNote> noteOpt = gardenRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return ContentResponse.error("Garden note not found with ID: " + noteId);
        }
        
        Optional<LifeLogEntry> entryOpt = lifeLogRepository.findById(lifeLogId);
        if (entryOpt.isEmpty()) {
            return ContentResponse.error("LifeLog entry not found with ID: " + lifeLogId);
        }
        
        try {
            GardenNote note = noteOpt.get();
            LifeLogEntry entry = entryOpt.get();
            
            // Remove the link
            note.getLinkedEntries().remove(entry);
            GardenNote updatedNote = gardenRepository.save(note);
            
            log.info("Unlinked garden note {} from LifeLog entry {}", noteId, lifeLogId);
            
            return ContentResponse.success(
                    mapToResponse(updatedNote),
                    "Note unlinked successfully"
            );
            
        } catch (Exception e) {
            log.error("Error unlinking note from entry", e);
            return ContentResponse.error("Failed to unlink note: " + e.getMessage());
        }
    }
    
    public ContentResponse<Page<GardenNoteResponse>> getNotesPaginated(Pageable pageable, GrowthStage stage) {
        try {
            Page<GardenNote> notes;
            
            if (stage != null) {
                notes = gardenRepository.findByGrowthStageOrderByUpdatedAtDesc(stage, pageable);
            } else {
                notes = gardenRepository.findAll(pageable);
            }
            
            Page<GardenNoteResponse> responses = notes.map(this::mapToResponse);
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving paginated notes", e);
            return ContentResponse.error("Failed to retrieve notes: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<GardenNoteResponse>> getRecentNotes(int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<GardenNote> notes = gardenRepository.findRecentNotes(pageable);
            List<GardenNoteResponse> responses = notes.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving recent notes", e);
            return ContentResponse.error("Failed to retrieve recent notes: " + e.getMessage());
        }
    }
    
    public ContentResponse<Map<String, Object>> getNoteStatistics() {
        try {
            long totalNotes = gardenRepository.count();
            
            List<Object[]> stageStats = gardenRepository.getNoteCountByGrowthStage();
            Map<String, Long> stageCountMap = stageStats.stream()
                    .collect(Collectors.toMap(
                            row -> ((GrowthStage) row[0]).name(),
                            row -> (Long) row[1]
                    ));
            
            long notesWithLinks = gardenRepository.countNotesWithLinks();
            long notesWithoutLinks = gardenRepository.countNotesWithoutLinks();
            
            Map<String, Object> stats = Map.of(
                    "totalNotes", totalNotes,
                    "byGrowthStage", stageCountMap,
                    "notesWithLinks", notesWithLinks,
                    "notesWithoutLinks", notesWithoutLinks
            );
            
            return ContentResponse.success(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving note statistics", e);
            return ContentResponse.error("Failed to retrieve statistics: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<Void> deleteNote(Long id) {
        Optional<GardenNote> note = gardenRepository.findById(id);
        
        if (note.isEmpty()) {
            return ContentResponse.error("Garden note not found with ID: " + id);
        }
        
        try {
            gardenRepository.deleteById(id);
            
            log.info("Deleted garden note with ID: {} - {}", id, note.get().getTitle());
            return ContentResponse.success(null, "Garden note deleted successfully");
            
        } catch (Exception e) {
            log.error("Error deleting garden note", e);
            return ContentResponse.error("Failed to delete garden note: " + e.getMessage());
        }
    }
    
    private ValidationResult validateGardenNote(GardenNoteRequest request) {
        ValidationResult result = new ValidationResult();
        
        // Basic validation
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            result.addError("title", "Title is required");
        }
        
        if (request.getGrowthStage() == null) {
            result.addError("growthStage", "Growth stage is required");
        }
        
        // Content length validation
        if (request.getContent() != null && request.getContent().length() > 10000) {
            result.addError("content", "Content cannot exceed 10,000 characters");
        }
        
        // Title length validation
        if (request.getTitle() != null && request.getTitle().length() > 200) {
            result.addError("title", "Title cannot exceed 200 characters");
        }
        
        return result;
    }
    
    private GardenNoteResponse mapToResponse(GardenNote note) {
        List<LifeLogEntryResponse> linkedEntryResponses = note.getLinkedEntries().stream()
                .map(this::mapLifeLogEntryToResponse)
                .collect(Collectors.toList());
        
        return GardenNoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .growthStage(note.getGrowthStage())
                .linkedEntries(linkedEntryResponses)
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
    
    private LifeLogEntryResponse mapLifeLogEntryToResponse(LifeLogEntry entry) {
        return LifeLogEntryResponse.builder()
                .id(entry.getId())
                .title(entry.getTitle())
                .type(entry.getType())
                .startDate(entry.getStartDate())
                .endDate(entry.getEndDate())
                .status(entry.getStatus())
                .rating(entry.getRating())
                .keyTakeaway(entry.getKeyTakeaway())
                .intensity(entry.getIntensity())
                .imageUrl(entry.getImageUrl())
                .externalId(entry.getExternalId())
                .metadata(entry.getMetadata())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}