package com.finance.finance.repository;

import com.finance.finance.entity.Transaction;
import com.finance.finance.entity.TransactionType;
import com.finance.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);
    List<Transaction> findByUserAndTransactionDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByUserAndType(User user, TransactionType type);
}