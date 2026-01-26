package com.personal.backend.service;

import com.personal.backend.model.Account;
import com.personal.backend.repository.AccountRepository;
import com.personal.backend.model.StockTransaction;
import com.personal.backend.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Service to initialize portfolio data with real transaction history
 * Runs automatically on application startup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioDataInitializer implements CommandLineRunner {

    private final StockTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            initializeAccounts();
            initializeStockTransactions();
        } catch (Exception e) {
            log.error("Failed to initialize portfolio data: {}", e.getMessage(), e);
        }
    }

    private void initializeAccounts() {
        if (accountRepository.count() > 0) {
            log.info("Accounts already initialized. Checking for updates...");
            updateFidelityCashBalance(); // Force update if needed
            renameAmazonAccount();      // Rename Amazon Account to Amazon 401k
            return;
        }

        log.info("Initializing financial accounts...");

        // 1. Stock Portfolio (Dynamic)
        accountRepository.save(Account.builder()
                .name("Stock Portfolio")
                .type(Account.AccountType.STOCK_PORTFOLIO)
                .balance(BigDecimal.ZERO) // Calculated from holdings
                .isManual(false)
                .build());

        // 2. Fidelity Cash
        accountRepository.save(Account.builder()
                .name("Fidelity Cash")
                .type(Account.AccountType.CASH)
                .balance(new BigDecimal("394828.35")) // Updated per user request
                .isManual(true)
                .build());

        // 3. Explicit User Accounts
        List<Account> userAccounts = List.of(
            Account.builder().name("Roth IRA").type(Account.AccountType.RETIREMENT).balance(new BigDecimal("27478.81")).build(),
            Account.builder().name("Amazon 401k").type(Account.AccountType.OTHER).balance(new BigDecimal("122618.65")).build(),
            Account.builder().name("Amazon RSU").type(Account.AccountType.OTHER).balance(new BigDecimal("228876.12")).build(),
            Account.builder().name("Evalyn's College Fund").type(Account.AccountType.EDUCATION).balance(new BigDecimal("24866.64")).build(),
            Account.builder().name("Madelyn's College Fund").type(Account.AccountType.EDUCATION).balance(new BigDecimal("23362.45")).build(),
            Account.builder().name("Nathan's College Fund").type(Account.AccountType.EDUCATION).balance(new BigDecimal("23682.82")).build(),
            Account.builder().name("Thrift Savings Plan").type(Account.AccountType.RETIREMENT).balance(new BigDecimal("219013.78")).build()
        );
        
        accountRepository.saveAll(userAccounts);
        log.info("Successfully seeded {} financial accounts", userAccounts.size() + 2);
    }

    private void updateFidelityCashBalance() {
        Account account = accountRepository.findByName("Fidelity Cash");
        if (account != null) {
            boolean changed = false;
            // Update balance if it's the old default
            if (account.getBalance().compareTo(new BigDecimal("10000.00")) == 0) {
                account.setBalance(new BigDecimal("394828.35"));
                changed = true;
            }
            if (changed) {
                accountRepository.save(account);
                log.info("Updated Fidelity Cash balance to correct value");
            }
        }
    }

    private void renameAmazonAccount() {
        Account account = accountRepository.findByName("Amazon Account");
        if (account != null) {
            account.setName("Amazon 401k");
            accountRepository.save(account);
            log.info("Renamed 'Amazon Account' to 'Amazon 401k'");
        }
    }

    private void initializeStockTransactions() {
        Long userId = 1L; // Admin user

        if (transactionRepository.count() > 0) {
            log.info("Stock transactions already exist. Skipping initialization.");
            return;
        }

        log.info("Initializing stock transaction history for user {}", userId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy", Locale.US);

        List<TransactionSeed> seeds = List.of(
            new TransactionSeed("ACHR", "Feb-20-2025", "5000", "9.57"),
            new TransactionSeed("AMZN", "Nov-17-2025", "434", "231.49"),
            new TransactionSeed("AMZN", "May-15-2025", "403", "204.21"),
            new TransactionSeed("AMZN", "Nov-21-2024", "257", "198.47"),
            new TransactionSeed("AMZN", "Nov-15-2024", "639", "203.10"),
            new TransactionSeed("AMZN", "May-21-2024", "257", "181.52"),
            new TransactionSeed("AMZN", "May-15-2024", "635", "184.35"),
            new TransactionSeed("AMZN", "Mar-15-2024", "321", "175.44"),
            new TransactionSeed("AMZN", "Nov-15-2023", "45", "144.42"),
            new TransactionSeed("AMZN", "Sep-15-2023", "348", "141.22"),
            new TransactionSeed("AMZN", "May-15-2023", "23", "110.38"),
            new TransactionSeed("ANET", "Feb-19-2025", "600", "102.65"),
            new TransactionSeed("CRWV", "Nov-14-2025", "148", "78.84"),
            new TransactionSeed("CRWV", "Nov-14-2025", "1", "78.89"),
            new TransactionSeed("CRWV", "Nov-14-2025", "0.128", "78.83"), // Assuming 0.128 based on provided text
            new TransactionSeed("CRWV", "Nov-14-2025", "485", "78.85"),
            new TransactionSeed("INTC", "Oct-29-2021", "0.672", "48.91"),
            new TransactionSeed("INTC", "Oct-29-2021", "306", "48.91"),
            new TransactionSeed("INTC", "May-10-2021", "600", "56.46"),
            new TransactionSeed("QQQ", "Jan-24-2022", "0.903", "341.21"),
            new TransactionSeed("QQQ", "Jan-24-2022", "109", "341.21"),
            new TransactionSeed("SOFI", "Feb-19-2025", "3500", "16.35"),
            new TransactionSeed("SOUN", "Feb-19-2025", "2500", "11.36"),
            new TransactionSeed("TMUS", "Nov-17-2021", "0.467", "116.64")
        );

        int count = 0;
        for (TransactionSeed seed : seeds) {
            try {
                // Handle comma in quantity (e.g. "5,000")
                String cleanQty = seed.quantity.replace(",", "");
                
                StockTransaction txn = StockTransaction.builder()
                        .userId(userId)
                        .symbol(seed.symbol)
                        .type(StockTransaction.TransactionType.BUY)
                        .transactionDate(LocalDate.parse(seed.date, formatter))
                        .quantity(new BigDecimal(cleanQty))
                        .pricePerShare(new BigDecimal(seed.price))
                        .totalCost(new BigDecimal(cleanQty).multiply(new BigDecimal(seed.price)))
                        .notes("Historical Import") // Identify these are imported
                        .build();

                transactionRepository.save(txn);
                count++;
            } catch (Exception e) {
                log.error("Failed to seed transaction {}: {}", seed, e.getMessage());
            }
        }

        log.info("Successfully seeded {} stock transactions", count);
    }

    private record TransactionSeed(String symbol, String date, String quantity, String price) {}
}