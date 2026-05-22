package id.ac.ui.cs.advprog.bidmart.order.model;

import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OutboxEventModelTest {

    @Test
    void notificationEvent_WithNullUserAndType_UsesFallbackValues() {
        SaveNotification notification = SaveNotification.builder()
                .title("title")
                .message("message")
                .build();

        OutboxEvent event = OutboxEvent.notificationEvent("topic", "{}", notification);

        assertEquals("notification", event.getAggregateType());
        assertNull(event.getAggregateId());
        assertEquals("NOTIFICATION", event.getEventType());
        assertNull(event.getMessageKey());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttempts());
    }

    @Test
    void notificationEvent_WithUserAndType_UsesNotificationValues() {
        UUID userId = UUID.randomUUID();
        SaveNotification notification = SaveNotification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("title")
                .message("message")
                .build();

        OutboxEvent event = OutboxEvent.notificationEvent("topic", "{}", notification);

        assertEquals(userId.toString(), event.getAggregateId());
        assertEquals(NotificationType.ORDER_CREATED.name(), event.getEventType());
        assertEquals(userId.toString(), event.getMessageKey());
    }

    @Test
    void prePersist_FillsMissingDefaults() {
        OutboxEvent event = new OutboxEvent("aggregate", "id", "TYPE", "topic", "key", "{}");

        event.prePersist();

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttempts());
        assertNotNull(event.getCreatedAt());
        assertNotNull(event.getUpdatedAt());
    }

    @Test
    void preUpdate_RefreshesUpdatedAt() {
        OutboxEvent event = new OutboxEvent("aggregate", "id", "TYPE", "topic", "key", "{}");
        event.setUpdatedAt(LocalDateTime.now().minusDays(1));

        event.preUpdate();

        assertNotNull(event.getUpdatedAt());
    }
}
