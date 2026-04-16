package id.ac.ui.cs.advprog.bidmart.order.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateShippingRequestTest {

    @Test
    void builderShouldPopulateFields() {
        UpdateShippingRequest request = UpdateShippingRequest.builder()
                .status("SHIPPED")
                .courier("JNE")
                .trackingNumber("TRK123")
                .build();

        assertEquals("SHIPPED", request.getStatus());
        assertEquals("JNE", request.getCourier());
        assertEquals("TRK123", request.getTrackingNumber());
    }
}
