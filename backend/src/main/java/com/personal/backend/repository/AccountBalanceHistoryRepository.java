package com.personal.backend.repository;

import com.personal.backend.model.AccountBalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccountBalanceHistoryRepository extends JpaRepository<AccountBalanceHistory, Long> {
    
    List<AccountBalanceHistory> findByAccountId(Long accountId);
    
    List<AccountBalanceHistory> findByAccountIdAndDateBetweenOrderByDateAsc(Long accountId, LocalDate startDate, LocalDate endDate);
    
    // Find history for all accounts in a range (could be large, handle with care or service logic)
    List<AccountBalanceHistory> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);
}
