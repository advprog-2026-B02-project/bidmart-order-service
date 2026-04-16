package id.ac.ui.cs.advprog.bidmart.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateShippingRequest {
    private String status;           
    private String courier;
    private String trackingNumber;
}