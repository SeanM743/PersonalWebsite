package com.personal.backend.dto;

import com.personal.backend.model.TripStatus;
import com.personal.backend.model.TripType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripRequest {
    
    @NotBlank(message = "Destination is required")
    private String destination;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private TripType tripType;
    
    private String description;
    
    private String plannedActivities;
    
    private TripStatus status;
}