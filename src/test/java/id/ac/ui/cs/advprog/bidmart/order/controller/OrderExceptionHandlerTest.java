package id.ac.ui.cs.advprog.bidmart.order.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderExceptionHandlerTest {

    private final OrderExceptionHandler handler = new OrderExceptionHandler();

    @Test
    void handleResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "msg");
        ResponseEntity<Map<String, Object>> res = handler.handleResponseStatusException(ex);
        assertEquals(404, res.getStatusCode().value());
        assertEquals("msg", res.getBody().get("message"));
    }

    @Test
    void handleResponseStatusException_UsesExceptionMessageWhenReasonNull() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map<String, Object>> res = handler.handleResponseStatusException(ex);

        assertEquals(400, res.getStatusCode().value());
        assertEquals(ex.getMessage(), res.getBody().get("message"));
    }

    @Test
    void handleGenericException() {
        ResponseEntity<Map<String, Object>> res = handler.handleGenericException(new Exception("error"));
        assertEquals(500, res.getStatusCode().value());
    }
}
