package com.personal.backend.repository;

import com.personal.backend.model.StockTicker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockTickerRepository extends JpaRepository<StockTicker, Long> {
    
    /**
     * Find all stock holdings for a specific user
     */
    List<StockTicker> findByUserIdOrderBySymbolAsc(Long userId);
    
    /**
     * Find a specific stock holding by user and symbol
     */
    Optional<StockTicker> findByUserIdAndSymbol(Long userId, String symbol);
    
    /**
     * Check if user already has a position in this stock
     */
    boolean existsByUserIdAndSymbol(Long userId, String symbol);
    
    /**
     * Get all unique symbols for a user (for market data fetching)
     */
    @Query("SELECT DISTINCT s.symbol FROM StockTicker s WHERE s.userId = :userId")
    List<String> findDistinctSymbolsByUserId(@Param("userId") Long userId);
    
    /**
     * Get all unique symbols across all users (for batch market data updates)
     */
    @Query("SELECT DISTINCT s.symbol FROM StockTicker s")
    List<String> findAllDistinctSymbols();
    
    /**
     * Count total positions for a user
     */
    long countByUserId(Long userId);
    
    /**
     * Get total investment amount for a user
     */
    @Query("SELECT COALESCE(SUM(s.purchasePrice * s.quantity), 0) FROM StockTicker s WHERE s.userId = :userId")
    BigDecimal getTotalInvestmentByUserId(@Param("userId") Long userId);
    
    /**
     * Get total current portfolio value for a user (using current market prices)
     * This is an optimized single-query approach to avoid N+1 queries
     */
    @Query("SELECT COALESCE(SUM(s.currentPrice * s.quantity), 0) FROM StockTicker s WHERE s.userId = :userId AND s.currentPrice IS NOT NULL")
    BigDecimal getTotalCurrentValueByUserId(@Param("userId") Long userId);
    
    /**
     * Find stocks by symbol pattern (for search functionality)
     */
    List<StockTicker> findByUserIdAndSymbolContainingIgnoreCaseOrderBySymbolAsc(Long userId, String symbolPattern);
    
    /**
     * Find stocks with notes containing specific text
     */
    List<StockTicker> findByUserIdAndNotesContainingIgnoreCaseOrderBySymbolAsc(Long userId, String notesPattern);
    
    /**
     * Find stocks created within a date range
     */
    List<StockTicker> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find stocks updated within a date range
     */
    List<StockTicker> findByUserIdAndUpdatedAtBetweenOrderByUpdatedAtDesc(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find stocks with quantity greater than specified amount
     */
    List<StockTicker> findByUserIdAndQuantityGreaterThanOrderByQuantityDesc(Long userId, BigDecimal minQuantity);
    
    /**
     * Find stocks with purchase price in a specific range
     */
    List<StockTicker> findByUserIdAndPurchasePriceBetweenOrderByPurchasePriceAsc(Long userId, BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * Find stocks with total investment greater than specified amount
     */
    @Query("SELECT s FROM StockTicker s WHERE s.userId = :userId AND (s.purchasePrice * s.quantity) > :minInvestment ORDER BY (s.purchasePrice * s.quantity) DESC")
    List<StockTicker> findByUserIdAndTotalInvestmentGreaterThan(@Param("userId") Long userId, @Param("minInvestment") BigDecimal minInvestment);
    
    /**
     * Get portfolio statistics for a user
     */
    @Query("""
        SELECT new map(
            COUNT(s) as totalPositions,
            COALESCE(SUM(s.purchasePrice * s.quantity), 0) as totalInvestment,
            COALESCE(AVG(s.purchasePrice * s.quantity), 0) as averagePositionSize,
            COALESCE(MIN(s.purchasePrice * s.quantity), 0) as smallestPosition,
            COALESCE(MAX(s.purchasePrice * s.quantity), 0) as largestPosition
        )
        FROM StockTicker s 
        WHERE s.userId = :userId
        """)
    Object getPortfolioStatistics(@Param("userId") Long userId);
    
    /**
     * Delete all positions for a user (for cleanup)
     */
    void deleteByUserId(Long userId);
    
    /**
     * Delete specific position by user and symbol
     */
    void deleteByUserIdAndSymbol(Long userId, String symbol);
    
    /**
     * Find stocks that haven't been updated recently (for maintenance)
     */
    List<StockTicker> findByUpdatedAtBeforeOrderByUpdatedAtAsc(LocalDateTime cutoffDate);
    
    /**
     * Get symbols that need market data updates (batch processing)
     */
    @Query("""
        SELECT DISTINCT s.symbol 
        FROM StockTicker s 
        WHERE s.updatedAt > :recentCutoff
        ORDER BY s.symbol
        """)
    List<String> findActiveSymbols(@Param("recentCutoff") LocalDateTime recentCutoff);
    
    /**
     * Check if user has reached maximum positions limit
     */
    @Query("SELECT COUNT(s) >= :maxPositions FROM StockTicker s WHERE s.userId = :userId")
    boolean hasReachedMaxPositions(@Param("userId") Long userId, @Param("maxPositions") long maxPositions);
    
    /**
     * Bulk update operations for portfolio performance calculations
     */
    @Query("""
        SELECT s FROM StockTicker s 
        WHERE s.userId = :userId 
        ORDER BY (s.purchasePrice * s.quantity) DESC
        """)
    List<StockTicker> findByUserIdOrderByTotalInvestmentDesc(@Param("userId") Long userId);
    
    /**
     * Get portfolio composition with percentage allocations
     */
    @Query("""
        SELECT new map(
            s.symbol as symbol,
            s.purchasePrice as purchasePrice,
            s.quantity as quantity,
            (s.purchasePrice * s.quantity) as totalInvestment,
            ROUND(
                (s.purchasePrice * s.quantity) * 100.0 / 
                (SELECT SUM(st.purchasePrice * st.quantity) FROM StockTicker st WHERE st.userId = :userId),
                2
            ) as allocationPercentage
        )
        FROM StockTicker s 
        WHERE s.userId = :userId
        ORDER BY (s.purchasePrice * s.quantity) DESC
        """)
    List<Object> getPortfolioComposition(@Param("userId") Long userId);
    
    /**
     * Get aggregated portfolio metrics for performance calculations
     */
    @Query("""
        SELECT new map(
            COUNT(s) as totalPositions,
            COALESCE(SUM(s.purchasePrice * s.quantity), 0) as totalInvestment,
            COALESCE(AVG(s.purchasePrice * s.quantity), 0) as averagePositionSize,
            COALESCE(MIN(s.purchasePrice * s.quantity), 0) as smallestPosition,
            COALESCE(MAX(s.purchasePrice * s.quantity), 0) as largestPosition,
            COALESCE(STDDEV(s.purchasePrice * s.quantity), 0) as positionSizeStdDev
        )
        FROM StockTicker s 
        WHERE s.userId = :userId
        """)
    Object getAggregatedPortfolioMetrics(@Param("userId") Long userId);
    
    /**
     * Batch operations for market data updates
     */
    @Query("""
        SELECT s FROM StockTicker s 
        WHERE s.symbol IN :symbols 
        ORDER BY s.userId, s.symbol
        """)
    List<StockTicker> findBySymbolsForBatchUpdate(@Param("symbols") List<String> symbols);
    
    /**
     * Get positions that need recalculation (for performance optimization)
     */
    @Query("""
        SELECT s FROM StockTicker s 
        WHERE s.userId = :userId 
        AND s.updatedAt < :cutoffTime
        ORDER BY s.updatedAt ASC
        """)
    List<StockTicker> findPositionsNeedingRecalculation(@Param("userId") Long userId, @Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Efficient bulk operations for portfolio updates
     */
    @Query("""
        UPDATE StockTicker s 
        SET s.updatedAt = :updateTime 
        WHERE s.userId = :userId 
        AND s.symbol IN :symbols
        """)
    @org.springframework.data.jpa.repository.Modifying
    void bulkUpdateTimestamps(@Param("userId") Long userId, @Param("symbols") List<String> symbols, @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * Get portfolio diversity metrics
     */
    @Query("""
        SELECT new map(
            COUNT(DISTINCT s.symbol) as uniqueSymbols,
            COUNT(s) as totalPositions,
            COALESCE(MAX(s.purchasePrice * s.quantity), 0) as largestPosition,
            COALESCE(MIN(s.purchasePrice * s.quantity), 0) as smallestPosition,
            CASE 
                WHEN COUNT(s) > 0 THEN 
                    COALESCE(MAX(s.purchasePrice * s.quantity), 0) / COALESCE(MIN(s.purchasePrice * s.quantity), 1)
                ELSE 0 
            END as concentrationRatio
        )
        FROM StockTicker s 
        WHERE s.userId = :userId
        """)
    Object getPortfolioDiversityMetrics(@Param("userId") Long userId);
}