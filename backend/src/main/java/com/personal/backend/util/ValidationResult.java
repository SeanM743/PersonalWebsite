package com.personal.backend.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }
    
    public static ValidationResult failure(String errorMessage) {
        return ValidationResult.builder()
                .valid(false)
                .errors(List.of(errorMessage))
                .build();
    }
    
    public static ValidationResult invalid(List<String> errors, List<String> warnings) {
        return ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .warnings(warnings)
                .build();
    }
    
    public static ValidationResult invalid(List<String> errors) {
        return ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .build();
    }
    
    public static ValidationResult valid(List<String> warnings) {
        return ValidationResult.builder()
                .valid(true)
                .warnings(warnings)
                .build();
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public boolean isInvalid() {
        return !valid;
    }
}