package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;      
import org.springframework.http.MediaType;           
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class NotificationServiceClient implements NotificationService {

    private final RestTemplate restTemplate;

    @Value("${app.client.notification-url}")
    private String notificationUrl;

    @Value("${app.service-token}")
    private String serviceToken;              

    public NotificationServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void saveNotification(SaveNotification notification) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", serviceToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SaveNotification> entity = new HttpEntity<>(notification, headers);
            restTemplate.postForObject(
                notificationUrl + "/internal/v1/notifications",
                entity,
                Void.class
            );
        } catch (Exception e) {
            log.warn("Failed to send notification: {}", e.getMessage());
        }
    }
}
