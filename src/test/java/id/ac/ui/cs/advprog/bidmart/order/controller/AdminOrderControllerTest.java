package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.order.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminOrderController controller;

    @Test
    void resolveDispute_CallsServiceAndReturnsOk() {
        UUID orderId = UUID.randomUUID();
        ResolveDisputeRequest req = ResolveDisputeRequest.builder().resolution("res").note("n").build();

        doNothing().when(orderService).resolveDispute(orderId, req);

        ResponseEntity<Void> res = controller.resolveDispute(orderId, req);

        assertEquals(200, res.getStatusCodeValue());
    }
}
