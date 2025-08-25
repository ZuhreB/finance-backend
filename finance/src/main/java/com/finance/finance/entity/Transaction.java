package com.finance.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal; // Para birimi için BigDecimal kullanmak en iyisidir
import java.time.LocalDate;   // İşlem tarihi için

@Entity
@Table(name = "transactions") // Veritabanındaki tablo adı
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // İşlemi yapan kullanıcı ile ilişki
    @ManyToOne(fetch = FetchType.LAZY) // LAZY yükleme performansı artırır
    @JoinColumn(name = "user_id", nullable = false) // user_id sütunu zorunlu
    private User user;

    @Column(nullable = false)
    private BigDecimal amount; // İşlem miktarı (para)

    @Enumerated(EnumType.STRING) // Enum'ı String olarak kaydet
    @Column(nullable = false)
    private TransactionType type; // GELİR veya GİDER

    @Column(nullable = false)
    private String description; // İşlemin açıklaması (örn: "Maaş", "Market Alışverişi")

    // Kategori için de bir Entity oluşturabilirsiniz, şimdilik String tutalım
    @Column(nullable = false)
    private String category; // İşlem kategorisi (örn: "Gıda", "Ulaşım", "Maaş")

    @Column(nullable = false)
    private LocalDate transactionDate; // İşlemin yapıldığı tarih

    @Column(nullable = false, updatable = false) // Oluşturulma tarihi, sonradan değiştirilemez
    private LocalDate createdAt = LocalDate.now();

    @Column(nullable = true)
    private String currencyPair; // Örn: "USD/TRY", "GRAM ALTIN"

    @Column(nullable = true, precision = 19, scale = 6)
    private BigDecimal purchaseRate; // Alış kuru (CURRENCY_BUY, GOLD_BUY için)

    @Column(nullable = true, precision = 19, scale = 6)
    private BigDecimal sellingRate; // Satış kuru (CURRENCY_SELL, GOLD_SELL için)

    @Column(nullable = true, precision = 19, scale = 6)
    private BigDecimal quantity;
    // --- Getter ve Setter Metotları ---
    // Lombok bağımlılığını eklediyseniz @Data veya @Getter/@Setter ile bunları otomatik oluşturabilirsiniz.
    // Şimdilik manuel ekliyorum, ancak Lombok kullanmak kodunuzu çok daha temiz yapar.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
    public String getCurrencyPair() { return currencyPair; }
    public void setCurrencyPair(String currencyPair) { this.currencyPair = currencyPair; }

    public BigDecimal getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(BigDecimal purchaseRate) { this.purchaseRate = purchaseRate; }

    public BigDecimal getSellingRate() { return sellingRate; }
    public void setSellingRate(BigDecimal sellingRate) { this.sellingRate = sellingRate; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public boolean isInvestmentTransaction() {
        return this.type == TransactionType.CURRENCY_BUY ||
                this.type == TransactionType.CURRENCY_SELL ||
                this.type == TransactionType.GOLD_BUY ||
                this.type == TransactionType.GOLD_SELL;
    }
}