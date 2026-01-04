package com.personal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickFactRequest {
    
    @NotBlank(message = "Key is required")
    @Size(max = 100, message = "Key must be less than 100 characters")
    private String key;
    
    @NotBlank(message = "Value is required")
    @Size(max = 1000, message = "Value must be less than 1000 characters")
    private String value;
    
    @Size(max = 50, message = "Category must be less than 50 characters")
    private String category;
    
    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
}