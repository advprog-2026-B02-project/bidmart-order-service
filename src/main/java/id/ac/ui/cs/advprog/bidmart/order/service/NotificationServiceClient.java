package id.ac.ui.cs.advprog.bidmart.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.order.model.OutboxEvent;
import id.ac.ui.cs.advprog.bidmart.order.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceClient implements NotificationService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.notification-topic:order.notification-requests}")
    private String notificationTopic;

    public NotificationServiceClient(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveNotification(SaveNotification notification) {
        try {
            String payload = objectMapper.writeValueAsString(notification);
            outboxRepository.save(OutboxEvent.notificationEvent(notificationTopic, payload, notification));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for outbox: {}", e.getMessage(), e);
            throw new IllegalStateException("Gagal menyimpan notifikasi ke outbox", e);
        } catch (RuntimeException e) {
            log.error("Failed to store notification in outbox: {}", e.getMessage(), e);
            throw e;
        }
    }
}
