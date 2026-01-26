package com.personal.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "quick_fact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickFact {
    
    @Id
    private String key;
    
    @Column(nullable = false)
    private String value;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    private String category;
    
    private String description;
    
    // Rich data fields for enhanced functionality
    @Column(name = "external_id")
    private String externalId; // ID from external API (e.g., book ISBN, movie TMDB ID)
    
    @Column(name = "image_url")
    private String imageUrl; // Cover art, poster, etc.
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data
    
    @Column(name = "source_url")
    private String sourceUrl; // Link to external source
    
    @Column(name = "is_enriched")
    private Boolean isEnriched = false; // Whether data has been fetched from external APIs
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}