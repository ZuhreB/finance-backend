package com.finance.finance.controller;

import com.finance.finance.entity.User;
import com.finance.finance.entity.UserAlert;
import com.finance.finance.service.AlertService;
import com.finance.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Doğru import!
import org.springframework.web.bind.annotation.*;
import com.finance.finance.dto.CreateAlertRequest; // DTO importu
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserService userService; // User'ı bulmak için

    @PostMapping
    public ResponseEntity<?> createAlert(@RequestBody CreateAlertRequest request, Authentication authentication) {
        // Principal'dan kullanıcı adını al
        String username = authentication.getName();
        // Kullanıcı adından User nesnesini bul
        User user = userService.findByUsername(username);

        UserAlert alert = alertService.createAlert(user, request.getCurrencyPair(), request.getThreshold(), request.getCondition());
        return ResponseEntity.ok(alert);
    }

    @GetMapping
    public ResponseEntity<?> getUserAlerts(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        List<UserAlert> alerts = alertService.getAlertsByUser(user);
        return ResponseEntity.ok(alerts);
    }

    // DELETE endpoint'i, etc.
}