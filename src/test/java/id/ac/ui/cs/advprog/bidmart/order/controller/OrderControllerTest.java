package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void getOrders() {
        UUID userId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(Map.of("userId", userId.toString()));
        OrderListResponse listResponse = OrderListResponse.builder().build();

        when(orderService.getOrders(userId, "BUYER", "CREATED", 0, 20))
                .thenReturn(listResponse);

        ResponseEntity<OrderListResponse> res = orderController.getOrders(
                authentication, "BUYER", "CREATED", 0, 20
        );

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        verify(orderService).getOrders(userId, "BUYER", "CREATED", 0, 20);
    }

    @Test
    void getOrderById() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(Map.of("userId", userId.toString()));
        OrderResponse orderResponse = OrderResponse.builder().id(orderId).build();

        when(orderService.getOrderById(orderId, userId)).thenReturn(orderResponse);

        ResponseEntity<OrderResponse> res = orderController.getOrderById(authentication, orderId);

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        verify(orderService).getOrderById(orderId, userId);
    }
}
