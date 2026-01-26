package com.personal.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMemberResponse {
    
    private Long id;
    private String name;
    private String primaryActivity;
    private String status;
    private String notes;
    private LocalDateTime updatedAt;
}