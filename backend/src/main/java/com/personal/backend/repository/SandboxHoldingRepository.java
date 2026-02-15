package com.personal.backend.repository;

import com.personal.backend.model.SandboxHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface SandboxHoldingRepository extends JpaRepository<SandboxHolding, Long> {
    Optional<SandboxHolding> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);
    List<SandboxHolding> findByPortfolioId(Long portfolioId);
}
