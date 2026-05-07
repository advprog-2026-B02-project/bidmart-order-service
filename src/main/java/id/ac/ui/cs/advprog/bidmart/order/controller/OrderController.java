package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.order.dto.DisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.UpdateShippingRequest;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<OrderListResponse> getOrders(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getOrders(userId, role, status, page, size));
    }
    

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId, userId));
    }

    @PutMapping("/{orderId}/ship")
    public ResponseEntity<Void> updateShipping(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId,
            @RequestBody UpdateShippingRequest request) {
        orderService.updateShipping(orderId, userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{orderId}/receive")
    public ResponseEntity<Void> confirmReceipt(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId) {
        orderService.confirmReceipt(orderId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/dispute")
    public ResponseEntity<Void> createDispute(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody DisputeRequest request) {
        orderService.createDispute(orderId, userId, request);
        return ResponseEntity.ok().build();
    }
}