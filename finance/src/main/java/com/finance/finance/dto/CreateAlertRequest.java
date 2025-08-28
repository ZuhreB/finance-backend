package com.finance.finance.dto; // DTO'lar için uygun bir package

import com.finance.finance.entity.AlertCondition;
import java.math.BigDecimal;

public class CreateAlertRequest {
    private String currencyPair;
    private BigDecimal threshold;
    private AlertCondition condition;

    // Getter ve Setter'lar
    public String getCurrencyPair() {
        return currencyPair;
    }

    public void setCurrencyPair(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public AlertCondition getCondition() {
        return condition;
    }

    public void setCondition(AlertCondition condition) {
        this.condition = condition;
    }
}