package com.finance.finance.controller;

import com.finance.finance.dto.TransactionRequest;
import com.finance.finance.entity.Transaction;
import com.finance.finance.entity.TransactionType;
import com.finance.finance.entity.User;
import com.finance.finance.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    private User getAuthenticatedUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("Not authenticated");
        }
        return user;
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody TransactionRequest request, HttpSession session) {
        try {
            User user = getAuthenticatedUser(session);
            Transaction transaction = transactionService.createTransaction(
                    user,
                    request.getAmount(),
                    request.getType(),
                    request.getDescription(),
                    request.getCategory(),
                    request.getTransactionDate()
            );

            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating transaction: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserTransactions(HttpSession session) {
        try {
            User user = getAuthenticatedUser(session);
            List<Transaction> transactions = transactionService.getUserTransactions(user);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getUserBalance(HttpSession session) {
        try {
            User user = getAuthenticatedUser(session);
            BigDecimal balance = transactionService.getUserBalance(user);
            return ResponseEntity.ok(balance);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<?> getUserTransactionsByType(@PathVariable TransactionType type, HttpSession session) {
        try {
            User user = getAuthenticatedUser(session);
            List<Transaction> transactions = transactionService.getUserTransactionsByType(user, type);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }
}