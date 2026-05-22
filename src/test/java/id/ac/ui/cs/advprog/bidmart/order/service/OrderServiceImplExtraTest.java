package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.DisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.model.Order;
import id.ac.ui.cs.advprog.bidmart.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplExtraTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private UUID orderId;
    private UUID buyerId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();

        order = new Order();
        order.setId(orderId);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setListingTitle("Item");
        order.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createDispute_Success() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        DisputeRequest req = DisputeRequest.builder()
                .reason("Reason")
                .description("Desc")
                .evidenceImages(List.of("a.png","b.png"))
                .build();

        orderService.createDispute(orderId, buyerId, req);

        assertEquals(OrderStatus.DISPUTED, order.getStatus());
        assertEquals("Reason", order.getDisputeReason());
        assertEquals("Desc", order.getDisputeDescription());
        assertNotNull(order.getDisputedAt());
        assertEquals("a.png,b.png", order.getEvidenceImages());
        verify(orderRepository).save(order);
        verify(notificationService).saveNotification(any());
    }

    @Test
    void createDispute_ForbiddenForNonBuyer() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        DisputeRequest req = DisputeRequest.builder().reason("r").build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createDispute(orderId, sellerId, req));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void createDispute_BadRequestWhenNotShipped() {
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        DisputeRequest req = DisputeRequest.builder().reason("r").build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createDispute(orderId, buyerId, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void resolveDispute_Success() {
        order.setStatus(OrderStatus.DISPUTED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveDisputeRequest req = ResolveDisputeRequest.builder()
                .resolution("REFUND")
                .note("note")
                .build();

        orderService.resolveDispute(orderId, req);

        assertEquals(OrderStatus.RESOLVED, order.getStatus());
        assertEquals("REFUND", order.getDisputeResolution());
        assertEquals("note", order.getDisputeNote());
        assertNotNull(order.getResolvedAt());
        verify(orderRepository).save(order);
        verify(notificationService, times(2)).saveNotification(any());
    }

    @Test
    void resolveDispute_BadRequestWhenNotDisputed() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResolveDisputeRequest req = ResolveDisputeRequest.builder().resolution("r").build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.resolveDispute(orderId, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
