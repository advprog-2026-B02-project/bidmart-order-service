package id.ac.ui.cs.advprog.bidmart.order.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AuctionSettledEvent(
    UUID eventId,
    UUID auctionId,
    String auctionType,
    List<WinnerDto> winners,
    LocalDateTime occurredAt
) {}
