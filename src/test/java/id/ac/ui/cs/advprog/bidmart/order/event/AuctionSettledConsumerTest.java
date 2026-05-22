package id.ac.ui.cs.advprog.bidmart.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.order.client.CatalogClient;
import id.ac.ui.cs.advprog.bidmart.order.client.UserClient;
import id.ac.ui.cs.advprog.bidmart.order.dto.client.InternalUserResponseDTO;
import id.ac.ui.cs.advprog.bidmart.order.dto.client.ListingDetailResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionSettledConsumerTest {

    private OrderService orderService;
    private UserClient userClient;
    private CatalogClient catalogClient;
    private ObjectMapper objectMapper;
    private AuctionSettledConsumer consumer;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        userClient = mock(UserClient.class);
        catalogClient = mock(CatalogClient.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.module.paramnames.ParameterNamesModule());
        objectMapper.findAndRegisterModules();
        consumer = new AuctionSettledConsumer(orderService, objectMapper, userClient, catalogClient);
    }

    @Test
    void consumeAuctionSettled_happyPath_callsOrderService() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        UUID listingId = auctionId;
        UUID sellerId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();

        // build JSON manually to avoid Jackson Java record serialization issues in test environment
        String json = String.format(
            "{\"eventId\":\"%s\",\"auctionId\":\"%s\",\"auctionType\":\"normal\",\"winners\":[{\"userId\":\"%s\",\"amount\":%d}],\"occurredAt\":null}",
            eventId, auctionId, winnerId, 500
        );
        ListingDetailResponse listing = new ListingDetailResponse();
        listing.setId(listingId);
        listing.setSellerId(sellerId);
        listing.setTitle("Item title");
        ListingDetailResponse.ListingImage img = new ListingDetailResponse.ListingImage();
        img.setUrl("http://example.com/img.png");
        listing.setImages(List.of(img));

        InternalUserResponseDTO buyer = new InternalUserResponseDTO();
        buyer.setId(winnerId);
        buyer.setDisplayName("Buyer Name");
        buyer.setShippingStreet("Street");
        buyer.setShippingCity("City");
        buyer.setShippingProvince("Prov");
        buyer.setShippingPostalCode("12345");

        InternalUserResponseDTO seller = new InternalUserResponseDTO();
        seller.setId(sellerId);
        seller.setDisplayName("Seller Name");

        when(catalogClient.getListingById(auctionId)).thenReturn(listing);
        when(userClient.getUserById(winnerId)).thenReturn(buyer);
        when(userClient.getUserById(sellerId)).thenReturn(seller);

        consumer.consumeAuctionSettled(json);

        ArgumentCaptor<CreateOrder> captor = ArgumentCaptor.forClass(CreateOrder.class);
        verify(orderService, times(1)).createOrderFromEvent(captor.capture(), eq(eventId.toString()));

        CreateOrder payload = captor.getValue();
        assertEquals(listingId, payload.getListingId());
        assertEquals(winnerId, payload.getBuyerId());
        assertEquals("Item title", payload.getListingTitle());
        assertEquals(500, payload.getTotalAmount());
        assertEquals("http://example.com/img.png", payload.getListingImageUrl());
    }

    @Test
    void consumeAuctionSettled_invalidJson_doesNotThrow() {
        String badJson = "{ not valid json }";
        // should not throw
        consumer.consumeAuctionSettled(badJson);
        verifyNoInteractions(orderService);
    }
}
