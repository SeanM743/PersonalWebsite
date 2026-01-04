package com.personal.backend.repository;

import com.personal.backend.model.SocialMediaPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SocialMediaPostRepository extends JpaRepository<SocialMediaPost, Long> {
    
    // Find posts ordered by publication date (newest first)
    List<SocialMediaPost> findAllByOrderByPublishedAtDesc();
    
    // Find posts with pagination ordered by publication date
    Page<SocialMediaPost> findAllByOrderByPublishedAtDesc(Pageable pageable);
    
    // Find posts published after a certain date
    List<SocialMediaPost> findByPublishedAtAfterOrderByPublishedAtDesc(LocalDateTime date);
    
    // Find posts containing specific content
    @Query("SELECT p FROM SocialMediaPost p WHERE p.content LIKE %:content% ORDER BY p.publishedAt DESC")
    List<SocialMediaPost> findByContentContainingIgnoreCase(String content);
    
    // Find posts with images
    @Query("SELECT p FROM SocialMediaPost p WHERE SIZE(p.imageUrls) > 0 ORDER BY p.publishedAt DESC")
    List<SocialMediaPost> findPostsWithImages();
    
    // Count posts published today - TEMPORARILY DISABLED DUE TO HIBERNATE ISSUE
    // @Query("SELECT COUNT(p) FROM SocialMediaPost p WHERE DATE(p.publishedAt) = CURRENT_DATE")
    // long countPostsToday();
}