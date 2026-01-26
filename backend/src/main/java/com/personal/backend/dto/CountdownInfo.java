package com.personal.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountdownInfo {
    
    private LocalDate targetDate;
    private long daysRemaining;
    private String label;
    private boolean isPast;
    private String description;
}