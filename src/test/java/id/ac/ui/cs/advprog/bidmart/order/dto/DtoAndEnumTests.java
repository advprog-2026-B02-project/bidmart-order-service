package id.ac.ui.cs.advprog.bidmart.order.dto;

import id.ac.ui.cs.advprog.bidmart.order.model.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtoAndEnumTests {

    @Test
    void saveNotificationBuilder() {
        UUID id = UUID.randomUUID();
        SaveNotification n = SaveNotification.builder()
                .userId(id)
                .type(NotificationType.ORDER_SHIPPED)
                .title("title")
                .message("msg")
                .data(Map.of("k", "v"))
                .build();

        assertEquals(id, n.getUserId());
        assertEquals(NotificationType.ORDER_SHIPPED, n.getType());
        assertEquals("title", n.getTitle());
        assertEquals("msg", n.getMessage());
        assertEquals("v", n.getData().get("k"));
    }

    @Test
    void disputeAndResolveRequestBuilders() {
        DisputeRequest d = DisputeRequest.builder()
                .reason("r")
                .description("desc")
                .evidenceImages(List.of("1","2"))
                .build();

        assertEquals("r", d.getReason());
        assertEquals("desc", d.getDescription());
        assertEquals(2, d.getEvidenceImages().size());

        ResolveDisputeRequest r = ResolveDisputeRequest.builder()
                .resolution("res")
                .note("n")
                .build();

        assertEquals("res", r.getResolution());
        assertEquals("n", r.getNote());
    }

    @Test
    void notificationTypeValues() {
        NotificationType[] vals = NotificationType.values();
        assertTrue(vals.length >= 1);
        assertEquals("ORDER_CREATED", NotificationType.ORDER_CREATED.name());
    }
}
