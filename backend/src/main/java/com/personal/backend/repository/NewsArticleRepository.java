package com.personal.backend.repository;

import com.personal.backend.model.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    List<NewsArticle> findByCategoryIdOrderByPublishedAtDesc(Long categoryId);
    void deleteByCategoryId(Long categoryId);
}
