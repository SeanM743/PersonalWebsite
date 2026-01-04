package com.personal.backend.service;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.QuickFactRequest;
import com.personal.backend.dto.ValidationResult;
import com.personal.backend.model.QuickFact;
import com.personal.backend.repository.QuickFactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuickFactService {
    
    private final QuickFactRepository quickFactRepository;
    private final ContentValidationService validationService;
    
    @Transactional
    public ContentResponse<QuickFact> createQuickFact(QuickFact quickFact) {
        log.debug("Creating quick fact with key: {}", quickFact.getKey());
        
        try {
            // Validate the quick fact
            if (quickFact.getKey() == null || quickFact.getKey().trim().isEmpty()) {
                return ContentResponse.error("Quick fact key cannot be empty");
            }
            
            if (quickFact.getValue() == null || quickFact.getValue().trim().isEmpty()) {
                return ContentResponse.error("Quick fact value cannot be empty");
            }
            
            // Sanitize input
            quickFact.setKey(validationService.sanitizeInput(quickFact.getKey()).trim());
            quickFact.setValue(validationService.sanitizeInput(quickFact.getValue()));
            
            if (quickFact.getCategory() != null) {
                quickFact.setCategory(validationService.sanitizeInput(quickFact.getCategory()));
            } else {
                quickFact.setCategory("general");
            }
            
            if (quickFact.getDescription() != null) {
                quickFact.setDescription(validationService.sanitizeInput(quickFact.getDescription()));
            }
            
            QuickFact savedFact = quickFactRepository.save(quickFact);
            
            log.info("Successfully created quick fact with key: {}", quickFact.getKey());
            return ContentResponse.success(savedFact, "Quick fact created successfully");
            
        } catch (Exception e) {
            log.error("Error creating quick fact", e);
            return ContentResponse.error("Failed to create quick fact: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<QuickFact> createOrUpdateQuickFact(QuickFactRequest request) {
        log.debug("Creating or updating quick fact with key: {}", request.getKey());
        
        // Validate the request
        ValidationResult validation = validationService.validateQuickFact(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        try {
            // Sanitize input
            String sanitizedKey = validationService.sanitizeInput(request.getKey()).trim();
            String sanitizedValue = validationService.sanitizeInput(request.getValue());
            String sanitizedCategory = validationService.sanitizeInput(request.getCategory());
            String sanitizedDescription = validationService.sanitizeInput(request.getDescription());
            
            // Check if fact already exists
            Optional<QuickFact> existingFact = quickFactRepository.findById(sanitizedKey);
            
            QuickFact quickFact;
            String operation;
            
            if (existingFact.isPresent()) {
                // Update existing fact
                quickFact = existingFact.get();
                quickFact.setValue(sanitizedValue);
                quickFact.setCategory(sanitizedCategory);
                quickFact.setDescription(sanitizedDescription);
                operation = "updated";
            } else {
                // Create new fact
                quickFact = QuickFact.builder()
                        .key(sanitizedKey)
                        .value(sanitizedValue)
                        .category(sanitizedCategory)
                        .description(sanitizedDescription)
                        .build();
                operation = "created";
            }
            
            QuickFact savedFact = quickFactRepository.save(quickFact);
            
            log.info("Successfully {} quick fact with key: {}", operation, sanitizedKey);
            
            return ContentResponse.success(
                    savedFact,
                    String.format("Quick fact %s successfully", operation)
            );
            
        } catch (Exception e) {
            log.error("Error creating/updating quick fact", e);
            return ContentResponse.error("Failed to save quick fact: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<QuickFact> updateQuickFactValue(String key, String newValue) {
        log.debug("Updating quick fact value for key: {}", key);
        
        if (newValue == null || newValue.trim().isEmpty()) {
            return ContentResponse.error("Value cannot be empty");
        }
        
        Optional<QuickFact> existingFact = quickFactRepository.findById(key);
        if (existingFact.isEmpty()) {
            // Auto-create new fact if key doesn't exist
            QuickFactRequest request = QuickFactRequest.builder()
                    .key(key)
                    .value(newValue)
                    .build();
            return createOrUpdateQuickFact(request);
        }
        
        try {
            QuickFact quickFact = existingFact.get();
            quickFact.setValue(validationService.sanitizeInput(newValue));
            
            QuickFact savedFact = quickFactRepository.save(quickFact);
            
            log.info("Updated quick fact value for key: {}", key);
            return ContentResponse.success(savedFact, "Quick fact value updated successfully");
            
        } catch (Exception e) {
            log.error("Error updating quick fact value", e);
            return ContentResponse.error("Failed to update quick fact: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<QuickFact> updateQuickFact(String key, String newValue, String newCategory) {
        log.debug("Updating quick fact for key: {} with new category: {}", key, newCategory);
        
        if (key == null || key.trim().isEmpty()) {
            return ContentResponse.error("Key cannot be empty");
        }
        
        Optional<QuickFact> existingFact = quickFactRepository.findById(key);
        if (existingFact.isEmpty()) {
            // Auto-create new fact if key doesn't exist
            QuickFactRequest request = QuickFactRequest.builder()
                    .key(key)
                    .value(newValue != null ? newValue : "")
                    .category(newCategory != null ? newCategory : "general")
                    .build();
            return createOrUpdateQuickFact(request);
        }
        
        try {
            QuickFact quickFact = existingFact.get();
            
            if (newValue != null && !newValue.trim().isEmpty()) {
                quickFact.setValue(validationService.sanitizeInput(newValue));
            }
            
            if (newCategory != null && !newCategory.trim().isEmpty()) {
                quickFact.setCategory(validationService.sanitizeInput(newCategory));
            }
            
            QuickFact savedFact = quickFactRepository.save(quickFact);
            
            log.info("Updated quick fact for key: {}", key);
            return ContentResponse.success(savedFact, "Quick fact updated successfully");
            
        } catch (Exception e) {
            log.error("Error updating quick fact", e);
            return ContentResponse.error("Failed to update quick fact: " + e.getMessage());
        }
    }
    
    public ContentResponse<QuickFact> getQuickFact(String key) {
        Optional<QuickFact> quickFact = quickFactRepository.findById(key);
        
        if (quickFact.isEmpty()) {
            return ContentResponse.error("Quick fact not found with key: " + key);
        }
        
        return ContentResponse.success(quickFact.get());
    }
    
    public ContentResponse<List<QuickFact>> getAllQuickFacts() {
        try {
            List<QuickFact> facts = quickFactRepository.findAll();
            return ContentResponse.success(facts);
            
        } catch (Exception e) {
            log.error("Error retrieving all quick facts", e);
            return ContentResponse.error("Failed to retrieve quick facts: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<QuickFact>> getQuickFactsByCategory(String category) {
        try {
            List<QuickFact> facts = quickFactRepository.findByCategoryOrderByUpdatedAtDesc(category);
            return ContentResponse.success(facts);
            
        } catch (Exception e) {
            log.error("Error retrieving quick facts by category", e);
            return ContentResponse.error("Failed to retrieve quick facts: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<QuickFact>> getRecentQuickFacts(int hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<QuickFact> facts = quickFactRepository.findByUpdatedAtAfterOrderByUpdatedAtDesc(since);
            return ContentResponse.success(facts);
            
        } catch (Exception e) {
            log.error("Error retrieving recent quick facts", e);
            return ContentResponse.error("Failed to retrieve recent quick facts: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<QuickFact>> searchQuickFacts(String searchTerm) {
        try {
            // Search in both keys and values
            List<QuickFact> keyMatches = quickFactRepository.findByKeyContainingIgnoreCase(searchTerm);
            List<QuickFact> valueMatches = quickFactRepository.findByValueContainingIgnoreCase(searchTerm);
            
            // Combine and deduplicate results
            List<QuickFact> allMatches = keyMatches.stream()
                    .collect(Collectors.toMap(QuickFact::getKey, qf -> qf, (existing, replacement) -> existing))
                    .values()
                    .stream()
                    .collect(Collectors.toList());
            
            valueMatches.forEach(qf -> {
                if (allMatches.stream().noneMatch(existing -> existing.getKey().equals(qf.getKey()))) {
                    allMatches.add(qf);
                }
            });
            
            // Sort by updated date
            allMatches.sort((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));
            
            return ContentResponse.success(allMatches);
            
        } catch (Exception e) {
            log.error("Error searching quick facts", e);
            return ContentResponse.error("Failed to search quick facts: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<String>> getAllCategories() {
        try {
            List<String> categories = quickFactRepository.findAllCategories();
            return ContentResponse.success(categories);
            
        } catch (Exception e) {
            log.error("Error retrieving categories", e);
            return ContentResponse.error("Failed to retrieve categories: " + e.getMessage());
        }
    }
    
    public ContentResponse<Map<String, Object>> getQuickFactStats() {
        try {
            long totalFacts = quickFactRepository.count();
            List<String> categories = quickFactRepository.findAllCategories();
            List<QuickFact> recentFacts = quickFactRepository.findTop10ByOrderByUpdatedAtDesc();
            
            Map<String, Object> stats = Map.of(
                    "totalFacts", totalFacts,
                    "totalCategories", categories.size(),
                    "categories", categories,
                    "recentlyUpdated", recentFacts.size()
            );
            
            return ContentResponse.success(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving quick fact stats", e);
            return ContentResponse.error("Failed to retrieve stats: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<Void> deleteQuickFact(String key) {
        Optional<QuickFact> quickFact = quickFactRepository.findById(key);
        
        if (quickFact.isEmpty()) {
            return ContentResponse.error("Quick fact not found with key: " + key);
        }
        
        try {
            quickFactRepository.deleteById(key);
            
            log.info("Deleted quick fact with key: {}", key);
            return ContentResponse.success(null, "Quick fact deleted successfully");
            
        } catch (Exception e) {
            log.error("Error deleting quick fact", e);
            return ContentResponse.error("Failed to delete quick fact: " + e.getMessage());
        }
    }
    
    public boolean quickFactExists(String key) {
        return quickFactRepository.existsByKey(key);
    }
    
    @Transactional
    public ContentResponse<List<QuickFact>> bulkUpdateQuickFacts(Map<String, String> keyValuePairs) {
        log.debug("Bulk updating {} quick facts", keyValuePairs.size());
        
        try {
            List<QuickFact> updatedFacts = keyValuePairs.entrySet().stream()
                    .map(entry -> {
                        String key = validationService.sanitizeInput(entry.getKey()).trim();
                        String value = validationService.sanitizeInput(entry.getValue());
                        
                        Optional<QuickFact> existing = quickFactRepository.findById(key);
                        if (existing.isPresent()) {
                            QuickFact fact = existing.get();
                            fact.setValue(value);
                            return fact;
                        } else {
                            return QuickFact.builder()
                                    .key(key)
                                    .value(value)
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());
            
            List<QuickFact> savedFacts = quickFactRepository.saveAll(updatedFacts);
            
            log.info("Bulk updated {} quick facts", savedFacts.size());
            return ContentResponse.success(savedFacts, "Quick facts updated successfully");
            
        } catch (Exception e) {
            log.error("Error bulk updating quick facts", e);
            return ContentResponse.error("Failed to bulk update quick facts: " + e.getMessage());
        }
    }
}