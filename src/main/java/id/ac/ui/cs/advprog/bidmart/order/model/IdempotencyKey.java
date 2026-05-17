package id.ac.ui.cs.advprog.bidmart.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "key", nullable = false, unique = true, length = 255)
    private String key;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "auction_id")
    private UUID auctionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public IdempotencyKey(String key, UUID orderId, UUID auctionId, LocalDateTime createdAt) {
        this.key = key;
        this.orderId = orderId;
        this.auctionId = auctionId;
        this.createdAt = createdAt;
    }
}
