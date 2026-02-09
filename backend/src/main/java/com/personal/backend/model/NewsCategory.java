package com.personal.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId; // Assuming single user for now, but keeping scalable
    
    @Column(nullable = false)
    private String topic; // Natural language topic e.g. "Tech Stocks"
    
    @Column(columnDefinition = "TEXT")
    private String searchQuery; // Optimized NewsAPI query
    
    @Builder.Default
    private String tab = "Misc"; // "Financial", "Sports", "Politics", "Misc"
    
    private LocalDateTime lastFetchedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
