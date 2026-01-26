package com.personal.backend.dto;

import com.personal.backend.model.EntryStatus;
import com.personal.backend.model.LifeLogType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LifeLogEntryResponse {
    
    private Long id;
    private String title;
    private LifeLogType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private EntryStatus status;
    
    // Mahoney Rating System
    private Integer rating;
    private String keyTakeaway;
    
    // Hobby-specific
    private Integer intensity;
    
    // Metadata
    private String imageUrl;
    private String externalId;
    private String metadata;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}