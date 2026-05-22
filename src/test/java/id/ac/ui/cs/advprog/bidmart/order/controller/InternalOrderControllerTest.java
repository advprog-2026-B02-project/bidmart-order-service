package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InternalOrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private InternalOrderController controller;

    @Test
    void createOrderFromEvent_ReturnsCreatedMap() {
        CreateOrder req = CreateOrder.builder()
                .auctionId(UUID.randomUUID())
                .listingTitle("Title")
                .build();

        org.mockito.Mockito.when(orderService.createOrderFromEvent(req, "idemp")).thenReturn(null);

        ResponseEntity<Map<String, Object>> res = controller.createOrderFromEvent("token", "idemp", req);

        assertEquals(201, res.getStatusCodeValue());
        assertEquals(req.getAuctionId().toString(), res.getBody().get("auctionId"));
        assertEquals("CREATED", res.getBody().get("status"));
        assertNotNull(res.getBody().get("createdAt"));
    }

    @Test
    void createOrderFromEvent_UsesOrderCreatedAtWhenServiceReturnsOrder() {
        CreateOrder req = CreateOrder.builder()
                .auctionId(UUID.randomUUID())
                .listingTitle("Title")
                .build();
        id.ac.ui.cs.advprog.bidmart.order.model.Order order =
                new id.ac.ui.cs.advprog.bidmart.order.model.Order();
        order.setCreatedAt(java.time.LocalDateTime.of(2026, 1, 2, 3, 4));

        org.mockito.Mockito.when(orderService.createOrderFromEvent(req, "idemp")).thenReturn(order);

        ResponseEntity<Map<String, Object>> res = controller.createOrderFromEvent("token", "idemp", req);

        assertEquals("2026-01-02T03:04", res.getBody().get("createdAt"));
    }
}
