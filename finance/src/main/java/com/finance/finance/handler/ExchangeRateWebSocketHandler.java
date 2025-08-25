package com.finance.finance.handler;

import org.springframework.http.*;
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

        // İlk bağlanan kullanıcıya hemen veri gönder
        sendAllRates();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket bağlantısı kapandı: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("getRates".equals(message.getPayload())) {
            sendAllRates();
        }
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledRateUpdate() {
        sendAllRates();
    }

    private void sendAllRates() {
        Map<String, BigDecimal> allRates = new HashMap<>();

        // Döviz kurlarını al
        Map<String, BigDecimal> forexRates = getForexRates();
        allRates.putAll(forexRates);

        // Altın fiyatlarını al
        Map<String, BigDecimal> goldRates = getGoldRates();
        allRates.putAll(goldRates);

        // Tüm verileri gönder
        sendRatesToClients(allRates);
    }

    public Map<String, BigDecimal> getForexRates() {
        Map<String, BigDecimal> rates = new HashMap<>();

        try {
            String forexResponse = restTemplate.getForObject(
                    "https://api.frankfurter.app/latest?from=USD", String.class);
            JsonNode forexNode = objectMapper.readTree(forexResponse);

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
                    throw new Exception("API'de gerekli döviz kurları bulunamadı");
                }
            } else {
                throw new Exception("API yanıtı beklenen formatta değil");
            }

        } catch (Exception e) {
            System.err.println("Döviz kurları alınırken hata: " + e.getMessage());
            // Fallback döviz kurları
            rates.put("USD/TRY", new BigDecimal("32.85"));
            rates.put("EUR/TRY", new BigDecimal("35.65"));
            rates.put("GBP/TRY", new BigDecimal("42.10"));
            rates.put("USD/EUR", new BigDecimal("0.93"));
            rates.put("EUR/USD", new BigDecimal("1.075"));
        }

        return rates;
    }

    public Map<String, BigDecimal> getGoldRates() {
        Map<String, BigDecimal> rates = new HashMap<>();

        try {

            String apiKey = "623947b9124568952e445c3f3845f586";
            String goldResponse = restTemplate.getForObject(
                    "https://api.metalpriceapi.com/v1/latest?api_key=" + apiKey + "&base=XAU&currencies=TRY",
                    String.class
            );

            System.out.println("MetalPriceAPI Yanıtı: " + goldResponse);

            JsonNode goldNode = objectMapper.readTree(goldResponse);

            if (goldNode != null && goldNode.has("rates")) {
                JsonNode ratesNode = goldNode.get("rates");
                if (ratesNode.has("TRY")) {
                    // 1 ons altın = XAU, 1 gram = XAU/31.1035
                    BigDecimal ouncePrice = new BigDecimal(ratesNode.get("TRY").asText());
                    BigDecimal gramPrice = ouncePrice.divide(new BigDecimal("31.1035"), 2, BigDecimal.ROUND_HALF_UP);
                    rates.put("GRAM ALTIN", gramPrice);
                    System.out.println("MetalPriceAPI'den alınan altın fiyatı: " + gramPrice);
                } else {
                    throw new Exception("Altın API yanıtında TRY bulunamadı");
                }
            } else {
                throw new Exception("Altın API yanıtı beklenen formatta değil");
            }

        } catch (Exception e) {
            System.err.println("MetalPriceAPI'den altın fiyatı alınırken hata: " + e.getMessage());

            // GoldAPI yedek denemesi
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("x-access-token", "goldapi-5a0u74smeqqn0eb-io");
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> goldResponse = restTemplate.exchange(
                        "https://www.goldapi.io/api/XAU/TRY",
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                System.out.println("GoldAPI Yanıtı: " + goldResponse.getBody());

                JsonNode goldNode = objectMapper.readTree(goldResponse.getBody());
                if (goldNode != null && goldNode.has("price")) {
                    // GoldAPI gram fiyatını direkt veriyor
                    BigDecimal goldPrice = new BigDecimal(goldNode.get("price").asText());
                    rates.put("GRAM ALTIN", goldPrice);
                    System.out.println("GoldAPI'den alınan altın fiyatı: " + goldPrice);
                } else {
                    throw new Exception("Gold API response does not contain 'price' field");
                }
            } catch (Exception ex) {
                System.err.println("Yedek altın API'sinden de veri alınamadı: " + ex.getMessage());

                // Her ikisi de çalışmazsa alternatif API deneyelim
                try {
                    // Ücretsiz alternatif: BigPara
                    String bigParaResponse = restTemplate.getForObject(
                            "https://bigpara.hurriyet.com.tr/api/v1/altin",
                            String.class
                    );

                    JsonNode bigParaNode = objectMapper.readTree(bigParaResponse);
                    if (bigParaNode != null && bigParaNode.has("data")) {
                        JsonNode dataNode = bigParaNode.get("data");
                        JsonNode gramAltinNode = dataNode.get("gram-altin");
                        if (gramAltinNode != null && gramAltinNode.has("alis")) {
                            String alisStr = gramAltinNode.get("alis").asText().replace(",", ".");
                            BigDecimal goldPrice = new BigDecimal(alisStr);
                            rates.put("GRAM ALTIN", goldPrice);
                            System.out.println("BigPara'dan alınan altın fiyatı: " + goldPrice);
                        }
                    }
                } catch (Exception bigParaEx) {
                    System.err.println("BigPara API'sinden de veri alınamadı: " + bigParaEx.getMessage());
                    rates.put("GRAM ALTIN", new BigDecimal("2250.50")); // Fallback değer
                }
            }
        }

        return rates;
    }

    private void sendRatesToClients(Map<String, BigDecimal> rates) {
        try {
            String ratesJson = objectMapper.writeValueAsString(rates);
            System.out.println("Gönderilen kurlar: " + ratesJson);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(ratesJson));
                    } catch (Exception e) {
                        System.err.println("WebSocket'e veri gönderilirken hata: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("JSON dönüştürme hatası: " + e.getMessage());
        }
    }
}