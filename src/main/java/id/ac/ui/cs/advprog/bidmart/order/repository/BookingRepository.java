package id.ac.ui.cs.advprog.bidmart.order.repository;

import id.ac.ui.cs.advprog.bidmart.order.model.Booking;
import id.ac.ui.cs.advprog.bidmart.order.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByBuyerId(UUID buyerId, Pageable pageable);

    Page<Booking> findBySellerId(UUID sellerId, Pageable pageable);

    Page<Booking> findByBuyerIdAndStatus(UUID buyerId, BookingStatus status, Pageable pageable);
    Page<Booking> findBySellerIdAndStatus(UUID sellerId, BookingStatus status, Pageable pageable);

    boolean existsByAuctionId(UUID auctionId);
}