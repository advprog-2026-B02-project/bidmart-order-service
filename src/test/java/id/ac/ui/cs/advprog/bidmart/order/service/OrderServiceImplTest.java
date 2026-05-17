package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.UpdateShippingRequest;
import id.ac.ui.cs.advprog.bidmart.order.model.Order;
import id.ac.ui.cs.advprog.bidmart.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private id.ac.ui.cs.advprog.bidmart.order.repository.IdempotencyRepository idempotencyRepository;

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
        order.setAuctionId(UUID.randomUUID());
        order.setListingId(UUID.randomUUID());
        order.setListingTitle("Sample Item");
        order.setListingImageUrl("http://image.url");
        order.setBuyerId(buyerId);
        order.setBuyerDisplayName("Buyer");
        order.setSellerId(sellerId);
        order.setSellerDisplayName("Seller");
        order.setTotalAmount(100);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(java.time.LocalDateTime.now());
    }

    @Test
    void getOrders_BySeller_NoStatus() {
        Page<Order> page = new PageImpl<>(Collections.singletonList(order));
        when(orderRepository.findBySellerId(eq(sellerId), any(Pageable.class))).thenReturn(page);

        OrderListResponse response = orderService.getOrders(sellerId, "SELLER", null, 0, 10);
        assertEquals(1, response.getContent().size());
        assertEquals("Sample Item", response.getContent().get(0).getListingTitle());
    }

    @Test
    void getOrders_ByBuyer_WithStatus() {
        Page<Order> page = new PageImpl<>(Collections.singletonList(order));
        when(orderRepository.findByBuyerIdAndStatus(eq(buyerId), eq(OrderStatus.CREATED), any(Pageable.class))).thenReturn(page);

        OrderListResponse response = orderService.getOrders(buyerId, "BUYER", "CREATED", 0, 10);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getOrders_BySeller_WithStatus() {
        Page<Order> page = new PageImpl<>(Collections.singletonList(order));
        when(orderRepository.findBySellerIdAndStatus(eq(sellerId), eq(OrderStatus.CREATED), any(Pageable.class))).thenReturn(page);

        OrderListResponse response = orderService.getOrders(sellerId, "SELLER", "CREATED", 0, 10);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getOrders_ByBuyer_NoStatus() {
        Page<Order> page = new PageImpl<>(Collections.singletonList(order));
        when(orderRepository.findByBuyerId(eq(buyerId), any(Pageable.class))).thenReturn(page);

        OrderListResponse response = orderService.getOrders(buyerId, "BUYER", "", 0, 10);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getOrders_InvalidStatus_ThrowsException() {
        assertThrows(ResponseStatusException.class, () -> 
                orderService.getOrders(buyerId, "BUYER", "INVALID_STATUS", 0, 10));
    }

    @Test
    void getOrderById_AsBuyer_Success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);
        assertNotNull(response);
        assertEquals(orderId, response.getId());
    }

    @Test
    void getOrderById_AsSeller_Success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, sellerId);
        assertNotNull(response);
        assertEquals(orderId, response.getId());
    }

    @Test
    void getOrderById_Forbidden() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> 
                orderService.getOrderById(orderId, UUID.randomUUID()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getOrderById_NotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> 
                orderService.getOrderById(orderId, buyerId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createOrderFromEvent_NotExists_Saves() {
        CreateOrder req = CreateOrder.builder()
                .auctionId(UUID.randomUUID())
                .listingId(UUID.randomUUID())
                .listingTitle("Test Item")
                .buyerId(buyerId)
                .buyerDisplayName("Buyer")
                .sellerId(sellerId)
                .sellerDisplayName("Seller")
                .totalAmount(100)
                .build();
        when(orderRepository.existsByAuctionId(req.getAuctionId())).thenReturn(false);
        when(idempotencyRepository.findByKey(anyString())).thenReturn(Optional.empty());
        
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID()); // Simulate ID generation
            return order;
        });
        doNothing().when(notificationService).saveNotification(any(SaveNotification.class));

        orderService.createOrderFromEvent(req, "idemp");
        verify(orderRepository).save(any(Order.class));
        verify(notificationService, times(2)).saveNotification(any(SaveNotification.class));
    }

    @Test
    void createOrderFromEvent_Exists_Skips() {
        CreateOrder req = CreateOrder.builder().auctionId(UUID.randomUUID()).build();
        when(orderRepository.existsByAuctionId(req.getAuctionId())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> orderService.createOrderFromEvent(req, null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationService, never()).saveNotification(any(SaveNotification.class));
    }

        @Test
        void createOrderFromEvent_DuplicateIdempotencyKey_ThrowsConflict() {
        CreateOrder req = CreateOrder.builder().auctionId(UUID.randomUUID()).build();
        when(idempotencyRepository.findByKey("idemp")).thenReturn(
            java.util.Optional.of(new id.ac.ui.cs.advprog.bidmart.order.model.IdempotencyKey(
                "idemp",
                UUID.randomUUID(),
                req.getAuctionId(),
                java.time.LocalDateTime.now())));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> orderService.createOrderFromEvent(req, "idemp"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationService, never()).saveNotification(any(SaveNotification.class));
        }

    @Test
    void getOrderById_NoImageUrl() {
        order.setListingImageUrl(null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);
        assertNotNull(response);
        assertTrue(response.getListing().getImages().isEmpty());
    }

    @Test
    void updateShipping_AsSeller_Success() {
        order.setStatus(OrderStatus.PACKAGED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(notificationService).saveNotification(any(SaveNotification.class));

        UpdateShippingRequest request = UpdateShippingRequest.builder()
                .status("SHIPPED")
                .courier("JNE")
                .trackingNumber("TRK123")
                .build();

        orderService.updateShipping(orderId, sellerId, request);

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals("JNE", order.getCourier());
        assertEquals("TRK123", order.getTrackingNumber());
        assertNotNull(order.getShippedAt());
        verify(orderRepository).save(order);
        verify(notificationService).saveNotification(any(SaveNotification.class));
    }

    @Test
    void updateShipping_ForbiddenForNonSeller() {
        order.setStatus(OrderStatus.PACKAGED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        UpdateShippingRequest request = UpdateShippingRequest.builder()
                .status("SHIPPED")
                .courier("JNE")
                .trackingNumber("TRK123")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.updateShipping(orderId, buyerId, request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void confirmReceipt_AsBuyer_Success() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(notificationService).saveNotification(any(SaveNotification.class));

        orderService.confirmReceipt(orderId, buyerId);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        verify(orderRepository).save(order);
        verify(notificationService).saveNotification(any(SaveNotification.class));
    }

    @Test
    void confirmReceipt_BadRequestWhenNotShipped() {
        order.setStatus(OrderStatus.PACKAGED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.confirmReceipt(orderId, buyerId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
