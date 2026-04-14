package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.order.dto.NotificationListResponse;
import id.ac.ui.cs.advprog.bidmart.order.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void getNotifications() {
        UUID userId = UUID.randomUUID();
        when(notificationService.getNotifications(userId, false, 0, 20))
            .thenReturn(NotificationListResponse.builder().build());
        
        ResponseEntity<NotificationListResponse> res = controller.getNotifications(userId, false, 0, 20);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void markAsRead() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doNothing().when(notificationService).markAsRead(id, userId);

        ResponseEntity<Void> res = controller.markAsRead(id, userId);
        assertEquals(200, res.getStatusCode().value());
        verify(notificationService).markAsRead(id, userId);
    }

    @Test
    void markAllAsRead() {
        UUID userId = UUID.randomUUID();
        doNothing().when(notificationService).markAllAsRead(userId);

        ResponseEntity<Void> res = controller.markAllAsRead(userId);
        assertEquals(200, res.getStatusCode().value());
        verify(notificationService).markAllAsRead(userId);
    }
}
