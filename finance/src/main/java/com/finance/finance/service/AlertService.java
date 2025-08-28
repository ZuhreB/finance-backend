package com.finance.finance.service;

import com.finance.finance.entity.AlertCondition;
import com.finance.finance.entity.User;
import com.finance.finance.entity.UserAlert;
import com.finance.finance.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    public UserAlert createAlert(User user, String currencyPair, BigDecimal threshold, AlertCondition condition) {
        UserAlert alert = new UserAlert();
        alert.setUser(user);
        alert.setCurrencyPair(currencyPair);
        alert.setThreshold(threshold);
        alert.setCondition(condition);
        return alertRepository.save(alert);
    }

    public List<UserAlert> getActiveAlertsByCurrencyPair(String currencyPair) {
        return alertRepository.findByCurrencyPairAndIsActiveTrue(currencyPair);
    }

    public List<UserAlert> getAlertsByUser(User user) {
        return alertRepository.findByUser(user);
    }

    // Diğer gerekli metotlar (delete, update, findById vb.)
    public void deleteAlert(Long alertId) {
        alertRepository.deleteById(alertId);
    }
}