package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceClient implements NotificationService {

    private final RestTemplate restTemplate;

    @Value("${app.client.notification-url}")
    private String notificationUrl;

    @Override
    public void saveNotification(SaveNotification notification) {
        try {
            restTemplate.postForObject(
                notificationUrl + "/api/notifications",
                notification,
                Void.class
            );
        } catch (Exception e) {
            log.warn("Failed to send notification: {}", e.getMessage());
        }
    }
}
