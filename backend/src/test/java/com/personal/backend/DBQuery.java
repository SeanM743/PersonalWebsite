package com.personal.backend;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DBQuery {
    static class HistoryPoint {
        LocalDate date;
        Long accountId;
        BigDecimal balance;
        
        public HistoryPoint(LocalDate d, Long id, BigDecimal b) {
            this.date = d;
            this.accountId = id;
            this.balance = b;
        }
    }

    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/personal_platform";
        String user = "admin";
        String password = "password";

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement st = con.createStatement()) {

            System.out.println("--- ACCOUNTS ---");
            Set<Long> stockAccountIds = new HashSet<>();
            ResultSet rs = st.executeQuery("SELECT id, name, type, balance FROM accounts ORDER BY id");
            while (rs.next()) {
                System.out.println(rs.getInt(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | " + rs.getBigDecimal(4));
                if ("STOCK_PORTFOLIO".equals(rs.getString(3))) {
                    stockAccountIds.add(rs.getLong(1));
                }
            }

            System.out.println("\n--- HOLDINGS ---");
            rs = st.executeQuery("SELECT symbol, quantity, current_price, purchase_price FROM stock_tickers ORDER BY symbol");
            while (rs.next()) {
                System.out.println(rs.getString(1) + " | Qty: " + rs.getBigDecimal(2) + " | Cur: " + rs.getBigDecimal(3) + " | Cost: " + rs.getBigDecimal(4));
            }

            System.out.println("\n--- HISTORY & CALCULATION ---");
            List<HistoryPoint> allHistory = new ArrayList<>();
            rs = st.executeQuery("SELECT date, account_id, balance FROM account_balance_history WHERE date >= '2026-01-01' ORDER BY date, account_id");
            while (rs.next()) {
                allHistory.add(new HistoryPoint(
                    rs.getDate(1).toLocalDate(),
                    rs.getLong(2),
                    rs.getBigDecimal(3)
                ));
            }
            
            // Logic replication
            Map<LocalDate, List<HistoryPoint>> historyByDate = allHistory.stream()
                    .collect(Collectors.groupingBy(h -> h.date, TreeMap::new, Collectors.toList()));
            
            boolean hasStockAccount = !stockAccountIds.isEmpty();
            Long stockAccountId = hasStockAccount ? stockAccountIds.iterator().next() : null;

            BigDecimal startOfYearValue = BigDecimal.ZERO;
            LocalDate baselineDate = null;

            for (Map.Entry<LocalDate, List<HistoryPoint>> entry : historyByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<HistoryPoint> points = entry.getValue();
                
                BigDecimal dailyTotal = points.stream()
                        .map(h -> h.balance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (dailyTotal.compareTo(BigDecimal.ZERO) <= 0) continue;
                
                if (hasStockAccount) {
                     boolean stockPresent = points.stream()
                             .anyMatch(h -> h.accountId.equals(stockAccountId));
                     if (!stockPresent) {
                         System.out.println("Skipping " + date + ": Stock account missing. Daily Total: " + dailyTotal);
                         continue;
                     }
                }
                
                startOfYearValue = dailyTotal;
                baselineDate = date;
                System.out.println("FOUND BASELINE: " + date + " | Value: " + dailyTotal);
                points.forEach(p -> System.out.println("  - Acc " + p.accountId + ": " + p.balance));
                break;
            }
            
            // Calculate hypothetical YTD based on Today's latest data in DB
            if (baselineDate != null) {
                // Get latest date
                LocalDate latestDate = historyByDate.keySet().stream().max(LocalDate::compareTo).orElse(LocalDate.now());
                List<HistoryPoint> latestPoints = historyByDate.get(latestDate);
                BigDecimal currentTotal = latestPoints.stream()
                        .map(h -> h.balance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal gain = currentTotal.subtract(startOfYearValue);
                System.out.println("\nLATEST DATE: " + latestDate + " | Total: " + currentTotal);
                System.out.println("YTD GAIN: " + gain);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
