package id.ac.ui.cs.advprog.bidmart.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.order.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.order.model.OutboxEvent;
import id.ac.ui.cs.advprog.bidmart.order.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceClientTest {

    private OutboxRepository outboxRepository;
    private ObjectMapper objectMapper;
    private NotificationServiceClient client;

    @BeforeEach
    void setUp() throws Exception {
        outboxRepository = mock(OutboxRepository.class);
        objectMapper = new ObjectMapper();
        client = new NotificationServiceClient(outboxRepository, objectMapper);

        Field topicField = NotificationServiceClient.class.getDeclaredField("notificationTopic");
        topicField.setAccessible(true);
        topicField.set(client, "order.notification-requests");
    }

    @Test
    void saveNotification_WritesOutboxEvent() {
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID userId = UUID.randomUUID();
        SaveNotification notification = SaveNotification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("t")
                .message("m")
                .build();

        client.saveNotification(notification);

        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void saveNotification_PersistsExpectedOutboxPayload() {
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID userId = UUID.randomUUID();
        SaveNotification notification = SaveNotification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("t")
                .message("m")
                .build();

        client.saveNotification(notification);

        verify(outboxRepository).save(org.mockito.ArgumentMatchers.argThat(event -> {
            assertEquals("order.notification-requests", event.getTopic());
            assertEquals("notification", event.getAggregateType());
            assertEquals(userId.toString(), event.getAggregateId());
            assertEquals(NotificationType.ORDER_CREATED.name(), event.getEventType());
            assertEquals(userId.toString(), event.getMessageKey());
            assertEquals("PENDING", event.getStatus().name());
            assertNotNull(event.getPayload());
            assertTrue(event.getPayload().contains("\"title\":\"t\""));
            return true;
        }));
    }

    @Test
    void saveNotification_WhenRepositoryFails_PropagatesRuntimeException() {
        doThrow(new RuntimeException("boom")).when(outboxRepository).save(any(OutboxEvent.class));

        SaveNotification notification = SaveNotification.builder()
                .userId(UUID.randomUUID())
                .type(NotificationType.ORDER_CREATED)
                .title("t")
                .message("m")
                .build();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> client.saveNotification(notification));
        assertEquals("boom", exception.getMessage());
    }
}
