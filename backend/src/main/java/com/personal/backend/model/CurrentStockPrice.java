package com.personal.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cache table for current stock prices.
 * Stores the latest price for each symbol to avoid redundant API calls.
 */
@Entity
@Table(name = "current_stock_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentStockPrice {
    
    @Id
    @Column(length = 10)
    private String symbol;  // Stock ticker symbol (e.g., "AAPL")
    
    @Column
    private String companyName;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal price;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal dailyChange;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal dailyChangePercent;
    
    @Column(nullable = false)
    private LocalDateTime fetchedAt;  // When this price was retrieved from the API
    
    @Column
    private Boolean marketOpenWhenFetched;  // Was the market open when we fetched this?
    
    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        if (this.fetchedAt == null) {
            this.fetchedAt = LocalDateTime.now();
        }
    }
}
