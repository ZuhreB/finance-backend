package com.finance.finance.config;

import com.finance.finance.handler.ExchangeRateWebSocketHandler;
import com.finance.finance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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
    private JwtUtil jwtUtil;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(exchangeRateWebSocketHandler, "/ws/exchange-rates")
                .setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {

                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request,
                                                   ServerHttpResponse response,
                                                   WebSocketHandler wsHandler,
                                                   Map<String, Object> attributes) throws Exception {
                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                            String token = servletRequest.getServletRequest().getParameter("token");

                            if (token != null && jwtUtil.validateToken(token)) {
                                String username = jwtUtil.extractUsername(token);
                                attributes.put("username", username);
                                return true;
                            }
                        }
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return false;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request,
                                               ServerHttpResponse response,
                                               WebSocketHandler wsHandler,
                                               Exception exception) {
                        // Handshake tamamlandıktan sonra yapılacak işlemler
                    }
                });
    }
}