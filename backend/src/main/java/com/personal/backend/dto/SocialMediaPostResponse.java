package com.personal.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialMediaPostResponse {
    
    private Long id;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    private List<String> imageUrls;
    private String comments;
    private String caption;
    
    // Alias method for backward compatibility
    public LocalDateTime getCreatedAt() {
        return publishedAt;
    }
}