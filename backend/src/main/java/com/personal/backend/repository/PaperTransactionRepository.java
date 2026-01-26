package com.personal.backend.repository;

import com.personal.backend.model.PaperTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperTransactionRepository extends JpaRepository<PaperTransaction, Long> {
    List<PaperTransaction> findByUserIdOrderByTransactionDateDesc(Long userId);
}
