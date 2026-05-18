package id.ac.ui.cs.advprog.bidmart.order.repository;

import id.ac.ui.cs.advprog.bidmart.order.model.OutboxEvent;
import id.ac.ui.cs.advprog.bidmart.order.model.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class OutboxRepositoryTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void findByStatusOrderByCreatedAtAsc_ReturnsPendingEventsInOrder() {
        OutboxEvent first = new OutboxEvent("notification", "user-a", "ORDER_CREATED",
                "order.notification-requests", "user-a", "{\"title\":\"first\"}");
        first.setStatus(OutboxStatus.PENDING);
        first.setAttempts(0);
        first.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        first.setUpdatedAt(first.getCreatedAt());

        OutboxEvent second = new OutboxEvent("notification", "user-b", "ORDER_CREATED",
                "order.notification-requests", "user-b", "{\"title\":\"second\"}");
        second.setStatus(OutboxStatus.PENDING);
        second.setAttempts(0);
        second.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        second.setUpdatedAt(second.getCreatedAt());

        outboxRepository.save(first);
        outboxRepository.save(second);

        var page = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertEquals("{\"title\":\"first\"}", page.getContent().get(0).getPayload());
        assertEquals("{\"title\":\"second\"}", page.getContent().get(1).getPayload());
    }
}