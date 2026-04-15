package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
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
        doNothing().when(notificationService).saveNotification(any());

        orderService.createOrderFromEvent(req);
        verify(orderRepository).save(any(Order.class));
        verify(notificationService, times(2)).saveNotification(any());
    }

    @Test
    void createOrderFromEvent_Exists_Skips() {
        CreateOrder req = CreateOrder.builder().auctionId(UUID.randomUUID()).build();
        when(orderRepository.existsByAuctionId(req.getAuctionId())).thenReturn(true);

        orderService.createOrderFromEvent(req);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_NoImageUrl() {
        order.setListingImageUrl(null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, buyerId);
        assertNotNull(response);
        assertTrue(response.getListing().getImages().isEmpty());
    }
}
