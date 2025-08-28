package com.finance.finance.config;

import com.finance.finance.handler.ExchangeRateWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {


    // Spring tarafından yönetilen ExchangeRateWebSocketHandler bean'ini enjekte et.
    @Autowired
    private ExchangeRateWebSocketHandler exchangeRateWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ExchangeRateWebSocketHandler(), "/ws/exchange-rates")
                .setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .setAllowedOriginPatterns("*"); // Tüm origin'lere izin ver
    }

}