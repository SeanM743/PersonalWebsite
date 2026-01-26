package com.personal.backend.dto;

import com.personal.backend.model.EntryStatus;
import com.personal.backend.model.LifeLogType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LifeLogEntryRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotNull(message = "Type is required")
    private LifeLogType type;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private EntryStatus status;
    
    // Mahoney Rating System
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;
    
    private String keyTakeaway;
    
    // Hobby-specific field - validation handled in service layer
    @Min(value = 1, message = "Intensity must be between 1 and 5")
    @Max(value = 5, message = "Intensity must be between 1 and 5")
    private Integer intensity;
    
    // Metadata fields
    private String imageUrl;
    private String externalId;
    private String metadata;
}