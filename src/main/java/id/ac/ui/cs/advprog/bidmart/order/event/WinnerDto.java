package id.ac.ui.cs.advprog.bidmart.order.event;

import java.util.UUID;

public record WinnerDto(
    UUID userId,
    Long amount
) {}