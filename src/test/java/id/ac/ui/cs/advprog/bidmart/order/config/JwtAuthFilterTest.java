package id.ac.ui.cs.advprog.bidmart.order.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JwtAuthFilterTest {

    @AfterEach
    void cleanup() {
        // clear security context if any
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_invalidToken_clearsSecurityContext() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter();

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer invalid.token.value");

        FilterChain chain = mock(FilterChain.class);

        // Should not throw even if token parsing fails
        filter.doFilterInternal(req, res, chain);

        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void resolvePrincipal_and_resolveAuthorities_viaReflection() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter();

        Claims claims = mock(Claims.class);

        // subject present
        when(claims.getSubject()).thenReturn("bob");

        Method resolvePrincipal = JwtAuthFilter.class.getDeclaredMethod("resolvePrincipal", Claims.class);
        resolvePrincipal.setAccessible(true);
        Object res1 = resolvePrincipal.invoke(filter, claims);
        assertEquals("bob", res1);

        // subject empty, userId present
        when(claims.getSubject()).thenReturn("");
        when(claims.get("userId")).thenReturn(123);
        Object res2 = resolvePrincipal.invoke(filter, claims);
        assertEquals("123", res2);

        // no subject nor userId
        when(claims.getSubject()).thenReturn(null);
        when(claims.get("userId")).thenReturn(null);
        Object res3 = resolvePrincipal.invoke(filter, claims);
        assertEquals("authenticated-user", res3);

        // test resolveAuthorities
        when(claims.get("roles")).thenReturn(List.of("ADMIN"));
        when(claims.get("authorities")).thenReturn("USER,MANAGER");
        when(claims.get("role")).thenReturn(Map.of("r","GUEST"));

        Method resolveAuthorities = JwtAuthFilter.class.getDeclaredMethod("resolveAuthorities", Claims.class);
        resolveAuthorities.setAccessible(true);
        Object auths = resolveAuthorities.invoke(filter, claims);
        assertTrue(auths instanceof java.util.Collection);
        @SuppressWarnings("unchecked")
        java.util.Collection<?> coll = (java.util.Collection<?>) auths;
        // Expect ROLE_ADMIN, ROLE_USER, ROLE_MANAGER, ROLE_GUEST (order-insensitive)
        assertTrue(coll.stream().anyMatch(o -> o.toString().contains("ROLE_ADMIN")));
        assertTrue(coll.stream().anyMatch(o -> o.toString().contains("ROLE_USER")));
        assertTrue(coll.stream().anyMatch(o -> o.toString().contains("ROLE_MANAGER")));
        assertTrue(coll.stream().anyMatch(o -> o.toString().contains("ROLE_GUEST")));
    }
}
