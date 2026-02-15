package com.personal.backend.repository;

import com.personal.backend.model.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    List<NewsArticle> findByCategoryIdOrderByPublishedAtDesc(Long categoryId);
    List<NewsArticle> findByCategoryIdOrderByRelevanceScoreDescPublishedAtDesc(Long categoryId);
    void deleteByCategoryId(Long categoryId);
    
    // Duplicate detection - check if article URL already exists for this category
    boolean existsByCategoryIdAndUrl(Long categoryId, String url);
    
    // Alternative: check by title (for cases where URL might differ slightly)
    boolean existsByCategoryIdAndTitle(Long categoryId, String title);
}
