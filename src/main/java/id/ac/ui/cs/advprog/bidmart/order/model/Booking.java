package id.ac.ui.cs.advprog.bidmart.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "auction_id", nullable = false, unique = true)
    private UUID auctionId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "listing_title", nullable = false, length = 255)
    private String listingTitle;

    @Column(name = "listing_image_url", length = 500)
    private String listingImageUrl;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "buyer_display_name", nullable = false, length = 100)
    private String buyerDisplayName;

    @Column(name = "shipping_street", length = 255)
    private String shippingStreet;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_province", length = 100)
    private String shippingProvince;

    @Column(name = "shipping_postal_code", length = 10)
    private String shippingPostalCode;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "seller_display_name", nullable = false, length = 100)
    private String sellerDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = BookingStatus.CREATED;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}