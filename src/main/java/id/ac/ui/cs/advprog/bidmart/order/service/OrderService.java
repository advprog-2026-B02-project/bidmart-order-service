package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.dto.DisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.UpdateShippingRequest;

import java.util.UUID;

public interface OrderService {

    OrderListResponse getOrders(UUID userId, String role, String status, int page, int size);
    OrderListResponse getOrdersAdmin(String status, int page, int size);
    OrderResponse getOrderById(UUID orderId, UUID userId);
    id.ac.ui.cs.advprog.bidmart.order.model.Order createOrderFromEvent(CreateOrder dto, String idempotencyKey);
    void updateShipping(UUID orderId, UUID userId, UpdateShippingRequest request);
    void confirmReceipt(UUID orderId, UUID userId);
    void createDispute(UUID orderId, UUID userId, DisputeRequest request);
    void resolveDispute(UUID orderId, ResolveDisputeRequest request);
}