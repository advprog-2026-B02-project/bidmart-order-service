package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.order.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.dto.DisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderSummary;
import id.ac.ui.cs.advprog.bidmart.order.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.dto.UpdateShippingRequest;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

        private final OrderRepository orderRepository;
        private final NotificationService notificationService;
        private final id.ac.ui.cs.advprog.bidmart.order.repository.IdempotencyRepository idempotencyRepository;

        public OrderServiceImpl(OrderRepository orderRepository, NotificationService notificationService,
                                                        id.ac.ui.cs.advprog.bidmart.order.repository.IdempotencyRepository idempotencyRepository) {
                this.orderRepository = orderRepository;
                this.notificationService = notificationService;
                this.idempotencyRepository = idempotencyRepository;
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
        @Transactional(readOnly = true)
        public OrderListResponse getOrdersAdmin(String status, int page, int size) {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

                Page<Order> result;
                if (status != null && !status.isBlank()) {
                        OrderStatus orderStatus = parseStatus(status);
                        result = orderRepository.findByStatus(orderStatus, pageable);
                } else {
                        result = orderRepository.findAll(pageable);
                }

                List<OrderSummary> content = result.getContent().stream().map(this::toSummaryDTO).toList();

                return OrderListResponse.builder()
                                .content(content)
                                .page(result.getNumber())
                                .size(result.getSize())
                                .totalElements(result.getTotalElements())
                                .totalPages(result.getTotalPages())
                                .build();
        }

    @Override
    @Transactional
        public id.ac.ui.cs.advprog.bidmart.order.model.Order createOrderFromEvent(CreateOrder dto, String idempotencyKey) {
                if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                        var existing = idempotencyRepository.findByKey(idempotencyKey);
                        if (existing.isPresent()) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Idempotency-Key sudah pernah digunakan");
                        }
                }

                if (orderRepository.existsByAuctionId(dto.getAuctionId())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Order untuk auctionId ini sudah ada");
                }

                id.ac.ui.cs.advprog.bidmart.order.model.Order order = new id.ac.ui.cs.advprog.bidmart.order.model.Order();
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

        order.setStatus(OrderStatus.CREATED);

        orderRepository.save(order);

        // persist idempotency key mapping after order creation
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            id.ac.ui.cs.advprog.bidmart.order.model.IdempotencyKey key = new id.ac.ui.cs.advprog.bidmart.order.model.IdempotencyKey(
                    idempotencyKey,
                    order.getId(),
                    order.getAuctionId(),
                    java.time.LocalDateTime.now()
            );
            idempotencyRepository.save(key);
        }

        // send notification to buyer
        notificationService.saveNotification(SaveNotification.builder()
                .userId(dto.getBuyerId())
                .type(NotificationType.ORDER_CREATED)
                .title("Pesanan Berhasil Dibuat")
                .message("Pesanan untuk " + dto.getListingTitle() + " telah berhasil dibuat.")
                .data(Map.of(
                        "orderId", order.getId().toString(),
                        "listingTitle", dto.getListingTitle(),
                        "totalAmount", dto.getTotalAmount().toString()
                ))
                .build());

        // send notification to seller
        notificationService.saveNotification(SaveNotification.builder()
                .userId(dto.getSellerId())
                .type(NotificationType.ORDER_CREATED)
                .title("Pesanan Baru Diterima")
                .message("Anda menerima pesanan baru untuk " + dto.getListingTitle() + ".")
                .data(Map.of(
                        "orderId", order.getId().toString(),
                        "listingTitle", dto.getListingTitle(),
                        "buyerDisplayName", dto.getBuyerDisplayName(),
                        "totalAmount", dto.getTotalAmount().toString()
                ))
                .build());

        return order;
    }

    @Override
    @Transactional
    public void updateShipping(UUID orderId, UUID userId, UpdateShippingRequest request) {
        Order order = findOrderOrThrow(orderId);

        if (!order.getSellerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Hanya penjual yang dapat mengupdate informasi pengiriman");
        }

        String requestedStatus = request.getStatus() == null || request.getStatus().isBlank()
                ? "SHIPPED"
                : request.getStatus().trim().toUpperCase();

        if ("PACKAGED".equals(requestedStatus)) {
            markPackaged(order);
            return;
        }

        if (!"SHIPPED".equals(requestedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status pengiriman tidak valid: " + request.getStatus());
        }

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PACKAGED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pesanan harus dalam status CREATED untuk dapat mengupdate pengiriman");
        }
        if (request.getCourier() == null || request.getCourier().isBlank()
                || request.getTrackingNumber() == null || request.getTrackingNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Kurir dan nomor resi wajib diisi");
        }

        // update shipping info
        order.setCourier(request.getCourier().trim());
        order.setTrackingNumber(request.getTrackingNumber().trim());
        order.setShippedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        // send notification
        notificationService.saveNotification(SaveNotification.builder()
                .userId(order.getBuyerId())
                .type(NotificationType.ORDER_SHIPPED)
                .title("Pesanan Telah Dikirim")
                .message("Pesanan " + order.getListingTitle() + " telah dikirim.")
                .data(Map.of(
                        "orderId", order.getId().toString(),
                        "listingTitle", order.getListingTitle(),
                        "courier", request.getCourier() != null ? request.getCourier() : "",
                        "trackingNumber", request.getTrackingNumber() != null ? request.getTrackingNumber() : ""
                ))
                .build());
    }

    private void markPackaged(Order order) {
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hanya pesanan berstatus CREATED yang dapat ditandai dikemas");
        }

        order.setStatus(OrderStatus.PACKAGED);
        orderRepository.save(order);

        notificationService.saveNotification(SaveNotification.builder()
                .userId(order.getBuyerId())
                .type(NotificationType.ORDER_PACKAGED)
                .title("Pesanan Sedang Dikemas")
                .message("Pesanan " + order.getListingTitle() + " sedang dikemas oleh penjual.")
                .data(Map.of(
                        "orderId", order.getId().toString(),
                        "listingTitle", order.getListingTitle()
                ))
                .build());
    }

    @Override
    @Transactional
    public void confirmReceipt(UUID orderId, UUID userId) {
        Order order = findOrderOrThrow(orderId);

        if (!order.getBuyerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Hanya pembeli yang dapat mengkonfirmasi penerimaan pesanan");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pesanan harus dalam status SHIPPED untuk dapat dikonfirmasi penerimaan");
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        // send notification
        notificationService.saveNotification(SaveNotification.builder()
                .userId(order.getSellerId())
                .type(NotificationType.ORDER_COMPLETED)
                .title("Pesanan Telah Diterima Pembeli")
                .message("Pesanan " + order.getListingTitle() + " telah dikonfirmasi diterima oleh pembeli.")
                .data(Map.of(
                        "orderId", order.getId().toString(),
                        "listingTitle", order.getListingTitle(),
                        "buyerDisplayName", order.getBuyerDisplayName()
                ))
                .build());
    }

    @Override
    @Transactional
    public void createDispute(UUID orderId, UUID userId, DisputeRequest request){
        Order order = findOrderOrThrow(orderId);

        if (!order.getBuyerId().equals(userId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Hanya pembeli yang dapat mengajukan sengketa");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Sengketa hanya bisa diajukan pada pesanan berstatus SHIPPED");
        }

        order.setStatus(OrderStatus.DISPUTED);
        order.setDisputeReason(request.getReason());
        order.setDisputeDescription(request.getDescription());
        order.setDisputedAt(LocalDateTime.now());
        order.setEvidenceImages(                           
                request.getEvidenceImages() != null
                        ? String.join(",", request.getEvidenceImages())
                        : null
        );
        orderRepository.save(order);

        notificationService.saveNotification(SaveNotification.builder()
            .userId(order.getSellerId())
            .type(NotificationType.DISPUTE_CREATED)
            .title("Sengketa Diajukan")
            .message("Pembeli mengajukan sengketa untuk pesanan " + order.getListingTitle())
            .data(Map.of("orderId", order.getId().toString()))
            .build());
    }

    @Override
    @Transactional
    public void resolveDispute(UUID orderId, ResolveDisputeRequest request){
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.DISPUTED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Pesanan tidak dalam status sengketa");
        }

        order.setStatus(OrderStatus.RESOLVED);
        order.setDisputeResolution(request.getResolution());
        order.setDisputeNote(request.getNote());
        order.setResolvedAt(LocalDateTime.now());
        orderRepository.save(order);

        notificationService.saveNotification(SaveNotification.builder()
                .userId(order.getBuyerId())
                .type(NotificationType.DISPUTE_RESOLVED)
                .title("Sengketa Diselesaikan")
                .message("Sengketa pesanan " + order.getListingTitle() 
                        + " telah diselesaikan dengan keputusan: " + request.getResolution())
                .data(Map.of(
                        "orderId", order.getId().toString(),
                        "resolution", request.getResolution()
                ))
                .build());

        notificationService.saveNotification(SaveNotification.builder()
                .userId(order.getSellerId())
                .type(NotificationType.DISPUTE_RESOLVED)
                .title("Sengketa Diselesaikan")
                .message("Sengketa pesanan " + order.getListingTitle()
                        + " telah diselesaikan dengan keputusan: " + request.getResolution())
                .data(Map.of(
                        "orderId", order.getId().toString(),
                        "resolution", request.getResolution()
                ))
                .build());
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
                .shipping(buildShippingDTO(order))
                .timeline(buildTimelineDTO(order))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderResponse.ShippingDTO buildShippingDTO(Order order) {
        if (order.getCourier() == null && order.getTrackingNumber() == null && order.getShippedAt() == null) {
            return null;
        }

        return OrderResponse.ShippingDTO.builder()
                .courier(order.getCourier())
                .trackingNumber(order.getTrackingNumber())
                .shippedAt(order.getShippedAt())
                .build();
    }

    private List<OrderResponse.TimelineDTO> buildTimelineDTO(Order order) {
        List<OrderResponse.TimelineDTO> timeline = new ArrayList<>();
        timeline.add(OrderResponse.TimelineDTO.builder()
                .status(OrderStatus.CREATED.name())
                .timestamp(order.getCreatedAt())
                .build());

        if (order.getStatus() == OrderStatus.PACKAGED || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.DISPUTED
                || order.getStatus() == OrderStatus.RESOLVED) {
            timeline.add(OrderResponse.TimelineDTO.builder()
                    .status(OrderStatus.PACKAGED.name())
                    .timestamp(order.getUpdatedAt())
                    .build());
        }
        if (order.getShippedAt() != null) {
            timeline.add(OrderResponse.TimelineDTO.builder()
                    .status(OrderStatus.SHIPPED.name())
                    .timestamp(order.getShippedAt())
                    .build());
        }
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.DISPUTED
                || order.getStatus() == OrderStatus.RESOLVED) {
            timeline.add(OrderResponse.TimelineDTO.builder()
                    .status(order.getStatus().name())
                    .timestamp(order.getUpdatedAt())
                    .build());
        }

        return timeline;
    }
}
