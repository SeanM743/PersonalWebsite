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
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}