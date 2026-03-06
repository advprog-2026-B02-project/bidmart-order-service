package id.ac.ui.cs.advprog.bidmart.order.repository;

import id.ac.ui.cs.advprog.bidmart.order.model.Order;
import id.ac.ui.cs.advprog.bidmart.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByBuyerId(UUID buyerId, Pageable pageable);

    Page<Order> findBySellerId(UUID sellerId, Pageable pageable);

    Page<Order> findByBuyerIdAndStatus(UUID buyerId, OrderStatus status, Pageable pageable);
    Page<Order> findBySellerIdAndStatus(UUID sellerId, OrderStatus status, Pageable pageable);

    boolean existsByAuctionId(UUID auctionId);
}