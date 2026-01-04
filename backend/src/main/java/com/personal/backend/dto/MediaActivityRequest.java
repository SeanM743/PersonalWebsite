package com.personal.backend.dto;

import com.personal.backend.model.ActivityStatus;
import com.personal.backend.model.MediaType;
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
public class MediaActivityRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotNull(message = "Media type is required")
    private MediaType mediaType;
    
    private String creator;
    
    private ActivityStatus status;
    
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;
    
    private LocalDate startDate;
    
    private LocalDate completionDate;
    
    private String notes;
    
    private String externalId; // For API lookup
}