package com.personal.backend.repository;

import com.personal.backend.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByType(Account.AccountType type);
    
    // Helper to find specific accounts by name (useful for seeding/lookup)
    Account findByName(String name);
}
