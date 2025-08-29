package com.finance.finance.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finance.entity.UserExchangeRateSubscription;
import com.finance.finance.repository.UserExchangeRateSubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ExchangeRateWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private UserExchangeRateSubscriptionRepository subscriptionRepository;

    private CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    // Düzeltme 1: Son bilinen kurları saklamak için Map nesnesini tanımla
    private final Map<String, BigDecimal> lastKnownRates = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("Yeni WebSocket bağlantısı: " + session.getId());

        // İlk bağlanan kullanıcıya hemen güncel veriyi gönder
        sendAllRates();
        checkAndNotifyExchangeRateChanges();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket bağlantısı kapandı: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("getRates".equals(message.getPayload())) {
            sendAllRates();
        }
    }

    // Düzeltme 2: İki ayrı @Scheduled metodu yerine tek bir metot kullanmak daha verimli olacaktır.
    // FixedRate değeri 1800000000ms (20.8 gün) çok uzun, test için daha küçük bir değer kullanın.
    @Transactional
    @Scheduled(fixedRate = 1800000000)
    public void checkAndNotifyExchangeRateChanges() {
        System.out.println("Checking for new rates");
        Map<String, BigDecimal> allRates = new HashMap<>();
        Map<String, BigDecimal> forexRates = getForexRates();
        allRates.putAll(forexRates);

        Map<String, BigDecimal> goldRates = getGoldRates();
        allRates.putAll(goldRates);

        if (allRates == null || allRates.isEmpty()) {
            System.err.println("Harici API'den kur bilgisi alınamadı.");
            return;
        }

        allRates.forEach((currencyPair, newRate) -> {
            List<UserExchangeRateSubscription> subscriptions = subscriptionRepository.findAllByCurrencyPair(currencyPair);

            for (UserExchangeRateSubscription subscription : subscriptions) {
                BigDecimal lastRate = subscription.getRate();

                if (lastRate == null || !lastRate.equals(newRate)) {
                    System.out.println("Değişim tespit edildi - Kullanıcı: " + subscription.getUser().getEmail() +
                            ", Kur: " + currencyPair + " Eski: " + lastRate + ", Yeni: " + newRate);

                    // Bildirim metni ve kullanıcı adı düzgün bir şekilde tanımlandı.
                    String username = subscription.getUser().getUsername();
                    String message = "Dikkat! " + currencyPair + " kuru şu anda " + newRate + " değerinde.";

                    sendNotificationToUser(username, message);
                    System.out.println(".................................................................................");
                    // Aboneliğin rate'ini güncelle
                    subscription.setRate(newRate);
                    subscriptionRepository.save(subscription);
                }
            }
        });
    }

    public void sendNotificationToUser(String username, String message) {
        try {
            TextMessage notificationMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    // Oturum özelliklerinden ("attributes" map) kullanıcı adını alıyoruz
                    String sessionUsername = (String) session.getAttributes().get("username");

                    if (sessionUsername != null && sessionUsername.equals(username)) {
                        System.out.println("Kullanıcıya bildirim gönderiliyor: " + username+"mesaj "+ notificationMessage);
                        synchronized (session) {
                            session.sendMessage(notificationMessage);
                        }

                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Kullanıcıya bildirim gönderilirken hata oluştu: " + e.getMessage());
        }
    }

    // Bu metot `Principal` kullandığı için şu an için çalışmayacaktır.
    private void broadcastUpdate(String currencyPair, BigDecimal newRate) {
        List<String> userEmailsToNotify = subscriptionRepository.findByCurrencyPair(currencyPair)
                .stream()
                .map(subscription -> subscription.getUser().getEmail())
                .toList();

        sessions.forEach(session -> {
            try {
                if (session.getPrincipal() != null) {
                    String sessionUserEmail = session.getPrincipal().getName();
                    if (userEmailsToNotify.contains(sessionUserEmail)) {
                        String notificationMessage = objectMapper.writeValueAsString(
                                Collections.singletonMap(currencyPair, newRate)
                        );
                        session.sendMessage(new TextMessage(notificationMessage));
                    }
                }
            } catch (IOException e) {
                System.err.println("Mesaj gönderilirken hata oluştu: " + e.getMessage());
            }
        });
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
            String apiKey = "cecdcace37141eba25339706aeeb61de";
            String goldResponse = restTemplate.getForObject(
                    "https://api.metalpriceapi.com/v1/latest?api_key=" + apiKey + "&base=XAU&currencies=TRY",
                    String.class
            );
            System.out.println("MetalPriceAPI Yanıtı: " + goldResponse);
            JsonNode goldNode = objectMapper.readTree(goldResponse);

            if (goldNode != null && goldNode.has("rates")) {
                JsonNode ratesNode = goldNode.get("rates");
                if (ratesNode.has("TRY")) {
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
                    BigDecimal goldPrice = new BigDecimal(goldNode.get("price").asText());
                    rates.put("GRAM ALTIN", goldPrice);
                    System.out.println("GoldAPI'den alınan altın fiyatı: " + goldPrice);
                } else {
                    throw new Exception("Gold API response does not contain 'price' field");
                }
            } catch (Exception ex) {
                System.err.println("Yedek altın API'sinden de veri alınamadı: " + ex.getMessage());
                try {
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
                    rates.put("GRAM ALTIN", new BigDecimal("2250.50"));
                }
            }
        }
        return rates;
    }

    private void sendAllRates() {
        Map<String, BigDecimal> allRates = new HashMap<>();
        Map<String, BigDecimal> forexRates = getForexRates();
        allRates.putAll(forexRates);

        Map<String, BigDecimal> goldRates = getGoldRates();
        allRates.putAll(goldRates);

        sendRatesToClients(allRates);
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