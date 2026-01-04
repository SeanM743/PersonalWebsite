package com.personal.backend.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

@Slf4j
public class FinancialCalculationUtil {
    
    // Standard precision for financial calculations
    public static final int PRICE_SCALE = 4;
    public static final int PERCENTAGE_SCALE = 4;
    public static final int CURRENCY_SCALE = 2;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
    
    // Common financial constants
    public static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private static final NumberFormat PERCENTAGE_FORMAT = NumberFormat.getPercentInstance(Locale.US);
    
    static {
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        PERCENTAGE_FORMAT.setMaximumFractionDigits(2);
        PERCENTAGE_FORMAT.setMinimumFractionDigits(2);
    }
    
    /**
     * Calculate total investment amount
     */
    public static BigDecimal calculateTotalInvestment(BigDecimal price, BigDecimal quantity) {
        if (price == null || quantity == null) {
            return ZERO;
        }
        return price.multiply(quantity).setScale(CURRENCY_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Calculate current value
     */
    public static BigDecimal calculateCurrentValue(BigDecimal currentPrice, BigDecimal quantity) {
        if (currentPrice == null || quantity == null) {
            return null;
        }
        return currentPrice.multiply(quantity).setScale(CURRENCY_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Calculate gain/loss amount
     */
    public static BigDecimal calculateGainLoss(BigDecimal currentValue, BigDecimal totalInvestment) {
        if (currentValue == null || totalInvestment == null) {
            return null;
        }
        return currentValue.subtract(totalInvestment).setScale(CURRENCY_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Calculate gain/loss percentage
     */
    public static BigDecimal calculateGainLossPercentage(BigDecimal gainLoss, BigDecimal totalInvestment) {
        if (gainLoss == null || totalInvestment == null || totalInvestment.compareTo(ZERO) == 0) {
            return null;
        }
        
        return gainLoss.divide(totalInvestment, PERCENTAGE_SCALE, DEFAULT_ROUNDING)
                .multiply(ONE_HUNDRED);
    }
    
    /**
     * Calculate daily change amount
     */
    public static BigDecimal calculateDailyChange(BigDecimal currentPrice, BigDecimal previousClose) {
        if (currentPrice == null || previousClose == null) {
            return null;
        }
        return currentPrice.subtract(previousClose).setScale(PRICE_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Calculate daily change percentage
     */
    public static BigDecimal calculateDailyChangePercentage(BigDecimal dailyChange, BigDecimal previousClose) {
        if (dailyChange == null || previousClose == null || previousClose.compareTo(ZERO) == 0) {
            return null;
        }
        
        return dailyChange.divide(previousClose, PERCENTAGE_SCALE, DEFAULT_ROUNDING)
                .multiply(ONE_HUNDRED);
    }
    
    /**
     * Calculate portfolio total value
     */
    public static BigDecimal calculatePortfolioTotalValue(List<BigDecimal> currentValues) {
        if (currentValues == null || currentValues.isEmpty()) {
            return ZERO;
        }
        
        return currentValues.stream()
                .filter(value -> value != null)
                .reduce(ZERO, BigDecimal::add)
                .setScale(CURRENCY_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Calculate portfolio total investment
     */
    public static BigDecimal calculatePortfolioTotalInvestment(List<BigDecimal> investments) {
        if (investments == null || investments.isEmpty()) {
            return ZERO;
        }
        
        return investments.stream()
                .filter(investment -> investment != null)
                .reduce(ZERO, BigDecimal::add)
                .setScale(CURRENCY_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Calculate weighted average
     */
    public static BigDecimal calculateWeightedAverage(List<BigDecimal> values, List<BigDecimal> weights) {
        if (values == null || weights == null || values.size() != weights.size() || values.isEmpty()) {
            return null;
        }
        
        BigDecimal weightedSum = ZERO;
        BigDecimal totalWeight = ZERO;
        
        for (int i = 0; i < values.size(); i++) {
            BigDecimal value = values.get(i);
            BigDecimal weight = weights.get(i);
            
            if (value != null && weight != null && weight.compareTo(ZERO) > 0) {
                weightedSum = weightedSum.add(value.multiply(weight));
                totalWeight = totalWeight.add(weight);
            }
        }
        
        if (totalWeight.compareTo(ZERO) == 0) {
            return null;
        }
        
        return weightedSum.divide(totalWeight, PRICE_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Validate price value
     */
    public static boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(ZERO) > 0;
    }
    
    /**
     * Validate quantity value
     */
    public static boolean isValidQuantity(BigDecimal quantity) {
        return quantity != null && quantity.compareTo(ZERO) > 0;
    }
    
    /**
     * Validate percentage value (between -100 and positive infinity)
     */
    public static boolean isValidPercentage(BigDecimal percentage) {
        return percentage != null && percentage.compareTo(BigDecimal.valueOf(-100)) > 0;
    }
    
    /**
     * Safe division with null checks
     */
    public static BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor, int scale) {
        if (dividend == null || divisor == null || divisor.compareTo(ZERO) == 0) {
            return null;
        }
        return dividend.divide(divisor, scale, DEFAULT_ROUNDING);
    }
    
    /**
     * Safe multiplication with null checks
     */
    public static BigDecimal safeMultiply(BigDecimal multiplicand, BigDecimal multiplier) {
        if (multiplicand == null || multiplier == null) {
            return null;
        }
        return multiplicand.multiply(multiplier);
    }
    
    /**
     * Safe addition with null checks
     */
    public static BigDecimal safeAdd(BigDecimal augend, BigDecimal addend) {
        if (augend == null && addend == null) {
            return null;
        }
        if (augend == null) {
            return addend;
        }
        if (addend == null) {
            return augend;
        }
        return augend.add(addend);
    }
    
    /**
     * Safe subtraction with null checks
     */
    public static BigDecimal safeSubtract(BigDecimal minuend, BigDecimal subtrahend) {
        if (minuend == null || subtrahend == null) {
            return null;
        }
        return minuend.subtract(subtrahend);
    }
    
    /**
     * Format currency amount
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "N/A";
        }
        return CURRENCY_FORMAT.format(amount);
    }
    
    /**
     * Format currency amount with sign
     */
    public static String formatCurrencyWithSign(BigDecimal amount) {
        if (amount == null) {
            return "N/A";
        }
        
        String formatted = CURRENCY_FORMAT.format(amount.abs());
        if (amount.compareTo(ZERO) > 0) {
            return "+" + formatted;
        } else if (amount.compareTo(ZERO) < 0) {
            return "-" + formatted;
        } else {
            return formatted;
        }
    }
    
    /**
     * Format percentage
     */
    public static String formatPercentage(BigDecimal percentage) {
        if (percentage == null) {
            return "N/A";
        }
        return PERCENTAGE_FORMAT.format(percentage.divide(ONE_HUNDRED, PERCENTAGE_SCALE, DEFAULT_ROUNDING));
    }
    
    /**
     * Format percentage with sign
     */
    public static String formatPercentageWithSign(BigDecimal percentage) {
        if (percentage == null) {
            return "N/A";
        }
        
        String formatted = PERCENTAGE_FORMAT.format(percentage.abs().divide(ONE_HUNDRED, PERCENTAGE_SCALE, DEFAULT_ROUNDING));
        if (percentage.compareTo(ZERO) > 0) {
            return "+" + formatted;
        } else if (percentage.compareTo(ZERO) < 0) {
            return "-" + formatted;
        } else {
            return formatted;
        }
    }
    
    /**
     * Format price with appropriate decimal places
     */
    public static String formatPrice(BigDecimal price) {
        if (price == null) {
            return "N/A";
        }
        
        // Use more decimal places for very small prices
        if (price.compareTo(BigDecimal.ONE) < 0) {
            return String.format("$%.4f", price);
        } else {
            return String.format("$%.2f", price);
        }
    }
    
    /**
     * Round to currency precision
     */
    public static BigDecimal roundToCurrency(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(CURRENCY_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Round to price precision
     */
    public static BigDecimal roundToPrice(BigDecimal price) {
        if (price == null) {
            return null;
        }
        return price.setScale(PRICE_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Round to percentage precision
     */
    public static BigDecimal roundToPercentage(BigDecimal percentage) {
        if (percentage == null) {
            return null;
        }
        return percentage.setScale(PERCENTAGE_SCALE, DEFAULT_ROUNDING);
    }
    
    /**
     * Check if two financial values are equal within tolerance
     */
    public static boolean isEqual(BigDecimal value1, BigDecimal value2, BigDecimal tolerance) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        
        BigDecimal difference = value1.subtract(value2).abs();
        return difference.compareTo(tolerance) <= 0;
    }
    
    /**
     * Check if financial value is effectively zero
     */
    public static boolean isEffectivelyZero(BigDecimal value) {
        if (value == null) {
            return true;
        }
        return value.abs().compareTo(BigDecimal.valueOf(0.01)) < 0;
    }
    
    // Convenience methods for backward compatibility
    public static BigDecimal subtract(BigDecimal minuend, BigDecimal subtrahend) {
        return safeSubtract(minuend, subtrahend);
    }
    
    public static BigDecimal multiply(BigDecimal multiplicand, BigDecimal multiplier) {
        return safeMultiply(multiplicand, multiplier);
    }
    
    public static BigDecimal add(BigDecimal augend, BigDecimal addend) {
        return safeAdd(augend, addend);
    }
    
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int scale) {
        return safeDivide(dividend, divisor, scale);
    }
}