package com.finance.finance.config;

import com.finance.finance.handler.ExchangeRateWebSocketHandler;
import com.finance.finance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ExchangeRateWebSocketHandler exchangeRateWebSocketHandler;

    @Autowired
    private JwtUtil jwtUtil; // JwtUtil'ı enjekte edin

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(exchangeRateWebSocketHandler, "/ws/exchange-rates")
                .setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

                            // Token'ı query parametresinden al
                            String token = servletRequest.getServletRequest().getParameter("token");

                            if (token != null && !token.isEmpty()) {
                                try {
                                    // Token'ı doğrula ve username'i al
                                    String username = jwtUtil.extractUsername(token);
                                    if (jwtUtil.validateToken(token)) {
                                        attributes.put("username", username);
                                        attributes.put("token", token);
                                        System.out.println("WebSocket authentication successful for user: " + username);
                                        return true;
                                    }
                                } catch (Exception e) {
                                    System.out.println("WebSocket token validation failed: " + e.getMessage());
                                    return false;
                                }
                            }
                        }
                        System.out.println("WebSocket connection rejected: No valid token");
                        return false;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                    }
                });
    }
}