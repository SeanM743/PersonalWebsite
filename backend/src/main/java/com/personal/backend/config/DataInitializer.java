package com.personal.backend.config;

import com.personal.backend.model.Account;
import com.personal.backend.model.AccountTransaction;
import com.personal.backend.model.FamilyMember;
import com.personal.backend.model.StockTransaction;
import com.personal.backend.repository.AccountRepository;
import com.personal.backend.repository.AccountTransactionRepository;
import com.personal.backend.repository.FamilyMemberRepository;
import com.personal.backend.repository.StockTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private AccountTransactionRepository accountTransactionRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeFamilyMembers();
        initializeBankAccount();
        backfillAccountTransactions();
    }

    private void initializeFamilyMembers() {
        // Check if family members already exist
        if (familyMemberRepository.count() > 0) {
            return; // Data already initialized
        }

        // Initialize Madelyn (WSU)
        FamilyMember madelyn = new FamilyMember();
        madelyn.setName("Madelyn");
        madelyn.setPrimaryActivity("WSU Student");
        madelyn.setStatus("Doing great at college");
        madelyn.setNotes("Studying hard and making new friends");
        madelyn.setUpdatedAt(LocalDateTime.now());
        familyMemberRepository.save(madelyn);

        // Initialize Evalyn (Driving)
        FamilyMember evalyn = new FamilyMember();
        evalyn.setName("Evalyn");
        evalyn.setPrimaryActivity("Learning to Drive");
        evalyn.setStatus("Getting more confident behind the wheel");
        evalyn.setNotes("Practicing parallel parking and highway driving");
        evalyn.setUpdatedAt(LocalDateTime.now());
        familyMemberRepository.save(evalyn);

        // Initialize Nate (School/Sports)
        FamilyMember nate = new FamilyMember();
        nate.setName("Nate");
        nate.setPrimaryActivity("School & Sports");
        nate.setStatus("Balancing academics and athletics");
        nate.setNotes("Doing well in classes and enjoying team sports");
        nate.setUpdatedAt(LocalDateTime.now());
        familyMemberRepository.save(nate);

        System.out.println("Family Pulse data initialized with 3 family members");
    }

    private void initializeBankAccount() {
        // Create "Bank Account" if it doesn't already exist
        if (accountRepository.findByName("Bank Account") != null) {
            return; // Already exists
        }

        Account bankAccount = Account.builder()
                .name("Bank Account")
                .type(Account.AccountType.CASH)
                .balance(new BigDecimal("10000.0000"))
                .isManual(true)
                .build();
        accountRepository.save(bankAccount);
        System.out.println("Bank Account initialized with $10,000 balance");
    }

    /**
     * Backfill AccountTransaction records for existing stock transactions
     * that don't yet have a corresponding ledger entry.
     * Replays transactions in chronological order against Fidelity Cash.
     */
    private void backfillAccountTransactions() {
        // One-time backfill: skip if records already exist
        if (accountTransactionRepository.count() > 0) {
            return;
        }

        Account fidelityCash = accountRepository.findByName("Fidelity Cash");
        if (fidelityCash == null) {
            return; // No Fidelity Cash account to backfill
        }

        // Get all transactions in chronological order (oldest first)
        List<StockTransaction> allTxns = stockTransactionRepository.findByUserIdOrderByTransactionDateAsc(1L);
        if (allTxns.isEmpty()) {
            return;
        }

        // We'll replay from the current balance backwards to determine the starting balance,
        // then replay forward to create accurate old/new balance records.
        // First, calculate total impact to determine what the starting balance was.
        BigDecimal totalImpact = BigDecimal.ZERO;
        for (StockTransaction txn : allTxns) {
            if (txn.getTotalCost() == null) continue;
            if (txn.getType() == StockTransaction.TransactionType.BUY) {
                totalImpact = totalImpact.subtract(txn.getTotalCost());
            } else if (txn.getType() == StockTransaction.TransactionType.SELL) {
                totalImpact = totalImpact.add(txn.getTotalCost());
            }
        }

        // Starting balance = current balance - total impact
        BigDecimal runningBalance = fidelityCash.getBalance().subtract(totalImpact);
        int count = 0;

        for (StockTransaction txn : allTxns) {
            if (txn.getTotalCost() == null) continue;

            BigDecimal oldBalance = runningBalance;
            AccountTransaction.TransactionType acctTxnType;
            String description;

            if (txn.getType() == StockTransaction.TransactionType.BUY) {
                runningBalance = runningBalance.subtract(txn.getTotalCost());
                acctTxnType = AccountTransaction.TransactionType.DEBIT;
                description = "BUY " + txn.getQuantity().stripTrailingZeros().toPlainString() + " " + txn.getSymbol() + " @ $" + txn.getPricePerShare().stripTrailingZeros().toPlainString();
            } else if (txn.getType() == StockTransaction.TransactionType.SELL) {
                runningBalance = runningBalance.add(txn.getTotalCost());
                acctTxnType = AccountTransaction.TransactionType.CREDIT;
                description = "SELL " + txn.getQuantity().stripTrailingZeros().toPlainString() + " " + txn.getSymbol() + " @ $" + txn.getPricePerShare().stripTrailingZeros().toPlainString();
            } else {
                continue;
            }

            accountTransactionRepository.save(AccountTransaction.builder()
                    .accountId(fidelityCash.getId())
                    .transactionDate(txn.getTransactionDate())
                    .amount(txn.getTotalCost())
                    .oldBalance(oldBalance)
                    .newBalance(runningBalance)
                    .type(acctTxnType)
                    .description(description)
                    .relatedStockTransactionId(txn.getId())
                    .build());
            count++;
        }

        System.out.println("Backfilled " + count + " account transactions for Fidelity Cash");
    }
}
