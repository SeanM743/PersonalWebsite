package com.personal.backend.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response models for Finnhub API integration
 */
public class FinnhubApiResponse {
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Quote {
        @JsonProperty("c")
        private BigDecimal currentPrice;
        
        @JsonProperty("d")
        private BigDecimal change;
        
        @JsonProperty("dp")
        private BigDecimal percentChange;
        
        @JsonProperty("h")
        private BigDecimal highPrice;
        
        @JsonProperty("l")
        private BigDecimal lowPrice;
        
        @JsonProperty("o")
        private BigDecimal openPrice;
        
        @JsonProperty("pc")
        private BigDecimal previousClose;
        
        @JsonProperty("t")
        private Long timestamp;
        
        public boolean isValid() {
            return currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0;
        }
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompanyProfile {
        @JsonProperty("country")
        private String country;
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("exchange")
        private String exchange;
        
        @JsonProperty("ipo")
        private String ipoDate;
        
        @JsonProperty("marketCapitalization")
        private BigDecimal marketCap;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("phone")
        private String phone;
        
        @JsonProperty("shareOutstanding")
        private BigDecimal sharesOutstanding;
        
        @JsonProperty("ticker")
        private String ticker;
        
        @JsonProperty("weburl")
        private String website;
        
        @JsonProperty("logo")
        private String logo;
        
        @JsonProperty("finnhubIndustry")
        private String industry;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MarketStatus {
        @JsonProperty("exchange")
        private String exchange;
        
        @JsonProperty("holiday")
        private String holiday;
        
        @JsonProperty("isOpen")
        private Boolean isOpen;
        
        @JsonProperty("session")
        private String session;
        
        @JsonProperty("timezone")
        private String timezone;
        
        @JsonProperty("t")
        private Long timestamp;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorResponse {
        @JsonProperty("error")
        private String error;
        
        @JsonProperty("message")
        private String message;
        
        public boolean hasError() {
            return error != null && !error.isEmpty();
        }
    }
}