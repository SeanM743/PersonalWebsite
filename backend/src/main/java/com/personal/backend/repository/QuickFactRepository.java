package com.personal.backend.repository;

import com.personal.backend.model.QuickFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuickFactRepository extends JpaRepository<QuickFact, String> {
    
    // Find facts by category
    List<QuickFact> findByCategoryOrderByUpdatedAtDesc(String category);
    
    // Find facts updated after a certain date
    List<QuickFact> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime date);
    
    // Find facts by key pattern (case insensitive)
    @Query("SELECT qf FROM QuickFact qf WHERE LOWER(qf.key) LIKE LOWER(CONCAT('%', :pattern, '%')) ORDER BY qf.updatedAt DESC")
    List<QuickFact> findByKeyContainingIgnoreCase(@Param("pattern") String pattern);
    
    // Find facts by value pattern (case insensitive)
    @Query("SELECT qf FROM QuickFact qf WHERE LOWER(qf.value) LIKE LOWER(CONCAT('%', :pattern, '%')) ORDER BY qf.updatedAt DESC")
    List<QuickFact> findByValueContainingIgnoreCase(@Param("pattern") String pattern);
    
    // Get all categories
    @Query("SELECT DISTINCT qf.category FROM QuickFact qf WHERE qf.category IS NOT NULL ORDER BY qf.category")
    List<String> findAllCategories();
    
    // Count facts by category
    @Query("SELECT COUNT(qf) FROM QuickFact qf WHERE qf.category = :category")
    long countByCategory(@Param("category") String category);
    
    // Find most recently updated facts
    List<QuickFact> findTop10ByOrderByUpdatedAtDesc();
    
    // Check if key exists
    boolean existsByKey(String key);
}