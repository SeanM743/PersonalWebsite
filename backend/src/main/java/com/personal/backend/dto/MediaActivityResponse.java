package com.personal.backend.dto;

import com.personal.backend.model.ActivityStatus;
import com.personal.backend.model.MediaType;
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
public class MediaActivityResponse {
    
    private Long id;
    private String title;
    private MediaType mediaType;
    private ActivityStatus status;
    private String creator;
    private String coverUrl;
    private Integer rating;
    private LocalDate startDate;
    private LocalDate completionDate;
    private String notes;
    private String externalId;
    private String publisher;
    private Integer releaseYear;
    private String genre;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}