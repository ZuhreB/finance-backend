package com.finance.finance.service;


import com.finance.finance.entity.AlertCondition;
import com.finance.finance.entity.User;
import com.finance.finance.entity.UserAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService; // Kullanıcı adından User objesi bulmak için

    public void sendAlertNotification(String username, UserAlert alert, BigDecimal currentRate) {
        // 1. Kullanıcı adından User nesnesini bul (Principal kullanmadan)
        User user = userService.findByUsername(username);
        if (user == null) {
            return; // Kullanıcı bulunamazsa işlemi sonlandır
        }

        // 2. Bildirim mesajını hazırla
        String message = createNotificationMessage(alert, currentRate);

        // 3. WebSocket üzerinden kullanıcıya özel bir queue'ya mesajı gönder
        // Destination: /user/{username}/queue/alerts
        String destination = "/user/" + user.getUsername() + "/queue/alerts";
        messagingTemplate.convertAndSend(destination, message);

        System.out.println("Bildirim gönderildi: Kullanıcı=" + user.getUsername() + ", Mesaj=" + message);
    }

    private String createNotificationMessage(UserAlert alert, BigDecimal currentRate) {
        String conditionSymbol = alert.getCondition() == AlertCondition.GREATER_THAN ? "üstüne çıktı" : "altına düştü";
        return String.format("⏰ Alarm! %s kuru, belirlediğiniz %s TL eşiğinin %s. 🟢 Anlık Kur: %s TL",
                alert.getCurrencyPair(),
                alert.getThreshold().toPlainString(),
                conditionSymbol,
                currentRate.toPlainString());
    }
}
