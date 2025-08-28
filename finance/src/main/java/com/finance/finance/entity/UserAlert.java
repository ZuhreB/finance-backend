package com.finance.finance.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "user_alerts")
public class UserAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)// kullannıcı bilgisini hep alsın
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String currencyPair; // Örn: "USD/TRY"

    @Column(nullable = false)
    private BigDecimal threshold; // Kullanıcının belirlediği eşik değeri

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertCondition condition; // GREATER_THAN, LESS_THAN

    private boolean isActive = true;

    public User getUser() {
        return user;
    }
    public void setUser(User user){
        this.user = user;
    }
    public void setCurrencyPair( String currencyPair){
        this.currencyPair = currencyPair;
    }
    public void setThreshold(BigDecimal threshold){
        this.threshold = threshold;
    }
    public void setCondition(AlertCondition condition){
        this.condition = condition;
    }
    public void setActive(boolean active){
        isActive = active;
    }
    public Long getId() {
        return id;
    }

    public String getCurrencyPair() {
        return currencyPair;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }
    public AlertCondition getCondition() {
        return condition;
    }
    public boolean isActive() {
        return isActive;
    }
    public void setId(Long id) {
        this.id = id;
    }


}

