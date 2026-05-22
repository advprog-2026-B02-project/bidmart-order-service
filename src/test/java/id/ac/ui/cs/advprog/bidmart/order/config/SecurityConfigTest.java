package id.ac.ui.cs.advprog.bidmart.order.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class SecurityConfigTest {

    @Test
    void corsConfigurationSource_includesFrontendUrl() throws Exception {
        JwtAuthFilter jwt = mock(JwtAuthFilter.class);
        ServiceTokenFilter svc = mock(ServiceTokenFilter.class);

        SecurityConfig cfg = new SecurityConfig(jwt, svc);

        // inject frontendUrl
        Field f = SecurityConfig.class.getDeclaredField("frontendUrl");
        f.setAccessible(true);
        f.set(cfg, "http://example.com");

        var source = cfg.corsConfigurationSource();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        req.setRequestURI("/test");
        var cors = source.getCorsConfiguration(req);
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(cors.getAllowedOrigins().contains("http://example.com"));
    }
}
