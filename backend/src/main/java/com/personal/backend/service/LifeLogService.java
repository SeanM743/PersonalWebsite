package com.personal.backend.service;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.LifeLogEntryRequest;
import com.personal.backend.dto.LifeLogEntryResponse;
import com.personal.backend.dto.ValidationResult;
import com.personal.backend.model.EntryStatus;
import com.personal.backend.model.LifeLogEntry;
import com.personal.backend.model.LifeLogType;
import com.personal.backend.repository.LifeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LifeLogService {
    
    private final LifeLogRepository lifeLogRepository;
    private final ContentValidationService validationService;
    private final MetadataFetcherService metadataFetcherService;
    
    @Transactional
    public ContentResponse<LifeLogEntryResponse> createEntry(LifeLogEntryRequest request) {
        log.debug("Creating life log entry: {} - {}", request.getType(), request.getTitle());
        
        // Validate the request
        ValidationResult validation = validateLifeLogEntry(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        try {
            LifeLogEntry entry = LifeLogEntry.builder()
                    .title(validationService.sanitizeInput(request.getTitle()))
                    .type(request.getType())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .status(request.getStatus() != null ? request.getStatus() : EntryStatus.PLANNED)
                    .rating(request.getRating())
                    .keyTakeaway(validationService.sanitizeInput(request.getKeyTakeaway()))
                    .intensity(request.getIntensity())
                    .imageUrl(request.getImageUrl())
                    .externalId(request.getExternalId())
                    .metadata(validationService.sanitizeInput(request.getMetadata()))
                    .build();
            
            LifeLogEntry savedEntry = lifeLogRepository.save(entry);
            
            log.info("Created life log entry with ID: {} - {} ({})", 
                    savedEntry.getId(), savedEntry.getTitle(), savedEntry.getType());
            
            return ContentResponse.success(
                    mapToResponse(savedEntry),
                    "Life log entry created successfully"
            );
            
        } catch (Exception e) {
            log.error("Error creating life log entry", e);
            return ContentResponse.error("Failed to create life log entry: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<LifeLogEntryResponse> updateEntry(Long id, LifeLogEntryRequest request) {
        log.debug("Updating life log entry with ID: {}", id);
        
        // Validate the request
        ValidationResult validation = validateLifeLogEntry(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        Optional<LifeLogEntry> existingEntry = lifeLogRepository.findById(id);
        if (existingEntry.isEmpty()) {
            return ContentResponse.error("Life log entry not found with ID: " + id);
        }
        
        try {
            LifeLogEntry entry = existingEntry.get();
            
            // Update fields
            entry.setTitle(validationService.sanitizeInput(request.getTitle()));
            entry.setType(request.getType());
            entry.setStartDate(request.getStartDate());
            entry.setEndDate(request.getEndDate());
            entry.setStatus(request.getStatus());
            entry.setRating(request.getRating());
            entry.setKeyTakeaway(validationService.sanitizeInput(request.getKeyTakeaway()));
            entry.setIntensity(request.getIntensity());
            entry.setImageUrl(request.getImageUrl());
            entry.setExternalId(request.getExternalId());
            entry.setMetadata(validationService.sanitizeInput(request.getMetadata()));
            
            LifeLogEntry updatedEntry = lifeLogRepository.save(entry);
            
            log.info("Updated life log entry with ID: {} - {}", updatedEntry.getId(), updatedEntry.getTitle());
            
            return ContentResponse.success(
                    mapToResponse(updatedEntry),
                    "Life log entry updated successfully"
            );
            
        } catch (Exception e) {
            log.error("Error updating life log entry", e);
            return ContentResponse.error("Failed to update life log entry: " + e.getMessage());
        }
    }
    
    public ContentResponse<LifeLogEntryResponse> getEntryById(Long id) {
        Optional<LifeLogEntry> entry = lifeLogRepository.findById(id);
        
        if (entry.isEmpty()) {
            return ContentResponse.error("Life log entry not found with ID: " + id);
        }
        
        return ContentResponse.success(mapToResponse(entry.get()));
    }
    
    public ContentResponse<List<LifeLogEntryResponse>> getAllEntries(LifeLogType type, EntryStatus status) {
        try {
            List<LifeLogEntry> entries;
            
            if (type != null && status != null) {
                entries = lifeLogRepository.findByTypeAndStatusOrderByUpdatedAtDesc(type, status);
            } else if (type != null) {
                entries = lifeLogRepository.findByTypeOrderByUpdatedAtDesc(type);
            } else if (status != null) {
                entries = lifeLogRepository.findByStatusOrderByUpdatedAtDesc(status);
            } else {
                entries = lifeLogRepository.findAll();
            }
            
            List<LifeLogEntryResponse> responses = entries.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving life log entries", e);
            return ContentResponse.error("Failed to retrieve life log entries: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<LifeLogEntryResponse>> getEntriesInDateRange(LocalDate start, LocalDate end) {
        try {
            List<LifeLogEntry> entries = lifeLogRepository.findEntriesInDateRange(start, end);
            List<LifeLogEntryResponse> responses = entries.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving entries in date range", e);
            return ContentResponse.error("Failed to retrieve entries: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<LifeLogEntryResponse>> getEntriesByTypes(List<LifeLogType> types, LocalDate start, LocalDate end) {
        try {
            List<LifeLogEntry> entries;
            
            if (start != null && end != null) {
                entries = lifeLogRepository.findByTypesInDateRange(types, start, end);
            } else {
                entries = lifeLogRepository.findByTypesInOrderByStartDateAsc(types);
            }
            
            List<LifeLogEntryResponse> responses = entries.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving entries by types", e);
            return ContentResponse.error("Failed to retrieve entries: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<LifeLogEntryResponse>> getActiveEntries() {
        try {
            List<LifeLogEntry> entries = lifeLogRepository.findActiveEntries();
            List<LifeLogEntryResponse> responses = entries.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving active entries", e);
            return ContentResponse.error("Failed to retrieve active entries: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<LifeLogEntryResponse>> searchEntries(String searchTerm) {
        try {
            List<LifeLogEntry> entries = lifeLogRepository.searchEntries(searchTerm);
            List<LifeLogEntryResponse> responses = entries.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error searching entries", e);
            return ContentResponse.error("Failed to search entries: " + e.getMessage());
        }
    }
    
    public ContentResponse<Page<LifeLogEntryResponse>> getEntriesPaginated(Pageable pageable, LifeLogType type, EntryStatus status) {
        try {
            Page<LifeLogEntry> entries;
            
            if (type != null && status != null) {
                entries = lifeLogRepository.findByTypeAndStatusOrderByUpdatedAtDesc(type, status, pageable);
            } else if (type != null) {
                entries = lifeLogRepository.findByTypeOrderByUpdatedAtDesc(type, pageable);
            } else if (status != null) {
                entries = lifeLogRepository.findByStatusOrderByUpdatedAtDesc(status, pageable);
            } else {
                entries = lifeLogRepository.findAll(pageable);
            }
            
            Page<LifeLogEntryResponse> responses = entries.map(this::mapToResponse);
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving paginated entries", e);
            return ContentResponse.error("Failed to retrieve entries: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<LifeLogEntryResponse>> getRecentEntries(int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<LifeLogEntry> entries = lifeLogRepository.findRecentEntries(pageable);
            List<LifeLogEntryResponse> responses = entries.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving recent entries", e);
            return ContentResponse.error("Failed to retrieve recent entries: " + e.getMessage());
        }
    }

    public ContentResponse<Object> searchMetadata(String query, LifeLogType type) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ContentResponse.error("Search query cannot be empty");
            }
            
            java.util.Optional<Object> result = java.util.Optional.empty();
            
            // Map LifeLogType to MediaType
            com.personal.backend.model.MediaType mediaType = null;
            try {
                if (type == LifeLogType.SHOW) {
                    mediaType = com.personal.backend.model.MediaType.TV_SHOW;
                } else {
                    mediaType = com.personal.backend.model.MediaType.valueOf(type.name());
                }
            } catch (IllegalArgumentException e) {
                // Type might not exist in MediaType enum or mapping is different
                log.warn("LifeLogType {} not directly mappable to MediaType", type);
            }
            
            if (mediaType != null) {
                // Using the generic fetch method
                result = metadataFetcherService.fetchMetadataByType(query, mediaType, null, null);
            }
            
            if (result.isPresent()) {
                return ContentResponse.success(result.get());
            } else {
                return ContentResponse.success(null, "No metadata found");
            }
            
        } catch (Exception e) {
            log.error("Error searching metadata for query: " + query, e);
            return ContentResponse.error("Failed to search metadata: " + e.getMessage());
        }
    }
    
    public ContentResponse<Map<String, Object>> getEntryStatistics() {
        try {
            long totalEntries = lifeLogRepository.count();
            
            List<Object[]> typeStats = lifeLogRepository.getEntryCountByType();
            Map<String, Long> typeCountMap = typeStats.stream()
                    .collect(Collectors.toMap(
                            row -> ((LifeLogType) row[0]).name(),
                            row -> (Long) row[1]
                    ));
            
            List<Object[]> statusStats = lifeLogRepository.getEntryCountByStatus();
            Map<String, Long> statusCountMap = statusStats.stream()
                    .collect(Collectors.toMap(
                            row -> ((EntryStatus) row[0]).name(),
                            row -> (Long) row[1]
                    ));
            
            Double averageHobbyIntensity = lifeLogRepository.getAverageHobbyIntensity();
            
            Map<String, Object> stats = Map.of(
                    "totalEntries", totalEntries,
                    "byType", typeCountMap,
                    "byStatus", statusCountMap,
                    "activeEntries", lifeLogRepository.findActiveEntries().size(),
                    "averageHobbyIntensity", averageHobbyIntensity != null ? averageHobbyIntensity : 0.0
            );
            
            return ContentResponse.success(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving entry statistics", e);
            return ContentResponse.error("Failed to retrieve statistics: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<Void> deleteEntry(Long id) {
        Optional<LifeLogEntry> entry = lifeLogRepository.findById(id);
        
        if (entry.isEmpty()) {
            return ContentResponse.error("Life log entry not found with ID: " + id);
        }
        
        try {
            lifeLogRepository.deleteById(id);
            
            log.info("Deleted life log entry with ID: {} - {}", id, entry.get().getTitle());
            return ContentResponse.success(null, "Life log entry deleted successfully");
            
        } catch (Exception e) {
            log.error("Error deleting life log entry", e);
            return ContentResponse.error("Failed to delete life log entry: " + e.getMessage());
        }
    }
    
    private ValidationResult validateLifeLogEntry(LifeLogEntryRequest request) {
        ValidationResult result = new ValidationResult();
        
        // Basic validation
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            result.addError("title", "Title is required");
        }
        
        if (request.getType() == null) {
            result.addError("type", "Type is required");
        }
        
        // HOBBY-specific validation
        if (request.getType() == LifeLogType.HOBBY) {
            if (request.getIntensity() == null) {
                result.addError("intensity", "Intensity is required for HOBBY entries");
            } else if (request.getIntensity() < 1 || request.getIntensity() > 5) {
                result.addError("intensity", "Intensity must be between 1 and 5");
            }
        }
        
        // Rating validation
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            result.addError("rating", "Rating must be between 1 and 5");
        }
        
        // Date validation
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                result.addError("dates", "Start date cannot be after end date");
            }
        }
        
        // Key takeaway length validation
        if (request.getKeyTakeaway() != null && request.getKeyTakeaway().length() > 500) {
            result.addError("keyTakeaway", "Key takeaway cannot exceed 500 characters");
        }
        
        // Metadata length validation
        if (request.getMetadata() != null && request.getMetadata().length() > 10000) {
            result.addError("metadata", "Metadata cannot exceed 10000 characters");
        }
        
        return result;
    }
    
    private LifeLogEntryResponse mapToResponse(LifeLogEntry entry) {
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