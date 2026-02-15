package com.personal.backend.repository;

import com.personal.backend.model.SandboxPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SandboxPortfolioRepository extends JpaRepository<SandboxPortfolio, Long> {
    List<SandboxPortfolio> findByUserId(Long userId);
}
