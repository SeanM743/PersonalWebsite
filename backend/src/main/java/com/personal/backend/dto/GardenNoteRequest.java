package com.personal.backend.dto;

import com.personal.backend.model.GrowthStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GardenNoteRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String content;
    
    @NotNull(message = "Growth stage is required")
    private GrowthStage growthStage;
    
    private List<Long> linkedEntryIds;
}