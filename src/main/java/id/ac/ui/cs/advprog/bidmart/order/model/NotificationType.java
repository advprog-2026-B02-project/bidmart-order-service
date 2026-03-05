package id.ac.ui.cs.advprog.bidmart.order.model;

public enum NotificationType {
    BID_PLACED,
    OUTBID,
    AUCTION_WON,
    AUCTION_LOST,
    AUCTION_EXTENDED,
    AUCTION_ENDED,

    ORDER_CREATED,
    ORDER_SHIPPED,
    ORDER_COMPLETED,
    DISPUTE_CREATED,
    DISPUTE_RESOLVED,

    WALLET_UPDATED
}