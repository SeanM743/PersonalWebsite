package com.personal.backend.repository;

import com.personal.backend.model.StockDailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockDailyPriceRepository extends JpaRepository<StockDailyPrice, Long> {
    
    Optional<StockDailyPrice> findBySymbolAndDate(String symbol, LocalDate date);
    
    List<StockDailyPrice> findBySymbolAndDateBetweenOrderByDateAsc(
        String symbol, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT DISTINCT s.symbol FROM StockDailyPrice s")
    List<String> findDistinctSymbols();
    
    @Query("SELECT MAX(s.date) FROM StockDailyPrice s WHERE s.symbol = :symbol")
    Optional<LocalDate> findLatestDateForSymbol(@Param("symbol") String symbol);
}
