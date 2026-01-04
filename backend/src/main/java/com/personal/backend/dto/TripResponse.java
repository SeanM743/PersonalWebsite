package com.personal.backend.dto;

import com.personal.backend.model.TripStatus;
import com.personal.backend.model.TripType;
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
public class TripResponse {
    
    private Long id;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private TripType tripType;
    private String description;
    private String plannedActivities;
    private TripStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}