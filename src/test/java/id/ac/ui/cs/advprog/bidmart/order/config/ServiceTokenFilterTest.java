package id.ac.ui.cs.advprog.bidmart.order.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ServiceTokenFilterTest {

    private ServiceTokenFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new ServiceTokenFilter();
        // inject serviceToken
        Field f = ServiceTokenFilter.class.getDeclaredField("serviceToken");
        f.setAccessible(true);
        f.set(filter, "secret-token");
    }

    @Test
    void doFilterInternal_missingOrWrongToken_returnsUnauthorized() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/internal/some");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        assertEquals(401, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_correctToken_callsChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/internal/some");
        req.addHeader("X-Service-Token", "secret-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        // default status is 200 (or 0), and chain should be called
        verify(chain, times(1)).doFilter(req, res);
    }
}
 
