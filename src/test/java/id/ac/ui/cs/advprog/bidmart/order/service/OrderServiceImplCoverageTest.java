package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.dto.DisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.order.dto.UpdateShippingRequest;
import id.ac.ui.cs.advprog.bidmart.order.model.Order;
import id.ac.ui.cs.advprog.bidmart.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.IdempotencyRepository;
import id.ac.ui.cs.advprog.bidmart.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplCoverageTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    private OrderServiceImpl orderService;
    private Order order;
    private UUID orderId;
    private UUID buyerId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, notificationService, idempotencyRepository);
        orderId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        order = baseOrder(OrderStatus.CREATED);
    }

    @Test
    void getOrdersAdmin_NoStatus_ReturnsAllOrders() {
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        OrderListResponse response = orderService.getOrdersAdmin(null, 0, 10);

        assertEquals(1, response.getContent().size());
        assertEquals(orderId, response.getContent().get(0).getId());
        verify(orderRepository).findAll(any(Pageable.class));
    }

    @Test
    void getOrdersAdmin_WithStatus_ReturnsMatchingOrders() {
        when(orderRepository.findByStatus(eq(OrderStatus.CREATED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        OrderListResponse response = orderService.getOrdersAdmin("created", 0, 10);

        assertEquals(1, response.getContent().size());
        verify(orderRepository).findByStatus(eq(OrderStatus.CREATED), any(Pageable.class));
    }

    @Test
    void getOrdersAdmin_BlankStatus_ReturnsAllOrders() {
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        OrderListResponse response = orderService.getOrdersAdmin(" ", 0, 10);

        assertEquals(1, response.getContent().size());
        verify(orderRepository).findAll(any(Pageable.class));
    }

    @Test
    void createOrderFromEvent_BlankIdempotencyKey_DoesNotPersistKey() {
        CreateOrder request = createOrderRequest();
        when(orderRepository.existsByAuctionId(request.getAuctionId())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(orderId);
            return saved;
        });
        doNothing().when(notificationService).saveNotification(any(SaveNotification.class));

        Order result = orderService.createOrderFromEvent(request, "   ");

        assertEquals(OrderStatus.CREATED, result.getStatus());
        verify(idempotencyRepository, never()).findByKey(any());
        verify(idempotencyRepository, never()).save(any());
        verify(notificationService, times(2)).saveNotification(any(SaveNotification.class));
    }

    @Test
    void createOrderFromEvent_NullIdempotencyKey_DoesNotPersistKey() {
        CreateOrder request = createOrderRequest();
        when(orderRepository.existsByAuctionId(request.getAuctionId())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(orderId);
            return saved;
        });

        Order result = orderService.createOrderFromEvent(request, null);

        assertEquals(OrderStatus.CREATED, result.getStatus());
        verify(idempotencyRepository, never()).findByKey(any());
        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void updateShipping_NullStatus_DefaultsToShipped() {
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                .courier(" JNE ")
                .trackingNumber(" TRK123 ")
                .build());

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals("JNE", order.getCourier());
        assertEquals("TRK123", order.getTrackingNumber());
    }

    @Test
    void updateShipping_BlankStatus_DefaultsToShipped() {
        order.setStatus(OrderStatus.PACKAGED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                .status("  ")
                .courier("SiCepat")
                .trackingNumber("FAST")
                .build());

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void updateShipping_PackagedStatus_MarksOrderAsPackaged() {
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                .status(" packaged ")
                .build());

        assertEquals(OrderStatus.PACKAGED, order.getStatus());
        verify(notificationService).saveNotification(any(SaveNotification.class));
    }

    @Test
    void updateShipping_PackagedStatusWhenNotCreated_ThrowsBadRequest() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                        .status("PACKAGED")
                        .build()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateShipping_InvalidRequestedStatus_ThrowsBadRequest() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                        .status("DELIVERING")
                        .build()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateShipping_WhenOrderAlreadyCompleted_ThrowsBadRequest() {
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                        .status("SHIPPED")
                        .courier("JNE")
                        .trackingNumber("TRK123")
                        .build()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateShipping_MissingCourier_ThrowsBadRequest() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                        .status("SHIPPED")
                        .trackingNumber("TRK123")
                        .build()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateShipping_BlankCourier_ThrowsBadRequest() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                        .status("SHIPPED")
                        .courier(" ")
                        .trackingNumber("TRK123")
                        .build()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateShipping_MissingTrackingNumber_ThrowsBadRequest() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                        .status("SHIPPED")
                        .courier("JNE")
                        .build()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateShipping_BlankTrackingNumber_ThrowsBadRequest() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.updateShipping(orderId, sellerId, UpdateShippingRequest.builder()
                        .status("SHIPPED")
                        .courier("JNE")
                        .trackingNumber(" ")
                        .build()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void confirmReceipt_ForbiddenForNonBuyer() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.confirmReceipt(orderId, sellerId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void createDispute_NullEvidenceImages_SavesNullEvidence() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createDispute(orderId, buyerId, DisputeRequest.builder()
                .reason("Not received")
                .description("Still waiting")
                .build());

        assertEquals(OrderStatus.DISPUTED, order.getStatus());
        assertNull(order.getEvidenceImages());
    }

    @Test
    void getOrderById_WithShippingInfo_ReturnsShippingDto() {
        order.setStatus(OrderStatus.SHIPPED);
        order.setCourier("JNE");
        order.setTrackingNumber("TRK123");
        order.setShippedAt(LocalDateTime.now());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);

        assertNotNull(response.getShipping());
        assertEquals("JNE", response.getShipping().getCourier());
    }

    @Test
    void getOrderById_WithOnlyShippedAt_ReturnsShippingDto() {
        LocalDateTime shippedAt = LocalDateTime.now();
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(shippedAt);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);

        assertNotNull(response.getShipping());
        assertNull(response.getShipping().getCourier());
        assertNull(response.getShipping().getTrackingNumber());
        assertEquals(shippedAt, response.getShipping().getShippedAt());
    }

    @Test
    void getOrderById_WithOnlyTrackingNumber_ReturnsShippingDto() {
        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber("TRK123");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);

        assertNotNull(response.getShipping());
        assertNull(response.getShipping().getCourier());
        assertEquals("TRK123", response.getShipping().getTrackingNumber());
        assertNull(response.getShipping().getShippedAt());
    }

    @Test
    void getOrderById_PackagedOrder_ReturnsPackagedTimeline() {
        order.setStatus(OrderStatus.PACKAGED);
        order.setUpdatedAt(LocalDateTime.now());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);

        assertEquals(List.of("CREATED", "PACKAGED"),
                response.getTimeline().stream().map(OrderResponse.TimelineDTO::getStatus).toList());
    }

    @Test
    void getOrderById_CompletedOrder_ReturnsFinalTimeline() {
        order.setStatus(OrderStatus.COMPLETED);
        order.setShippedAt(LocalDateTime.now().minusDays(1));
        order.setUpdatedAt(LocalDateTime.now());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);

        assertEquals(List.of("CREATED", "PACKAGED", "SHIPPED", "COMPLETED"),
                response.getTimeline().stream().map(OrderResponse.TimelineDTO::getStatus).toList());
    }

    @Test
    void getOrderById_DisputedOrder_ReturnsDisputedTimeline() {
        order.setStatus(OrderStatus.DISPUTED);
        order.setUpdatedAt(LocalDateTime.now());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);

        assertEquals(List.of("CREATED", "PACKAGED", "DISPUTED"),
                response.getTimeline().stream().map(OrderResponse.TimelineDTO::getStatus).toList());
    }

    @Test
    void getOrderById_ResolvedOrder_ReturnsResolvedTimeline() {
        order.setStatus(OrderStatus.RESOLVED);
        order.setUpdatedAt(LocalDateTime.now());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);

        assertEquals(List.of("CREATED", "PACKAGED", "RESOLVED"),
                response.getTimeline().stream().map(OrderResponse.TimelineDTO::getStatus).toList());
    }

    private Order baseOrder(OrderStatus status) {
        Order newOrder = new Order();
        newOrder.setId(orderId);
        newOrder.setAuctionId(UUID.randomUUID());
        newOrder.setListingId(UUID.randomUUID());
        newOrder.setListingTitle("Sample Item");
        newOrder.setListingImageUrl("https://example.test/image.png");
        newOrder.setBuyerId(buyerId);
        newOrder.setBuyerDisplayName("Buyer");
        newOrder.setShippingStreet("Street");
        newOrder.setShippingCity("City");
        newOrder.setShippingProvince("Province");
        newOrder.setShippingPostalCode("12345");
        newOrder.setSellerId(sellerId);
        newOrder.setSellerDisplayName("Seller");
        newOrder.setTotalAmount(100);
        newOrder.setStatus(status);
        newOrder.setCreatedAt(LocalDateTime.now().minusDays(2));
        newOrder.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return newOrder;
    }

    private CreateOrder createOrderRequest() {
        return CreateOrder.builder()
                .auctionId(UUID.randomUUID())
                .listingId(UUID.randomUUID())
                .listingTitle("Created Item")
                .listingImageUrl("https://example.test/created.png")
                .buyerId(buyerId)
                .buyerDisplayName("Buyer")
                .shippingStreet("Street")
                .shippingCity("City")
                .shippingProvince("Province")
                .shippingPostalCode("12345")
                .sellerId(sellerId)
                .sellerDisplayName("Seller")
                .totalAmount(250)
                .build();
    }
}
