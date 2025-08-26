package com.finance.finance.service;

import com.finance.finance.entity.Transaction;
import com.finance.finance.entity.TransactionType;
import com.finance.finance.entity.User;
import com.finance.finance.handler.ExchangeRateWebSocketHandler;
import com.finance.finance.repository.TransactionRepository;
import com.finance.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExchangeRateWebSocketHandler exchangeRateWebSocketHandler;


    public Transaction createTransaction(User user, BigDecimal amount, TransactionType type,
                                         String description, String category, LocalDate transactionDate,
                                         String currencyPair, BigDecimal purchaseRate,BigDecimal sellingRate, BigDecimal quantity) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setCategory(category);
        transaction.setTransactionDate(transactionDate);

        // Döviz/altın işlemleri için ek alanlar
        if (currencyPair != null) {
            transaction.setCurrencyPair(currencyPair);
        }
        if (purchaseRate != null) {
            transaction.setPurchaseRate(purchaseRate);
        }
        if (sellingRate != null) {
            transaction.setSellingRate(sellingRate);
        }
        if (quantity != null) {
            transaction.setQuantity(quantity);
        }

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
            if (transaction.getType() == TransactionType.INCOME || transaction.getType()==TransactionType.GOLD_SELL
                    || transaction.getType()==TransactionType.CURRENCY_SELL) {
                balance = balance.add(transaction.getAmount());
            } else {
                balance = balance.subtract(transaction.getAmount());
            }
        }

        return balance;
    }
    // bu fonksiyon kullanıldktan sonra yeni balance ı görmek için getbalance() fonksiyonu tekrar çağırılmalı d
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


    public Map<String, Object> getInvestmentProfitLoss(User user, LocalDate startDate, LocalDate endDate) {
        List<Transaction> investmentTransactions = new ArrayList<>();
        investmentTransactions.addAll(transactionRepository.findByUserAndTypeAndTransactionDateBetween(
                user,
                TransactionType.CURRENCY_BUY,
                startDate,
                endDate
        ));
        investmentTransactions.addAll(transactionRepository.findByUserAndTypeAndTransactionDateBetween(
                user,
                TransactionType.CURRENCY_SELL,
                startDate,
                endDate
        ));
        investmentTransactions.addAll(transactionRepository.findByUserAndTypeAndTransactionDateBetween(
                user,
                TransactionType.GOLD_SELL,
                startDate,
                endDate
        ));
        investmentTransactions.addAll(transactionRepository.findByUserAndTypeAndTransactionDateBetween(
                user,
                TransactionType.GOLD_BUY,
                startDate,
                endDate
        ));

        Map<String, Object> result = new HashMap<>();
        Map<String, BigDecimal> profitLossByAsset = new HashMap<>();
        Map<String, BigDecimal> currentHoldings = new HashMap<>();
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        // Mevcut piyasa fiyatlarını al
        Map<String, BigDecimal> currentRates = new HashMap<>();
        currentRates.putAll(exchangeRateWebSocketHandler.getGoldRates());
        currentRates.putAll(exchangeRateWebSocketHandler.getForexRates());

        for (Transaction transaction : investmentTransactions) {
            String assetKey = transaction.getCurrencyPair();

            if (transaction.getType() == TransactionType.CURRENCY_BUY ||
                    transaction.getType() == TransactionType.GOLD_BUY) {
                // ALIŞ işlemi
                BigDecimal currentQuantity = currentHoldings.getOrDefault(assetKey, BigDecimal.ZERO);
                currentHoldings.put(assetKey, currentQuantity.add(transaction.getQuantity()));

            } else if (transaction.getType() == TransactionType.CURRENCY_SELL ||
                    transaction.getType() == TransactionType.GOLD_SELL) {
                // SATIŞ işlemi - kar/zarar hesapla
                BigDecimal purchaseCost = transaction.getPurchaseRate().multiply(transaction.getQuantity());
                BigDecimal sellingRevenue = transaction.getSellingRate().multiply(transaction.getQuantity());
                BigDecimal profitLoss = sellingRevenue.subtract(purchaseCost);

                profitLossByAsset.merge(assetKey, profitLoss, BigDecimal::add);
                totalProfitLoss = totalProfitLoss.add(profitLoss);

                // Satış yapılan miktarı currentHoldings'ten düş
                BigDecimal currentQuantity = currentHoldings.getOrDefault(assetKey, BigDecimal.ZERO);
                currentHoldings.put(assetKey, currentQuantity.subtract(transaction.getQuantity()));
            }
        }

        // Gerçekleşmemiş kar/zarar hesapla (mevcut portföy)
        Map<String, BigDecimal> unrealizedProfitLoss = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : currentHoldings.entrySet()) {
            String asset = entry.getKey();
            BigDecimal quantity = entry.getValue();
            BigDecimal currentPrice = currentRates.getOrDefault(asset, BigDecimal.ZERO);

            // Bu varlığın ortalama alış maliyetini bul
            BigDecimal avgPurchaseRate = calculateAveragePurchaseRate(investmentTransactions, asset);
            if (avgPurchaseRate.compareTo(BigDecimal.ZERO) > 0 && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unrealizedPL = currentPrice.subtract(avgPurchaseRate).multiply(quantity);
                unrealizedProfitLoss.put(asset, unrealizedPL);
                totalProfitLoss = totalProfitLoss.add(unrealizedPL);
            }
        }

        result.put("realizedProfitLossByAsset", profitLossByAsset);
        result.put("unrealizedProfitLossByAsset", unrealizedProfitLoss);
        result.put("currentHoldings", currentHoldings);
        result.put("totalProfitLoss", totalProfitLoss);
        result.put("currentRates", currentRates);

        return result;
    }
    private BigDecimal calculateAveragePurchaseRate(List<Transaction> transactions, String asset) {
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getCurrencyPair().equals(asset) &&
                    (t.getType() == TransactionType.CURRENCY_BUY || t.getType() == TransactionType.GOLD_BUY)) {
                totalCost = totalCost.add(t.getPurchaseRate().multiply(t.getQuantity()));
                totalQuantity = totalQuantity.add(t.getQuantity());
            }
        }

        return totalQuantity.compareTo(BigDecimal.ZERO) > 0 ?
                totalCost.divide(totalQuantity, 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}