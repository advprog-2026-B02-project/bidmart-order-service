package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.model.OutboxEvent;
import id.ac.ui.cs.advprog.bidmart.order.model.OutboxStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxDispatcherTest {

    private OutboxRepository outboxRepository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() throws Exception {
        outboxRepository = mock(OutboxRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        dispatcher = new OutboxDispatcher(outboxRepository, kafkaTemplate);

        Field topicField = OutboxDispatcher.class.getDeclaredField("defaultTopic");
        topicField.setAccessible(true);
        topicField.set(dispatcher, "order.notification-requests");

        Field batchField = OutboxDispatcher.class.getDeclaredField("batchSize");
        batchField.setAccessible(true);
        batchField.set(dispatcher, 10);

        Field attemptsField = OutboxDispatcher.class.getDeclaredField("maxAttempts");
        attemptsField.setAccessible(true);
        attemptsField.set(dispatcher, 3);
    }

    @Test
    void dispatchPendingEvents_MarksEventAsSent() {
        OutboxEvent event = new OutboxEvent("notification", "user-a", "ORDER_CREATED",
                "order.notification-requests", "user-a", "{\"title\":\"first\"}");
        event.setId(UUID.randomUUID());
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        event.setUpdatedAt(event.getCreatedAt());

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));
        CompletableFuture<SendResult<String, String>> sendFuture = new CompletableFuture<>();
        sendFuture.complete(null);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(sendFuture);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatchPendingEvents();

        verify(outboxRepository).save(any(OutboxEvent.class));
        assertEquals(OutboxStatus.SENT, event.getStatus());
        assertNotNull(event.getDispatchedAt());
    }

    @Test
    void dispatchPendingEvents_WhenKafkaFails_MarksAttemptsAndKeepsPending() {
        OutboxEvent event = new OutboxEvent("notification", "user-a", "ORDER_CREATED",
                "order.notification-requests", "user-a", "{\"title\":\"first\"}");
        event.setId(UUID.randomUUID());
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        event.setUpdatedAt(event.getCreatedAt());

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new RuntimeException("broker down"));
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatchPendingEvents();

        verify(outboxRepository).save(any(OutboxEvent.class));
        assertEquals(1, event.getAttempts());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
    }

    @Test
    void dispatchPendingEvents_WhenNoEvents_DoesNothing() {
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        dispatcher.dispatchPendingEvents();

        verify(outboxRepository).findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class));
    }

    @Test
    void dispatchPendingEvents_WhenTopicIsNull_UsesDefaultTopicAndHandlesNullHeaders() {
        OutboxEvent event = new OutboxEvent(null, null, null,
                null, "user-a", "{\"title\":\"first\"}");
        event.setId(UUID.randomUUID());
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        event.setUpdatedAt(event.getCreatedAt());

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));
        CompletableFuture<SendResult<String, String>> sendFuture = new CompletableFuture<>();
        sendFuture.complete(null);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(sendFuture);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatchPendingEvents();

        verify(kafkaTemplate).send(argThat((ProducerRecord<String, String> record) ->
                "order.notification-requests".equals(record.topic())));
        assertEquals(OutboxStatus.SENT, event.getStatus());
    }

    @Test
    void dispatchPendingEvents_WhenMaxAttemptsReached_MarksFailed() {
        OutboxEvent event = new OutboxEvent("notification", "user-a", "ORDER_CREATED",
                "order.notification-requests", "user-a", "{\"title\":\"first\"}");
        event.setId(UUID.randomUUID());
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(2);
        event.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        event.setUpdatedAt(event.getCreatedAt());

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new RuntimeException("broker down"));
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatchPendingEvents();

        assertEquals(3, event.getAttempts());
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals("broker down", event.getLastError());
    }

    @Test
    void dispatchPendingEvents_WhenFailureMessageIsNull_StoresNullLastError() {
        OutboxEvent event = new OutboxEvent("notification", "user-a", "ORDER_CREATED",
                "order.notification-requests", "user-a", "{\"title\":\"first\"}");
        event.setId(UUID.randomUUID());
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        event.setUpdatedAt(event.getCreatedAt());

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new RuntimeException((String) null));
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatchPendingEvents();

        assertEquals(1, event.getAttempts());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(null, event.getLastError());
    }

    @Test
    void dispatchPendingEvents_WhenFailureMessageIsTooLong_TrimsLastError() {
        OutboxEvent event = new OutboxEvent("notification", "user-a", "ORDER_CREATED",
                "order.notification-requests", "user-a", "{\"title\":\"first\"}");
        event.setId(UUID.randomUUID());
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        event.setUpdatedAt(event.getCreatedAt());
        String longMessage = "x".repeat(1001);

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new RuntimeException(longMessage));
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatchPendingEvents();

        assertEquals(1000, event.getLastError().length());
    }
}
