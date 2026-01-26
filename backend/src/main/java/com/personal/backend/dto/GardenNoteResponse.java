package com.personal.backend.dto;

import com.personal.backend.model.GrowthStage;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GardenNoteResponse {
    
    private Long id;
    private String title;
    private String content;
    private GrowthStage growthStage;
    private List<LifeLogEntryResponse> linkedEntries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}