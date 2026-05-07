package id.ac.ui.cs.advprog.bidmart.order.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

class ServiceTokenFilterTest {

    private ServiceTokenFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new ServiceTokenFilter();
        Field f = ServiceTokenFilter.class.getDeclaredField("serviceToken");
        f.setAccessible(true);
        f.set(filter, "secret-token");
    }

    @Test
    void doFilterInternal_UnauthorizedWhenMissingOrWrongToken() throws ServletException, IOException {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/internal/v1/foo");
        when(req.getHeader("X-Service-Token")).thenReturn("bad-token");

        filter.doFilterInternal(req, res, chain);

        verify(res).setStatus(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_AllowsWhenTokenMatches() throws ServletException, IOException {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/internal/v1/foo");
        when(req.getHeader("X-Service-Token")).thenReturn("secret-token");

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        verify(res, never()).setStatus(anyInt());
    }
}
