package com.personal.backend.repository;

import com.personal.backend.model.SandboxTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SandboxTransactionRepository extends JpaRepository<SandboxTransaction, Long> {
    List<SandboxTransaction> findByPortfolioIdOrderByTransactionDateDesc(Long portfolioId);
}
