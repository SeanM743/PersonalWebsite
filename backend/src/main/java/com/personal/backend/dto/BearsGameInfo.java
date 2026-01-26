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
public class BearsGameInfo {
    
    private String opponent;
    private String location;
    private LocalDateTime gameDate;
    private String gameTime;
    private boolean isCompleted;
    private Integer homeScore;
    private Integer awayScore;
    private boolean isHome;
    private String status; // "SCHEDULED", "IN_PROGRESS", "COMPLETED"
    private boolean isStale; // Indicates if data is from cache due to API unavailability
}