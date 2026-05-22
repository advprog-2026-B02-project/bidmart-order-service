package id.ac.ui.cs.advprog.bidmart.order.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AuctionSettledEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final RestTemplate restTemplate;

    @Value("${app.client.bidding-url:http://localhost:8082}")
    private String biddingUrl;

    @Value("${app.client.catalog-url:http://localhost:8083}")
    private String catalogUrl;

    @Value("${app.client.user-url:http://localhost:8081}")
    private String userUrl;

    @Value("${app.service-token}")
    private String serviceToken;

    public AuctionSettledEventConsumer(ObjectMapper objectMapper, OrderService orderService, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(
            topics = "${app.kafka.auction-settled-topic:auction.settled}",
            groupId = "${app.kafka.auction-settled-consumer-group:order-service}")
    public void handleAuctionSettled(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            if (optionalUuid(event, "listingId") == null) {
                log.warn("Skipping legacy auction.settled event without listingId: {}", event.path("auctionId").asText());
                return;
            }

            CreateOrder order = mapToCreateOrder(event);
            String idempotencyKey = event.path("eventId").asText("auction-settled-" + order.getAuctionId());

            orderService.createOrderFromEvent(order, idempotencyKey);
            log.info("Created order for settled auction {}", order.getAuctionId());
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 409) {
                log.info("Skipping settled auction event because order already exists or key was reused: {}", ex.getReason());
                return;
            }
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create order from auction.settled payload: {}", payload, ex);
            throw new IllegalStateException("Failed to create order from auction.settled", ex);
        }
    }

    private CreateOrder mapToCreateOrder(JsonNode event) {
        UUID auctionId = requiredUuid(event, "auctionId");
        JsonNode winner = firstWinner(event);
        UUID buyerId = requiredUuid(winner, "userId");
        UUID listingId = optionalUuid(event, "listingId");

        ListingSnapshot listing = fetchListing(listingId);
        UUID sellerId = optionalUuid(event, "sellerId");
        if (sellerId == null) {
            sellerId = listing.sellerId();
        }

        UserSnapshot buyer = fetchUser(buyerId);
        UserSnapshot seller = fetchUser(sellerId);

        return CreateOrder.builder()
                .auctionId(auctionId)
                .listingId(listingId)
                .listingTitle(listing.title())
                .listingImageUrl(listing.imageUrl())
                .buyerId(buyerId)
                .buyerDisplayName(buyer.displayName())
                .shippingStreet(null)
                .shippingCity(null)
                .shippingProvince(null)
                .shippingPostalCode(null)
                .sellerId(sellerId)
                .sellerDisplayName(seller.displayName())
                .totalAmount(toIntegerAmount(winner.path("amount")))
                .build();
    }

    private JsonNode firstWinner(JsonNode event) {
        JsonNode winners = event.path("winners");
        if (!winners.isArray() || winners.isEmpty()) {
            throw new IllegalArgumentException("auction.settled event does not contain winners");
        }
        return winners.get(0);
    }

    private ListingSnapshot fetchListing(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = restTemplate.getForObject(catalogUrl + "/listings/" + listingId, Map.class);
        if (listing == null) {
            throw new IllegalArgumentException("Cannot resolve listing " + listingId);
        }

        UUID sellerId = UUID.fromString(requiredString(listing, "sellerId"));
        String title = stringOrDefault(listing, "title", "Listing " + listingId);
        String imageUrl = firstImageUrl(listing.get("images"));
        return new ListingSnapshot(sellerId, title, imageUrl);
    }

    private UserSnapshot fetchUser(UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Token", serviceToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        Map<String, Object> user = null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    userUrl + "/internal/users/" + userId,
                    HttpMethod.GET,
                    request,
                    Map.class
            ).getBody();
            user = response;
        } catch (RestClientException ex) {
            log.warn("Failed to fetch user snapshot for {}; using fallback display name", userId, ex);
        }

        String displayName = user != null
                ? stringOrDefault(user, "displayName", "User " + userId.toString().substring(0, 8))
                : "User " + userId.toString().substring(0, 8);
        return new UserSnapshot(displayName);
    }

    private String firstImageUrl(Object imagesValue) {
        if (!(imagesValue instanceof List<?> images) || images.isEmpty()) {
            return null;
        }

        Object first = images.get(0);
        if (!(first instanceof Map<?, ?> image)) {
            return null;
        }

        Object thumbnail = image.get("thumbnailUrl");
        if (thumbnail != null && !thumbnail.toString().isBlank()) {
            return thumbnail.toString();
        }

        Object url = image.get("url");
        return url != null && !url.toString().isBlank() ? url.toString() : null;
    }

    private UUID requiredUuid(JsonNode node, String field) {
        UUID value = optionalUuid(node, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private UUID optionalUuid(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return UUID.fromString(value.asText());
    }

    private Integer toIntegerAmount(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException("winner amount is required");
        }
        BigDecimal amount = value.decimalValue();
        return amount.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private String requiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.toString();
    }

    private String stringOrDefault(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    private record ListingSnapshot(UUID sellerId, String title, String imageUrl) {
    }

    private record UserSnapshot(String displayName) {
    }
}
