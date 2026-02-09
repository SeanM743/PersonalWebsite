package com.personal.backend.repository;

import com.personal.backend.model.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsCategoryRepository extends JpaRepository<NewsCategory, Long> {
    List<NewsCategory> findByUserId(Long userId);
    boolean existsByUserIdAndTopic(Long userId, String topic);
}
