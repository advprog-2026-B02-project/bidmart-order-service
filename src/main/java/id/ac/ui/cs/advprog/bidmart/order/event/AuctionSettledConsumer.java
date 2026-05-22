package id.ac.ui.cs.advprog.bidmart.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.order.client.CatalogClient;
import id.ac.ui.cs.advprog.bidmart.order.client.UserClient;
import id.ac.ui.cs.advprog.bidmart.order.dto.CreateOrder;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionSettledConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final UserClient userClient;
    private final CatalogClient catalogClient;

    @KafkaListener(topics = "auction.settled", groupId = "order-group")
    public void consumeAuctionSettled(String messageJson) {
        log.info("Menerima sinyal lelang selesai dari Kafka: {}", messageJson);

        try {
            AuctionSettledEvent event = objectMapper.readValue(messageJson, AuctionSettledEvent.class);
            UUID auctionId = event.auctionId();

            if (event.winners() == null || event.winners().isEmpty()) {
                log.info("Lelang {} selesai tanpa pemenang. Tidak ada pesanan yang dibuat.", auctionId);
                return;
            }

            UUID winnerId = event.winners().get(0).userId();
            Integer winningAmount = event.winners().get(0).amount().intValue();
            String idempotencyKey = event.eventId().toString(); 

            var listingInfo = catalogClient.getListingById(auctionId);
            String imageUrl = (listingInfo.getImages() != null && !listingInfo.getImages().isEmpty()) 
                    ? listingInfo.getImages().get(0).getUrl() : null;

            var buyerInfo = userClient.getUserById(winnerId);
            var sellerInfo = userClient.getUserById(listingInfo.getSellerId());

            boolean isAddressEmpty = buyerInfo.getShippingStreet() == null || buyerInfo.getShippingStreet().isBlank();
            if (isAddressEmpty) {
                log.warn("{} memenangkan lelang tetapi belum mengisi alamat pengiriman!", winnerId);
            }

            CreateOrder newOrderPayload = CreateOrder.builder()
                .auctionId(auctionId)
                .listingId(listingInfo.getId())
                .listingTitle(listingInfo.getTitle())
                .listingImageUrl(imageUrl)
                
                .buyerId(winnerId)
                .buyerDisplayName(buyerInfo.getDisplayName())
                .shippingStreet(buyerInfo.getShippingStreet() != null ? buyerInfo.getShippingStreet() : "")
                .shippingCity(buyerInfo.getShippingCity() != null ? buyerInfo.getShippingCity() : "")
                .shippingProvince(buyerInfo.getShippingProvince() != null ? buyerInfo.getShippingProvince() : "")
                .shippingPostalCode(buyerInfo.getShippingPostalCode() != null ? buyerInfo.getShippingPostalCode() : "")
                
                .sellerId(sellerInfo.getId())
                .sellerDisplayName(sellerInfo.getDisplayName())
                .totalAmount(winningAmount)
                .build();

            orderService.createOrderFromEvent(newOrderPayload, idempotencyKey);
            log.info("Proses selesai: Pesanan otomatis berhasil diterbitkan untuk lelang ID: {}", auctionId);

        } catch (Exception e) {
            log.error("Proses gagal: Terjadi kendala saat merakit data pesanan otomatis dari Kafka", e);
        }
    }
}