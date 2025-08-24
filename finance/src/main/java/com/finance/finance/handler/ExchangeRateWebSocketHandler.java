package com.finance.finance.handler;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ExchangeRateWebSocketHandler extends TextWebSocketHandler {

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("Yeni WebSocket bağlantısı: " + session.getId());
        System.out.println("Remote address: " + session.getRemoteAddress());
        System.out.println("Headers: " + session.getHandshakeHeaders());

        // İlk bağlanan kullanıcıya hemen veri gönder
        sendExchangeRates();
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket bağlantısı kapandı: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("getRates".equals(message.getPayload())) {
            sendExchangeRates();
        }
    }

    @Scheduled(fixedRate = 30000)
    public void scheduledRateUpdate() {
        sendExchangeRates();
    }

    private void sendExchangeRates() {
        try {
            Map<String, BigDecimal> rates = new HashMap<>();

            // ExchangeRateWebSocketHandler.java - döviz kuru kısmını güncelleyin
            try {
                // Alternatif döviz kuru API'si - daha güvenilir bir API kullanalım
                String forexResponse = restTemplate.getForObject(
                        "https://api.frankfurter.app/latest?from=USD", String.class);
                JsonNode forexNode = objectMapper.readTree(forexResponse);

                // API yanıtını kontrol et
                if (forexNode != null && forexNode.has("rates")) {
                    JsonNode ratesNode = forexNode.get("rates");

                    if (ratesNode.has("TRY") && ratesNode.has("EUR") && ratesNode.has("GBP")) {
                        BigDecimal usdTryRate = new BigDecimal(ratesNode.get("TRY").asText());
                        BigDecimal eurRate = new BigDecimal(ratesNode.get("EUR").asText());
                        BigDecimal gbpRate = new BigDecimal(ratesNode.get("GBP").asText());

                        rates.put("USD/TRY", usdTryRate);
                        rates.put("EUR/TRY", usdTryRate.divide(eurRate, 4, BigDecimal.ROUND_HALF_UP));
                        rates.put("GBP/TRY", usdTryRate.divide(gbpRate, 4, BigDecimal.ROUND_HALF_UP));
                        rates.put("USD/EUR", eurRate);
                        rates.put("EUR/USD", BigDecimal.ONE.divide(eurRate, 4, BigDecimal.ROUND_HALF_UP));
                    } else {
                        // Gerekli döviz kurları yoksa fallback kullan
                        throw new Exception("API'de gerekli döviz kurları bulunamadı");
                    }
                } else {
                    // API yanıtı beklenen formatta değilse fallback kullan
                    throw new Exception("API yanıtı beklenen formatta değil");
                }

            } catch (Exception e) {
                System.err.println("Döviz kurları alınırken hata: " + e.getMessage());
                // Fallback döviz kurları - GERÇEK VERİLERLE GÜNCELLEYELİM
                rates.put("USD/TRY", new BigDecimal("32.85"));
                rates.put("EUR/TRY", new BigDecimal("35.65"));
                rates.put("GBP/TRY", new BigDecimal("42.10"));
                rates.put("USD/EUR", new BigDecimal("0.93"));
                rates.put("EUR/USD", new BigDecimal("1.075"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}