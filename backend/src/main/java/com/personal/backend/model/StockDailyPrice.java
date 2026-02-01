package com.personal.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_daily_prices", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "date"}),
       indexes = {
           @Index(name = "idx_stock_daily_prices_symbol", columnList = "symbol"),
           @Index(name = "idx_stock_daily_prices_date", columnList = "date"),
           @Index(name = "idx_stock_daily_prices_symbol_date", columnList = "symbol,date")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDailyPrice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10)
    private String symbol;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal closePrice;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
