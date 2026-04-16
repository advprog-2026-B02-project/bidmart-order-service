package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.dto.UpdateShippingRequest;

import java.util.UUID;

public interface OrderService {

    OrderListResponse getOrders(UUID userId, String role, String status, int page, int size);
    OrderResponse getOrderById(UUID orderId, UUID userId);
    void createOrderFromEvent(CreateOrder dto);
    void updateShipping(UUID orderId, UUID userId, UpdateShippingRequest request);
    void confirmReceipt(UUID orderId, UUID userId);

}