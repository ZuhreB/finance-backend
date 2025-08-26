package com.finance.finance.controller;

import com.finance.finance.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}) // CORS izinleri
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatWithAI(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mesaj boş olamaz."));
        }

        try {
            String aiResponse = aiService.getAIResponse(userMessage);
            return ResponseEntity.ok(Map.of("response", aiResponse));
        } catch (Exception e) {
            System.err.println("AI ile iletişim kurulamadı: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "AI ile iletişim kurulamadı. Lütfen daha sonra tekrar deneyin."));
        }
    }
}