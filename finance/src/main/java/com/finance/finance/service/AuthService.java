package com.finance.finance.service;

import com.finance.finance.entity.User;
import com.finance.finance.repository.UserRepository;
import com.finance.finance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public Map<String, Object> registerUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(username);

        Map<String, Object> response = new HashMap<>();
        response.put("user", savedUser);
        response.put("token", token);

        return response;
    }

    public Map<String, Object> authenticateUser(String username, String password) {
        System.out.println("Authenticating user: " + username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("User not found: " + username);
                    return new RuntimeException("User not found");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            System.out.println("Invalid password for user: " + username);
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(username);
        System.out.println("Generated token for user: " + username);
        System.out.println("Token: " + token);

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("token", token);

        return response;
    }
}