package com.personal.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BearsGameInfo {
    
    private String opponent;
    private String location;
    
    @JsonProperty("date")
    private LocalDateTime gameDate;
    
    @JsonProperty("time")
    private String gameTime;
    
    private boolean isCompleted;
    
    // Internal score storage
    private Integer homeScore;
    private Integer awayScore;
    
    @JsonProperty("isHome")
    private boolean isHome;
    
    // Frontend-specific fields
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("isStale")
    private boolean isStale;
    
    // Computed properties for frontend compatibility
    
    @JsonProperty("bearsScore")
    public Integer getBearsScore() {
        if (homeScore == null || awayScore == null) return 0;
        return isHome ? homeScore : awayScore;
    }
    
    @JsonProperty("opponentScore")
    public Integer getOpponentScore() {
        if (homeScore == null || awayScore == null) return 0;
        return isHome ? awayScore : homeScore;
    }
    
    @JsonProperty("isWin")
    public boolean isWin() {
        if (homeScore == null || awayScore == null) return false;
        int bears = getBearsScore();
        int opp = getOpponentScore();
        return bears > opp;
    }
}