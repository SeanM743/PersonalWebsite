package com.personal.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_activity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MediaActivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private MediaType mediaType;
    
    @Enumerated(EnumType.STRING)
    private ActivityStatus status;
    
    private String creator; // Author, Director, Artist, etc.
    
    @Column(name = "cover_url")
    private String coverUrl;
    
    private Integer rating; // 1-5 scale
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "completion_date")
    private LocalDate completionDate;
    
    private String notes;
    
    // External metadata
    @Column(name = "external_id")
    private String externalId; // ISBN, IMDB ID, Spotify ID, etc.
    
    private String publisher;
    
    @Column(name = "release_year")
    private Integer releaseYear;
    
    private String genre;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}