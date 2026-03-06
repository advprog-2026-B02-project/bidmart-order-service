package id.ac.ui.cs.advprog.bidmart.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class CreateOrder {

    private UUID auctionId;
    private UUID listingId;
    private String listingTitle;
    private String listingImageUrl;

    private UUID buyerId;
    private String buyerDisplayName;
    private String shippingStreet;
    private String shippingCity;
    private String shippingProvince;
    private String shippingPostalCode;

    private UUID sellerId;
    private String sellerDisplayName;

    private Integer totalAmount;
}