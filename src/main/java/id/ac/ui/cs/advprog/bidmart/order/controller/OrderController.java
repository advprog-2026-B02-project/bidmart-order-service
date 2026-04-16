package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.UpdateShippingRequest;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<OrderListResponse> getOrders(
            Authentication authentication,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = getUserIdFromAuthentication(authentication);
        return ResponseEntity.ok(orderService.getOrders(userId, role, status, page, size));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            Authentication authentication,
            @PathVariable UUID orderId
    ) {
        UUID userId = getUserIdFromAuthentication(authentication);
        return ResponseEntity.ok(orderService.getOrderById(orderId, userId));
    }

    @PutMapping("/{orderId}/ship")
    public ResponseEntity<Void> updateShipping(
            @PathVariable UUID orderId,
            @RequestBody UpdateShippingRequest request,
            Authentication authentication
    ) {
        UUID userId = getUserIdFromAuthentication(authentication);
        orderService.updateShipping(orderId, userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{orderId}/confirm-receipt")
    public ResponseEntity<Void> confirmReceipt(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {
        UUID userId = getUserIdFromAuthentication(authentication);
        orderService.confirmReceipt(orderId, userId);
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        return UUID.fromString((String) principal.get("userId"));
    }
}