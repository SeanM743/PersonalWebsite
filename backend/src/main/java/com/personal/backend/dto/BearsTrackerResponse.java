package com.personal.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BearsTrackerResponse {
    private BearsGameInfo nextGame;
    private BearsGameInfo lastGame;
    private TeamRecord record;
    
    @JsonProperty("isApiAvailable")
    private boolean isApiAvailable;
    
    private LocalDateTime lastUpdated;
    
    @JsonProperty("isCachedData")
    private boolean isCachedData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamRecord {
        private int wins;
        private int losses;
        private int ties;
    }
}
