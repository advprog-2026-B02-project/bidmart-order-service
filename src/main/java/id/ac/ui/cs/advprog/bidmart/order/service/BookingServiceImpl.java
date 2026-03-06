package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderSummary;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.model.Booking;
import id.ac.ui.cs.advprog.bidmart.order.model.BookingStatus;
import id.ac.ui.cs.advprog.bidmart.order.repository.BookingRepository;
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
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderListResponse getOrders(UUID userId, String role, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Booking> result;

        if (status != null && !status.isBlank()) {
            BookingStatus bookingStatus = parseStatus(status);
            if ("SELLER".equalsIgnoreCase(role)) {
                result = bookingRepository.findBySellerIdAndStatus(userId, bookingStatus, pageable);
            } else {
                result = bookingRepository.findByBuyerIdAndStatus(userId, bookingStatus, pageable);
            }
        } else {
            if ("SELLER".equalsIgnoreCase(role)) {
                result = bookingRepository.findBySellerId(userId, pageable);
            } else {
                result = bookingRepository.findByBuyerId(userId, pageable);
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
        Booking booking = findBookingOrThrow(orderId);

        boolean isBuyer = booking.getBuyerId().equals(userId);
        boolean isSeller = booking.getSellerId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Anda tidak memiliki akses ke pesanan ini");
        }

        return toResponseDTO(booking);
    }

    @Override
    @Transactional
    public void createOrderFromEvent(CreateOrder dto) {
        if (bookingRepository.existsByAuctionId(dto.getAuctionId())) {
            return;
        }

        Booking booking = new Booking();
        booking.setAuctionId(dto.getAuctionId());
        booking.setListingId(dto.getListingId());
        booking.setListingTitle(dto.getListingTitle());
        booking.setListingImageUrl(dto.getListingImageUrl());
        booking.setBuyerId(dto.getBuyerId());
        booking.setBuyerDisplayName(dto.getBuyerDisplayName());
        booking.setShippingStreet(dto.getShippingStreet());
        booking.setShippingCity(dto.getShippingCity());
        booking.setShippingProvince(dto.getShippingProvince());
        booking.setShippingPostalCode(dto.getShippingPostalCode());
        booking.setSellerId(dto.getSellerId());
        booking.setSellerDisplayName(dto.getSellerDisplayName());
        booking.setTotalAmount(dto.getTotalAmount());

        bookingRepository.save(booking);
    }

    private Booking findBookingOrThrow(UUID orderId) {
        return bookingRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pesanan tidak ditemukan"));
    }

    private BookingStatus parseStatus(String status) {
        try {
            return BookingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status tidak valid: " + status);
        }
    }

    private OrderSummary toSummaryDTO(Booking booking) {
        return OrderSummary.builder()
                .id(booking.getId())
                .auctionId(booking.getAuctionId())
                .listingTitle(booking.getListingTitle())
                .amount(booking.getTotalAmount())
                .buyer(OrderSummary.UserBasicDTO.builder()
                        .id(booking.getBuyerId())
                        .displayName(booking.getBuyerDisplayName())
                        .build())
                .seller(OrderSummary.UserBasicDTO.builder()
                        .id(booking.getSellerId())
                        .displayName(booking.getSellerDisplayName())
                        .build())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private OrderResponse toResponseDTO(Booking booking) {
        return OrderResponse.builder()
                .id(booking.getId())
                .auctionId(booking.getAuctionId())
                .listing(OrderResponse.ListingDTO.builder()
                        .id(booking.getListingId())
                        .title(booking.getListingTitle())
                        .images(booking.getListingImageUrl() != null
                                ? List.of(booking.getListingImageUrl())
                                : List.of())
                        .build())
                .amount(booking.getTotalAmount())
                .buyer(OrderResponse.BuyerDTO.builder()
                        .id(booking.getBuyerId())
                        .displayName(booking.getBuyerDisplayName())
                        .shippingAddress(OrderResponse.ShippingAddressDTO.builder()
                                .street(booking.getShippingStreet())
                                .city(booking.getShippingCity())
                                .province(booking.getShippingProvince())
                                .postalCode(booking.getShippingPostalCode())
                                .build())
                        .build())
                .seller(OrderResponse.SellerDTO.builder()
                        .id(booking.getSellerId())
                        .displayName(booking.getSellerDisplayName())
                        .build())
                .status(booking.getStatus().name())
                .shipping(null)
                .timeline(null)
                .createdAt(booking.getCreatedAt())
                .build();
    }
}