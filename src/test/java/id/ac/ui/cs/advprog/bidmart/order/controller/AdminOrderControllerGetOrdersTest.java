package id.ac.ui.cs.advprog.bidmart.order.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.bidmart.order.dto.OrderListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.OrderSummary;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;

@SpringBootTest(properties = "jwt.secret=test-jwt-secret-key-test-jwt-secret-key-123456")
@AutoConfigureMockMvc
class AdminOrderControllerGetOrdersTest {

    private static final String JWT_SECRET = "test-jwt-secret-key-test-jwt-secret-key-123456";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private String adminToken;
    private String buyerToken;

    @BeforeEach
    void setUp() {
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
    void getOrders_AllowsAdmin() throws Exception {
        OrderSummary s = OrderSummary.builder()
                .id(UUID.randomUUID())
                .auctionId(UUID.randomUUID())
                .listingTitle("Item")
                .amount(100)
                .buyer(OrderSummary.UserBasicDTO.builder().id(UUID.randomUUID()).displayName("B").build())
                .seller(OrderSummary.UserBasicDTO.builder().id(UUID.randomUUID()).displayName("S").build())
                .status("DISPUTED")
                .build();

        OrderListResponse resp = OrderListResponse.builder()
                .content(List.of(s))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(orderService.getOrdersAdmin(eq("DISPUTED"), eq(0), eq(20))).thenReturn(resp);

        mockMvc.perform(get("/admin/v1/orders").param("status", "DISPUTED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getOrders_ForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/admin/v1/orders").param("status", "DISPUTED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }
}
