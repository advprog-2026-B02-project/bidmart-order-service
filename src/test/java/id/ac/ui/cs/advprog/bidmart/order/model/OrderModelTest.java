package id.ac.ui.cs.advprog.bidmart.order.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderModelTest {

    @Test
    void testOrder() {
        Order order = new Order();
        UUID id = UUID.randomUUID();
        order.setId(id);
        order.setAuctionId(id);
        order.setListingId(id);
        order.setListingTitle("Title");
        order.setListingImageUrl("url");
        order.setBuyerId(id);
        order.setBuyerDisplayName("buyer");
        order.setShippingStreet("street");
        order.setShippingCity("city");
        order.setShippingProvince("prov");
        order.setShippingPostalCode("zip");
        order.setSellerId(id);
        order.setSellerDisplayName("seller");
        order.setTotalAmount(100);
        order.setStatus(OrderStatus.CREATED);
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);

        assertEquals(id, order.getId());
        assertEquals(id, order.getAuctionId());
        assertEquals(id, order.getListingId());
        assertEquals("Title", order.getListingTitle());
        assertEquals("url", order.getListingImageUrl());
        assertEquals(id, order.getBuyerId());
        assertEquals("buyer", order.getBuyerDisplayName());
        assertEquals("street", order.getShippingStreet());
        assertEquals("city", order.getShippingCity());
        assertEquals("prov", order.getShippingProvince());
        assertEquals("zip", order.getShippingPostalCode());
        assertEquals(id, order.getSellerId());
        assertEquals("seller", order.getSellerDisplayName());
        assertEquals(100, order.getTotalAmount());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(now, order.getCreatedAt());

        order.prePersist();
        order.preUpdate();
    }
}