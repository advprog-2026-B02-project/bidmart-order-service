package id.ac.ui.cs.advprog.bidmart.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import java.time.LocalDateTime;

import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/v1/orders")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrderFromEvent(
            @RequestHeader("X-Service-Token") String serviceToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateOrder request) {
        orderService.createOrderFromEvent(request);
        return ResponseEntity.status(201).body(Map.of(
            "auctionId", request.getAuctionId().toString(),
            "status", "CREATED",
            "createdAt", LocalDateTime.now().toString()
        ));
    }
}