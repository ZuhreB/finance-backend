// src/main/java/com/finance/finance/controller/SubscriptionController.java
package com.finance.finance.controller;

import com.finance.finance.entity.User;
import com.finance.finance.entity.UserExchangeRateSubscription;
import com.finance.finance.repository.UserExchangeRateSubscriptionRepository;
import com.finance.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private UserExchangeRateSubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> addSubscription(@RequestBody Map<String, String> payload, Principal principal) {
        String currencyPair = payload.get("currencyPair");
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        // Burada geçerli rate'i almanız gerekecek
        // Örnek olarak 0 değeriyle kaydediyoruz, WebSocketHandler'da güncellenecek
        UserExchangeRateSubscription subscription = new UserExchangeRateSubscription(user, currencyPair, BigDecimal.ZERO);
        subscriptionRepository.save(subscription);
        return ResponseEntity.ok(Collections.singletonMap("message", "Subscription added successfully"));
    }

    @DeleteMapping("/{currencyPair}")
    @Transactional
    public ResponseEntity<?> removeSubscription(@PathVariable String currencyPair, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }
        subscriptionRepository.deleteByUserIdAndCurrencyPair(user.getId(), currencyPair);
        return ResponseEntity.ok(Collections.singletonMap("message", "Subscription removed successfully"));
    }
}