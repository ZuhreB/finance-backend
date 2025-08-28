// src/main/java/com/finance/finance/entity/UserExchangeRateSubscription.java
package com.finance.finance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_exchange_rate_subscriptions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "currencyPair"})
})
public class UserExchangeRateSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String currencyPair; // Örnek: "USD/TRY", "EUR/USD", "GRAM ALTIN"

    // Constructor'lar, Getter ve Setter'lar
    public UserExchangeRateSubscription() {}

    public UserExchangeRateSubscription(User user, String currencyPair) {
        this.user = user;
        this.currencyPair = currencyPair;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCurrencyPair() { return currencyPair; }
    public void setCurrencyPair(String currencyPair) { this.currencyPair = currencyPair; }
}