package com.personal.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "social_media_post")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialMediaPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ElementCollection
    @CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "social_media_post_id"))
    @Column(name = "image_urls")
    private List<String> imageUrls;
    
    @Column(columnDefinition = "TEXT")
    private String comments;
    
    private String caption;
    
    @PrePersist
    protected void onCreate() {
        publishedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}