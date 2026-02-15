package com.personal.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_articles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private NewsCategory category;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String summary; // Short summary (max 3 sentences)
    
    @Column(columnDefinition = "TEXT")
    private String content; // Detailed writeup (expanded)
    
    private String url; // external link
    private String source; // e.g. "CNN", "TechCrunch"
    private String imageUrl;
    
    private LocalDateTime publishedAt;
    
    @Builder.Default
    private Integer relevanceScore = 5; // 1-10 scale, higher = more relevant
    
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Helper method to update content
    public void updateContent(String summary, String content) {
        this.summary = summary;
        this.content = content;
        this.createdAt = LocalDateTime.now(); // Refresh timestamp on update
    }
}
