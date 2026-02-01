package com.personal.backend.repository;

import com.personal.backend.model.CurrentStockPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrentStockPriceRepository extends JpaRepository<CurrentStockPrice, String> {
    
    /**
     * Find prices for multiple symbols at once
     */
    List<CurrentStockPrice> findBySymbolIn(List<String> symbols);
}
