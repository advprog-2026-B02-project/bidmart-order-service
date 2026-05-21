package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.order.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @PutMapping("/{orderId}/dispute/resolve")
    public ResponseEntity<Void> resolveDispute(
            @PathVariable UUID orderId,
            @RequestBody ResolveDisputeRequest request) {
        orderService.resolveDispute(orderId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getOrdersAdmin(status, page, size));
    }
}
