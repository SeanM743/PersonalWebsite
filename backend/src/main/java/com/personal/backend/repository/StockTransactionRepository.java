package com.personal.backend.repository;

import com.personal.backend.model.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    
    List<StockTransaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    List<StockTransaction> findByUserIdOrderByTransactionDateAsc(Long userId);
    
    List<StockTransaction> findByUserIdAndSymbolOrderByTransactionDateDesc(Long userId, String symbol);
    
    Page<StockTransaction> findByUserId(Long userId, Pageable pageable);
}
