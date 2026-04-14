package id.ac.ui.cs.advprog.bidmart.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class OrderResponse {

    private UUID id;
    private UUID auctionId;
    private ListingDTO listing;
    private Integer amount;
    private BuyerDTO buyer;
    private SellerDTO seller;
    private String status;
    private ShippingDTO shipping;
    private List<TimelineDTO> timeline;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Builder
    public static class ListingDTO {
        private UUID id;
        private String title;
        private List<String> images;
    }

    @Getter
    @Setter
    @Builder
    public static class SellerDTO {
        private UUID id;
        private String displayName;
    }

    @Getter
    @Setter
    @Builder
    public static class BuyerDTO {
        private UUID id;
        private String displayName;
        private ShippingAddressDTO shippingAddress;
    }

    @Getter
    @Setter
    @Builder
    public static class ShippingAddressDTO {
        private String street;
        private String city;
        private String province;
        private String postalCode;
    }

    @Getter
    @Setter
    @Builder
    public static class ShippingDTO {
        private String courier;
        private String trackingNumber;
        private LocalDateTime shippedAt;
    }

    @Getter
    @Setter
    @Builder
    public static class TimelineDTO {
        private String status;
        private LocalDateTime timestamp;
    }
}