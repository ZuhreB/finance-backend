package com.finance.finance.controller;

import com.finance.finance.dto.AuthRequest;
import com.finance.finance.dto.RegisterRequest;
import com.finance.finance.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            Map<String, Object> response = authService.registerUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {

        System.out.println("Login attempt for user: " + authRequest.getUsername());
        try {
            Map<String, Object> response = authService.authenticateUser(
                    authRequest.getUsername(),
                    authRequest.getPassword()
            );

            System.out.println("Login successful, token generated: " + response.get("token"));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}