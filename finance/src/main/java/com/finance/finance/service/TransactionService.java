package com.finance.finance.service;

import com.finance.finance.entity.Transaction;
import com.finance.finance.entity.TransactionType;
import com.finance.finance.entity.User;
import com.finance.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction createTransaction(User user, BigDecimal amount, TransactionType type,
                                         String description, String category, LocalDate transactionDate) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setCategory(category);
        transaction.setTransactionDate(transactionDate);

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getUserTransactions(User user) {
        return transactionRepository.findByUser(user);
    }

    public List<Transaction> getUserTransactionsByDateRange(User user, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByUserAndTransactionDateBetween(user, startDate, endDate);
    }

    public List<Transaction> getUserTransactionsByType(User user, TransactionType type) {
        return transactionRepository.findByUserAndType(user, type);
    }

    public BigDecimal getUserBalance(User user) {
        List<Transaction> transactions = getUserTransactions(user);
        BigDecimal balance = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                balance = balance.add(transaction.getAmount());
            } else {
                balance = balance.subtract(transaction.getAmount());
            }
        }

        return balance;
    }
}