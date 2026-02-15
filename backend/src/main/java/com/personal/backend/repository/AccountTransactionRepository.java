package com.personal.backend.repository;

import com.personal.backend.model.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    List<AccountTransaction> findByAccountIdOrderByTransactionDateDescCreatedAtDesc(Long accountId);
    
    List<AccountTransaction> findByAccountIdAndTransactionDateBetween(Long accountId, LocalDate startDate, LocalDate endDate);
}
