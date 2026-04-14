package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderSummary;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.model.Order;
import id.ac.ui.cs.advprog.bidmart.order.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderListResponse getOrders(UUID userId, String role, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Order> result;

        if (status != null && !status.isBlank()) {
            OrderStatus orderStatus = parseStatus(status);
            if ("SELLER".equalsIgnoreCase(role)) {
                result = orderRepository.findBySellerIdAndStatus(userId, orderStatus, pageable);
            } else {
                result = orderRepository.findByBuyerIdAndStatus(userId, orderStatus, pageable);
            }
        } else {
            if ("SELLER".equalsIgnoreCase(role)) {
                result = orderRepository.findBySellerId(userId, pageable);
            } else {
                result = orderRepository.findByBuyerId(userId, pageable);
            }
        }

        List<OrderSummary> content = result.getContent()
                .stream()
                .map(this::toSummaryDTO)
                .toList();

        return OrderListResponse.builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, UUID userId) {
        Order order = findOrderOrThrow(orderId);

        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Anda tidak memiliki akses ke pesanan ini");
        }

        return toResponseDTO(order);
    }

    @Override
    @Transactional
    public void createOrderFromEvent(CreateOrder dto) {
        if (orderRepository.existsByAuctionId(dto.getAuctionId())) {
            return;
        }

        Order order = new Order();
        order.setAuctionId(dto.getAuctionId());
        order.setListingId(dto.getListingId());
        order.setListingTitle(dto.getListingTitle());
        order.setListingImageUrl(dto.getListingImageUrl());
        order.setBuyerId(dto.getBuyerId());
        order.setBuyerDisplayName(dto.getBuyerDisplayName());
        order.setShippingStreet(dto.getShippingStreet());
        order.setShippingCity(dto.getShippingCity());
        order.setShippingProvince(dto.getShippingProvince());
        order.setShippingPostalCode(dto.getShippingPostalCode());
        order.setSellerId(dto.getSellerId());
        order.setSellerDisplayName(dto.getSellerDisplayName());
        order.setTotalAmount(dto.getTotalAmount());

        orderRepository.save(order);
    }

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pesanan tidak ditemukan"));
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status tidak valid: " + status);
        }
    }

    private OrderSummary toSummaryDTO(Order order) {
        return OrderSummary.builder()
                .id(order.getId())
                .auctionId(order.getAuctionId())
                .listingTitle(order.getListingTitle())
                .amount(order.getTotalAmount())
                .buyer(OrderSummary.UserBasicDTO.builder()
                        .id(order.getBuyerId())
                        .displayName(order.getBuyerDisplayName())
                        .build())
                .seller(OrderSummary.UserBasicDTO.builder()
                        .id(order.getSellerId())
                        .displayName(order.getSellerDisplayName())
                        .build())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderResponse toResponseDTO(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .auctionId(order.getAuctionId())
                .listing(OrderResponse.ListingDTO.builder()
                        .id(order.getListingId())
                        .title(order.getListingTitle())
                        .images(order.getListingImageUrl() != null
                                ? List.of(order.getListingImageUrl())
                                : List.of())
                        .build())
                .amount(order.getTotalAmount())
                .buyer(OrderResponse.BuyerDTO.builder()
                        .id(order.getBuyerId())
                        .displayName(order.getBuyerDisplayName())
                        .shippingAddress(OrderResponse.ShippingAddressDTO.builder()
                                .street(order.getShippingStreet())
                                .city(order.getShippingCity())
                                .province(order.getShippingProvince())
                                .postalCode(order.getShippingPostalCode())
                                .build())
                        .build())
                .seller(OrderResponse.SellerDTO.builder()
                        .id(order.getSellerId())
                        .displayName(order.getSellerDisplayName())
                        .build())
                .status(order.getStatus().name())
                .shipping(null)
                .timeline(null)
                .createdAt(order.getCreatedAt())
                .build();
    }
}