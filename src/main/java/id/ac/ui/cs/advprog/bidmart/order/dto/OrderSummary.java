package id.ac.ui.cs.advprog.bidmart.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class OrderSummary {

    private UUID id;
    private UUID auctionId;
    private String listingTitle;
    private Integer amount;
    private UserBasicDTO buyer;
    private UserBasicDTO seller;
    private String status;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Builder
    public static class UserBasicDTO {
        private UUID id;
        private String displayName;
    }
}