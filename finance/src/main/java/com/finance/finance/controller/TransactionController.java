package com.finance.finance.controller;

import com.finance.finance.dto.TransactionRequest;
import com.finance.finance.entity.Transaction;
import com.finance.finance.entity.TransactionType;
import com.finance.finance.entity.User;
import com.finance.finance.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.finance.finance.service.UserService;
@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    // Kullanıcıyı kimlik doğrulama bağlamından alan yardımcı metot
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Not authenticated");
        }
        String username = authentication.getName();
        return userService.findByUsername(username);
    }

    @GetMapping("/reports/summary/type")
    public ResponseEntity<?> getTransactionsSummaryByDateRangeAndType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam TransactionType type) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body("User not found or not authenticated");
            }
            List<Transaction> transactions = transactionService.getUserTransactionsByDateRangeAndType(user, startDate, endDate, type);

            // Kategori bazlı gruplama
            Map<String, BigDecimal> categoryMap = transactions.stream()
                    .collect(Collectors.groupingBy(
                            Transaction::getCategory,
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));

            return ResponseEntity.ok(categoryMap);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody TransactionRequest request) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body("User not found or not authenticated");
            }
            Transaction transaction = transactionService.createTransaction(
                    user,
                    request.getAmount(),
                    request.getType(),
                    request.getDescription(),
                    request.getCategory(),
                    request.getTransactionDate(),
                    request.getCurrencyPair(),
                    request.getPurchaseRate(),
                    request.getSellingRate(),
                    request.getQuantity()
            );
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating transaction: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserTransactions() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body("User not found or not authenticated");
            }
            List<Transaction> transactions = transactionService.getUserTransactions(user);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }


    @GetMapping("/balance")
    public ResponseEntity<?> getUserBalance() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body("User not found or not authenticated");
            }
            BigDecimal balance = transactionService.getUserBalance(user);
            return ResponseEntity.ok(balance);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<?> getUserTransactionsByType(@PathVariable TransactionType type) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body("User not found or not authenticated");
            }
            List<Transaction> transactions = transactionService.getUserTransactionsByType(user, type);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }
    @GetMapping("/reports/date-range-type")
    public ResponseEntity<?> getTransactionsByDateRangeAndType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam TransactionType type) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body("User not found or not authenticated");
            }
            List<Transaction> transactions = transactionService.getUserTransactionsByDateRangeAndType(user, startDate, endDate, type);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }
    @GetMapping("/reports/summary")
    public ResponseEntity<?> getTransactionsSummaryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body("User not found or not authenticated");
            }
            BigDecimal totalIncome = transactionService.getUserTransactionsByDateRangeAndType(user, startDate, endDate, TransactionType.INCOME)
                    .stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpense = transactionService.getUserTransactionsByDateRangeAndType(user, startDate, endDate, TransactionType.EXPENSE)
                    .stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, BigDecimal> summary = new HashMap<>();
            summary.put("totalIncome", totalIncome);
            summary.put("totalExpense", totalExpense);

            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }

    @GetMapping("/reports/category-breakdown")
    public ResponseEntity<?> getCategoryBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam TransactionType type) {

        User user = getAuthenticatedUser();
        List<Transaction> transactions = transactionService.getUserTransactionsByDateRangeAndType(
                user, startDate, endDate, type
        );

        // Kategori bazlı gruplama
        Map<String, BigDecimal> categoryMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        return ResponseEntity.ok(categoryMap);
    }
    // TransactionController.java'ya ekleyin
    @GetMapping("/reports/investment-profit-loss")
    public ResponseEntity<?> getInvestmentProfitLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(401).body("User not found or not authenticated");
            }

            Map<String, Object> profitLossReport = transactionService.getInvestmentProfitLoss(user, startDate, endDate);
            return ResponseEntity.ok(profitLossReport);

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating profit/loss report: " + e.getMessage());
        }
    }
}