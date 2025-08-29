package com.finance.finance.repository;

import com.finance.finance.entity.UserExchangeRateSubscription;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserExchangeRateSubscriptionRepository extends JpaRepository<UserExchangeRateSubscription,Long> {
    List<UserExchangeRateSubscription> findByCurrencyPair(String currencyPair);
    Optional<UserExchangeRateSubscription> findByUserIdAndCurrencyPair(Long userId, String currencyPair);
    void deleteByUserIdAndCurrencyPair(Long userId, String currencyPair);
    List<UserExchangeRateSubscription> findAllByCurrencyPair(String currencyPair);
    @Modifying
    @Transactional
    @Query("UPDATE UserExchangeRateSubscription u SET u.rate = :rate WHERE u.currencyPair = :currencyPair")
    void updateAllRatesForCurrencyPair(@Param("currencyPair") String currencyPair, @Param("rate") BigDecimal rate);
}
