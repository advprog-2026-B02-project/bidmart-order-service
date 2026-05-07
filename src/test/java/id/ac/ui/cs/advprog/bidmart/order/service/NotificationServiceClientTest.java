package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.order.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationServiceClientTest {

    private RestTemplate restTemplate;
    private NotificationServiceClient client;

    @BeforeEach
    void setUp() throws Exception {
        restTemplate = mock(RestTemplate.class);
        client = new NotificationServiceClient(restTemplate);

        // set private @Value fields via reflection
        Field urlField = NotificationServiceClient.class.getDeclaredField("notificationUrl");
        urlField.setAccessible(true);
        urlField.set(client, "http://notif.example.com");

        Field tokenField = NotificationServiceClient.class.getDeclaredField("serviceToken");
        tokenField.setAccessible(true);
        tokenField.set(client, "token-123");
    }

    @Test
    void saveNotification_CallsRestTemplate() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(null);

        SaveNotification n = SaveNotification.builder()
                .userId(UUID.randomUUID())
                .type(NotificationType.ORDER_CREATED)
                .title("t")
                .message("m")
                .build();

        client.saveNotification(n);

        verify(restTemplate).postForObject(eq("http://notif.example.com/internal/v1/notifications"), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void saveNotification_WhenRestThrows_DoesNotPropagate() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("boom"));

        SaveNotification n = SaveNotification.builder()
                .userId(UUID.randomUUID())
                .type(NotificationType.ORDER_CREATED)
                .title("t")
                .message("m")
                .build();

        assertDoesNotThrow(() -> client.saveNotification(n));
        verify(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(Void.class));
    }
}
