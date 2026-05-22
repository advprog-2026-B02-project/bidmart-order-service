package id.ac.ui.cs.advprog.bidmart.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuctionSettledEventConsumerTest {

    private ObjectMapper objectMapper;
    private OrderService orderService;
    private RestTemplate restTemplate;
    private AuctionSettledEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orderService = mock(OrderService.class);
        restTemplate = mock(RestTemplate.class);
        consumer = new AuctionSettledEventConsumer(objectMapper, orderService, restTemplate);

        ReflectionTestUtils.setField(consumer, "biddingUrl", "http://bidding");
        ReflectionTestUtils.setField(consumer, "catalogUrl", "http://catalog");
        ReflectionTestUtils.setField(consumer, "userUrl", "http://user");
        ReflectionTestUtils.setField(consumer, "serviceToken", "service-token");
    }

    @Test
    void handleAuctionSettled_WithoutListingId_SkipsLegacyEvent() {
        String payload = "{\"auctionId\":\"" + UUID.randomUUID()
                + "\",\"winners\":[{\"userId\":\"" + UUID.randomUUID() + "\",\"amount\":100}]}";

        consumer.handleAuctionSettled(payload);

        verifyNoInteractions(orderService);
    }

    @Test
    void handleAuctionSettled_HappyPathWithSellerIdAndThumbnail_CreatesOrder() {
        UUID eventId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"eventId":"%s","auctionId":"%s","listingId":"%s","sellerId":"%s",
                "winners":[{"userId":"%s","amount":100.60}]}
                """.formatted(eventId, auctionId, listingId, sellerId, buyerId);

        when(restTemplate.getForObject("http://catalog/listings/" + listingId, Map.class))
                .thenReturn(Map.of(
                        "sellerId", sellerId.toString(),
                        "title", "Listing title",
                        "images", List.of(Map.of("thumbnailUrl", "thumb.png", "url", "full.png"))
                ));
        mockUser(buyerId, "Buyer");
        mockUser(sellerId, "Seller");

        consumer.handleAuctionSettled(payload);

        ArgumentCaptor<CreateOrder> captor = ArgumentCaptor.forClass(CreateOrder.class);
        verify(orderService).createOrderFromEvent(captor.capture(), eq(eventId.toString()));
        CreateOrder order = captor.getValue();
        assertEquals(auctionId, order.getAuctionId());
        assertEquals(listingId, order.getListingId());
        assertEquals(sellerId, order.getSellerId());
        assertEquals(buyerId, order.getBuyerId());
        assertEquals("Listing title", order.getListingTitle());
        assertEquals("thumb.png", order.getListingImageUrl());
        assertEquals(101, order.getTotalAmount());
    }

    @Test
    void handleAuctionSettled_WithoutSellerId_UsesListingSellerAndUrlImage() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","winners":[{"userId":"%s","amount":100}]}
                """.formatted(auctionId, listingId, buyerId);

        when(restTemplate.getForObject("http://catalog/listings/" + listingId, Map.class))
                .thenReturn(Map.of(
                        "sellerId", sellerId.toString(),
                        "title", "Listing title",
                        "images", List.of(Map.of("thumbnailUrl", " ", "url", "full.png"))
                ));
        mockUser(buyerId, "Buyer");
        mockUser(sellerId, "Seller");

        consumer.handleAuctionSettled(payload);

        ArgumentCaptor<CreateOrder> captor = ArgumentCaptor.forClass(CreateOrder.class);
        verify(orderService).createOrderFromEvent(captor.capture(), eq("auction-settled-" + auctionId));
        assertEquals(sellerId, captor.getValue().getSellerId());
        assertEquals("full.png", captor.getValue().getListingImageUrl());
    }

    @Test
    void handleAuctionSettled_UserFetchFails_UsesFallbackDisplayName() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","sellerId":"%s",
                "winners":[{"userId":"%s","amount":100}]}
                """.formatted(auctionId, listingId, sellerId, buyerId);

        when(restTemplate.getForObject("http://catalog/listings/" + listingId, Map.class))
                .thenReturn(Map.of("sellerId", sellerId.toString(), "title", " ", "images", List.of()));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenThrow(new RestClientException("down"));

        consumer.handleAuctionSettled(payload);

        ArgumentCaptor<CreateOrder> captor = ArgumentCaptor.forClass(CreateOrder.class);
        verify(orderService).createOrderFromEvent(captor.capture(), eq("auction-settled-" + auctionId));
        assertEquals("Listing " + listingId, captor.getValue().getListingTitle());
        assertNull(captor.getValue().getListingImageUrl());
        assertEquals("User " + buyerId.toString().substring(0, 8), captor.getValue().getBuyerDisplayName());
        assertEquals("User " + sellerId.toString().substring(0, 8), captor.getValue().getSellerDisplayName());
    }

    @Test
    void handleAuctionSettled_FirstImageIsNotMap_UsesNullImage() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","sellerId":"%s",
                "winners":[{"userId":"%s","amount":100}]}
                """.formatted(auctionId, listingId, sellerId, buyerId);

        when(restTemplate.getForObject("http://catalog/listings/" + listingId, Map.class))
                .thenReturn(Map.of("sellerId", sellerId.toString(), "title", "Title", "images", List.of("not-a-map")));
        mockUser(buyerId, "Buyer");
        mockUser(sellerId, "Seller");

        consumer.handleAuctionSettled(payload);

        ArgumentCaptor<CreateOrder> captor = ArgumentCaptor.forClass(CreateOrder.class);
        verify(orderService).createOrderFromEvent(captor.capture(), eq("auction-settled-" + auctionId));
        assertNull(captor.getValue().getListingImageUrl());
    }

    @Test
    void handleAuctionSettled_BlankUrlImage_UsesNullImage() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","sellerId":"%s",
                "winners":[{"userId":"%s","amount":100}]}
                """.formatted(auctionId, listingId, sellerId, buyerId);

        when(restTemplate.getForObject("http://catalog/listings/" + listingId, Map.class))
                .thenReturn(Map.of(
                        "sellerId", sellerId.toString(),
                        "title", "Title",
                        "images", List.of(Map.of("thumbnailUrl", " ", "url", " "))
                ));
        mockUser(buyerId, "Buyer");
        mockUser(sellerId, "Seller");

        consumer.handleAuctionSettled(payload);

        ArgumentCaptor<CreateOrder> captor = ArgumentCaptor.forClass(CreateOrder.class);
        verify(orderService).createOrderFromEvent(captor.capture(), eq("auction-settled-" + auctionId));
        assertNull(captor.getValue().getListingImageUrl());
    }

    @Test
    void handleAuctionSettled_MissingAuctionId_IsWrapped() {
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"listingId":"%s","winners":[{"userId":"%s","amount":100}]}
                """.formatted(listingId, buyerId);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> consumer.handleAuctionSettled(payload));

        assertNotNull(exception.getCause());
    }

    @Test
    void handleAuctionSettled_NullWinnerUserId_IsWrapped() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","winners":[{"userId":null,"amount":100}]}
                """.formatted(auctionId, listingId);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> consumer.handleAuctionSettled(payload));

        assertNotNull(exception.getCause());
    }

    @Test
    void handleAuctionSettled_ConflictFromOrderService_IsIgnored() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","sellerId":"%s",
                "winners":[{"userId":"%s","amount":100}]}
                """.formatted(auctionId, listingId, sellerId, buyerId);

        when(restTemplate.getForObject("http://catalog/listings/" + listingId, Map.class))
                .thenReturn(Map.of("sellerId", sellerId.toString(), "title", "Title"));
        mockUser(buyerId, "Buyer");
        mockUser(sellerId, "Seller");
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "duplicate"))
                .when(orderService).createOrderFromEvent(any(CreateOrder.class), anyString());

        consumer.handleAuctionSettled(payload);

        verify(orderService).createOrderFromEvent(any(CreateOrder.class), anyString());
    }

    @Test
    void handleAuctionSettled_NonConflictResponseStatus_IsRethrown() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","sellerId":"%s",
                "winners":[{"userId":"%s","amount":100}]}
                """.formatted(auctionId, listingId, sellerId, buyerId);

        when(restTemplate.getForObject("http://catalog/listings/" + listingId, Map.class))
                .thenReturn(Map.of("sellerId", sellerId.toString(), "title", "Title"));
        mockUser(buyerId, "Buyer");
        mockUser(sellerId, "Seller");
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad"))
                .when(orderService).createOrderFromEvent(any(CreateOrder.class), anyString());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> consumer.handleAuctionSettled(payload));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void handleAuctionSettled_InvalidJson_IsWrapped() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> consumer.handleAuctionSettled("{bad json"));

        assertNotNull(exception.getCause());
        verify(orderService, never()).createOrderFromEvent(any(), anyString());
    }

    @Test
    void handleAuctionSettled_NoWinners_IsWrapped() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        String payload = "{\"auctionId\":\"%s\",\"listingId\":\"%s\",\"winners\":[]}"
                .formatted(auctionId, listingId);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> consumer.handleAuctionSettled(payload));

        assertNotNull(exception.getCause());
    }

    @Test
    void handleAuctionSettled_NullListing_IsWrapped() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","winners":[{"userId":"%s","amount":100}]}
                """.formatted(auctionId, listingId, buyerId);

        when(restTemplate.getForObject("http://catalog/listings/" + listingId, Map.class)).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> consumer.handleAuctionSettled(payload));

        assertNotNull(exception.getCause());
    }

    @Test
    void handleAuctionSettled_MissingAmount_IsWrapped() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","winners":[{"userId":"%s"}]}
                """.formatted(auctionId, listingId, buyerId);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> consumer.handleAuctionSettled(payload));

        assertNotNull(exception.getCause());
    }

    @Test
    void handleAuctionSettled_BlankRequiredListingSeller_IsWrapped() {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String payload = """
                {"auctionId":"%s","listingId":"%s","winners":[{"userId":"%s","amount":100}]}
                """.formatted(auctionId, listingId, buyerId);

        when(restTemplate.getForObject("http://catalog/listings/" + listingId, Map.class))
                .thenReturn(Map.of("sellerId", " "));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> consumer.handleAuctionSettled(payload));

        assertNotNull(exception.getCause());
    }

    private void mockUser(UUID userId, String displayName) {
        when(restTemplate.exchange(
                eq("http://user/internal/users/" + userId),
                eq(HttpMethod.GET),
                any(),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(Map.of("displayName", displayName)));
    }
}
