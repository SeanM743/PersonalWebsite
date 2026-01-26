package com.personal.backend.repository;

import com.personal.backend.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByUserIdOrderBySymbolAsc(Long userId);
    Optional<WatchlistItem> findByUserIdAndSymbol(Long userId, String symbol);
}
