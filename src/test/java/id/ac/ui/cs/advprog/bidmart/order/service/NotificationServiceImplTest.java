package id.ac.ui.cs.advprog.bidmart.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.order.dto.NotificationListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.order.model.Notification;
import id.ac.ui.cs.advprog.bidmart.order.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.order.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;
    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setIsRead(false);
        notification.setData("{\"key\":\"value\"}");
    }

    @Test
    void getNotifications_All() throws JsonProcessingException {
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        when(notificationRepository.countByUserIdAndIsRead(userId, false)).thenReturn(1L);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("key", "value"));

        NotificationListResponse res = notificationService.getNotifications(userId, null, 0, 10);
        assertEquals(1, res.getContent().size());
        assertEquals("Title", res.getContent().get(0).getTitle());
        assertEquals(1L, res.getUnreadCount());
    }

    @Test
    void getNotifications_IsRead() throws JsonProcessingException {
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByUserIdAndIsRead(eq(userId), eq(true), any(Pageable.class))).thenReturn(page);

        NotificationListResponse res = notificationService.getNotifications(userId, true, 0, 10);
        assertEquals(1, res.getContent().size());
    }

    @Test
    void getNotifications_JsonError() throws JsonProcessingException {
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenThrow(JsonProcessingException.class);

        NotificationListResponse res = notificationService.getNotifications(userId, null, 0, 10);
        assertNull(res.getContent().get(0).getData());
    }

    @Test
    void markAsRead_Success() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        notificationService.markAsRead(notificationId, userId);
        assertTrue(notification.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_AlreadyRead() {
        notification.setIsRead(true);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        notificationService.markAsRead(notificationId, userId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_NotFound() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> notificationService.markAsRead(notificationId, userId));
    }

    @Test
    void markAsRead_Forbidden() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        assertThrows(ResponseStatusException.class, () -> notificationService.markAsRead(notificationId, UUID.randomUUID()));
    }

    @Test
    void markAllAsRead_Success() {
        notificationService.markAllAsRead(userId);
        verify(notificationRepository).markAllAsReadByUserId(userId);
    }

    @Test
    void saveNotification_Success() throws JsonProcessingException {
        SaveNotification req = SaveNotification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .data(Map.of("key", "val"))
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("str");

        notificationService.saveNotification(req);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void saveNotification_JsonError() throws JsonProcessingException {
        SaveNotification req = SaveNotification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .data(Map.of("key", "val"))
                .build();

        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

        notificationService.saveNotification(req);
        verify(notificationRepository).save(any(Notification.class));
    }
}
