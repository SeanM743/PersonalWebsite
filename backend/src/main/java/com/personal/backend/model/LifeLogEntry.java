package com.personal.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "life_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LifeLogEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifeLogType type; // BOOK, MOVIE, SHOW, ALBUM, HOBBY
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    private EntryStatus status; // IN_PROGRESS, COMPLETED, PLANNED
    
    // Mahoney Rating System
    private Integer rating; // 1-5
    
    @Column(name = "key_takeaway", length = 500)
    private String keyTakeaway;
    
    // Hobby-specific field
    private Integer intensity; // 1-5, only for HOBBY type
    
    // Metadata fields
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "external_id")
    private String externalId;
    
    @Column(length = 1000)
    private String metadata; // JSON for additional type-specific data
    
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