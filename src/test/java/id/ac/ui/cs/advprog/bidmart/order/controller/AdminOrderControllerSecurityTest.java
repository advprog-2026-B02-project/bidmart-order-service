package id.ac.ui.cs.advprog.bidmart.order.controller;

import id.ac.ui.cs.advprog.bidmart.order.dto.ResolveDisputeRequest;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "jwt.secret=test-jwt-secret-key-test-jwt-secret-key-123456")
@AutoConfigureMockMvc
class AdminOrderControllerSecurityTest {

    private static final String JWT_SECRET = "test-jwt-secret-key-test-jwt-secret-key-123456";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private UUID orderId;
    private String adminToken;
    private String buyerToken;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        doNothing().when(orderService).resolveDispute(eq(orderId), any(ResolveDisputeRequest.class));

        Key key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        adminToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("ADMIN"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();

        buyerToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("BUYER"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();
    }

    @Test
    void resolveDispute_ForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(put("/admin/v1/orders/{orderId}/dispute/resolve", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\":\"REFUND_BUYER\",\"note\":\"ok\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resolveDispute_AllowsAdmin() throws Exception {
        mockMvc.perform(put("/admin/v1/orders/{orderId}/dispute/resolve", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\":\"REFUND_BUYER\",\"note\":\"ok\"}"))
                .andExpect(status().isOk());
    }
}
