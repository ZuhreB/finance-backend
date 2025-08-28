package com.finance.finance.repository;

import com.finance.finance.entity.User;
import com.finance.finance.entity.UserAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<UserAlert, Long> {
   public List<UserAlert> findByCurrencyPairAndIsActiveTrue(String currencyPair);
   public List<UserAlert> findByUser(User user);
}
