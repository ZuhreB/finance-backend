package com.finance.finance.dto;

import com.finance.finance.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private String category;
    private LocalDate transactionDate;

    // Getter ve Setter'lar
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
}