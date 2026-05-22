package id.ac.ui.cs.advprog.bidmart.order.dto.client;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalUserResponseDTO {
    private UUID id;
    private String displayName;
    private String shippingStreet;
    private String shippingCity;
    private String shippingProvince;
    private String shippingPostalCode;
}