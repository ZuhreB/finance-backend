package com.finance.finance.service;

import com.finance.finance.entity.Transaction;
import com.finance.finance.entity.TransactionType;
import com.finance.finance.entity.User;
import com.finance.finance.repository.TransactionRepository;
import com.finance.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

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
    public void deleteTransaction(Long transactionId, Long userId) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .filter(t -> t.getUser().getId().equals(userId)) // Güvenlik kontrolü
                .orElseThrow(() -> new RuntimeException("İşlem bulunamadı veya yetkiniz yok: " + transactionId));
        transactionRepository.delete(transaction);
    }
    public List<Transaction> getIncomeTransactionsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));
        return transactionRepository.findByUserAndType(user, TransactionType.INCOME);
    }

    public List<Transaction> getExpenseTransactionsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));
        return transactionRepository.findByUserAndType(user, TransactionType.EXPENSE);

    }

    public BigDecimal getMonthlyIncome(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1); // Ayın son günü
        return transactionRepository.findByUserAndTypeAndTransactionDateBetween(user, TransactionType.INCOME, startDate, endDate)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    public BigDecimal getMonthlyExpense(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        return transactionRepository.findByUserAndTypeAndTransactionDateBetween(user, TransactionType.EXPENSE, startDate, endDate)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Transaction> getUserTransactionsByDateRangeAndType(User user, LocalDate startDate, LocalDate endDate, TransactionType type) {
        List<Transaction> transactions = transactionRepository.findByUserAndTransactionDateBetween(user, startDate, endDate);
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
    }
}