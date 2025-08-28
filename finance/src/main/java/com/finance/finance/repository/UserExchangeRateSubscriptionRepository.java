package com.finance.finance.repository;

import com.finance.finance.entity.UserExchangeRateSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserExchangeRateSubscriptionRepository extends JpaRepository<UserExchangeRateSubscription,Long> {
    List<UserExchangeRateSubscription> findByCurrencyPair(String currencyPair);
    Optional<UserExchangeRateSubscription> findByUserIdAndCurrencyPair(Long userId, String currencyPair);
    void deleteByUserIdAndCurrencyPair(Long userId, String currencyPair);
}
