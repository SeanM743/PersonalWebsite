package com.personal.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ValidationResult {
    
    private boolean valid = true;
    private Map<String, List<String>> errors = new HashMap<>();
    
    public void addError(String field, String message) {
        valid = false;
        errors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
    }
    
    public boolean hasErrors() {
        return !valid;
    }
    
    public boolean hasErrorsForField(String field) {
        return errors.containsKey(field) && !errors.get(field).isEmpty();
    }
    
    public List<String> getErrorsForField(String field) {
        return errors.getOrDefault(field, new ArrayList<>());
    }
    
    public Map<String, Object> toErrorMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("validation_errors", errors);
        return metadata;
    }
}