package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.config.KafkaConfig;
import id.ac.ui.cs.advprog.bidmart.order.model.OutboxEvent;
import id.ac.ui.cs.advprog.bidmart.order.model.OutboxStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Profile("!test")
public class OutboxDispatcher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.notification-topic:" + KafkaConfig.TOPIC_NOTIFICATION_REQUESTS + "}")
    private String defaultTopic;

    @Value("${app.kafka.outbox.batch-size:50}")
    private int batchSize;

    @Value("${app.kafka.outbox.max-attempts:5}")
    private int maxAttempts;

    public OutboxDispatcher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-delay-ms:5000}")
    @Transactional
    public void dispatchPendingEvents() {
        List<OutboxEvent> events = outboxRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, batchSize))
                .getContent();

        for (OutboxEvent event : events) {
            dispatchSingleEvent(event);
        }
    }

    private void dispatchSingleEvent(OutboxEvent event) {
        String topic = event.getTopic() != null ? event.getTopic() : defaultTopic;
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getMessageKey(), event.getPayload());
            record.headers().add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("aggregateType", safeBytes(event.getAggregateType()));
            record.headers().add("aggregateId", safeBytes(event.getAggregateId()));
            record.headers().add("eventType", safeBytes(event.getEventType()));

            kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);

            event.setStatus(OutboxStatus.SENT);
            event.setDispatchedAt(LocalDateTime.now());
            event.setLastError(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markDispatchFailure(event, topic, e);
        } catch (ExecutionException | TimeoutException e) {
            markDispatchFailure(event, topic, e);
        } catch (RuntimeException e) {
            markDispatchFailure(event, topic, e);
        }

        outboxRepository.save(event);
    }

    private void markDispatchFailure(OutboxEvent event, String topic, Exception e) {
        int nextAttempts = event.getAttempts() + 1;
        event.setAttempts(nextAttempts);
        event.setLastError(trimMessage(e.getMessage()));
        if (nextAttempts >= maxAttempts) {
            event.setStatus(OutboxStatus.FAILED);
        } else {
            event.setStatus(OutboxStatus.PENDING);
        }
        log.warn("Failed to dispatch outbox event {} on topic {} (attempt {}/{}): {}",
                event.getId(), topic, nextAttempts, maxAttempts, e.getMessage());
    }

    private byte[] safeBytes(String value) {
        return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    }

    private String trimMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
