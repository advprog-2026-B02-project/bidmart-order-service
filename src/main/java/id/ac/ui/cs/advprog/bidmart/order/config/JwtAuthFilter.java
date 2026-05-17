package id.ac.ui.cs.advprog.bidmart.order.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7).trim();
            try {
                Authentication authentication = buildAuthentication(token, request);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private Authentication buildAuthentication(String token, HttpServletRequest request) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Jws<Claims> jws = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token);

        Claims claims = jws.getPayload();
        String principal = resolvePrincipal(claims);
        Collection<SimpleGrantedAuthority> authorities = resolveAuthorities(claims);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }

    private String resolvePrincipal(Claims claims) {
        String subject = claims.getSubject();
        if (StringUtils.hasText(subject)) {
            return subject;
        }

        Object userId = claims.get("userId");
        if (userId != null) {
            return String.valueOf(userId);
        }

        return "authenticated-user";
    }

    private Collection<SimpleGrantedAuthority> resolveAuthorities(Claims claims) {
        List<String> rawRoles = new ArrayList<>();

        addClaimValues(rawRoles, claims.get("roles"));
        addClaimValues(rawRoles, claims.get("authorities"));
        addClaimValues(rawRoles, claims.get("role"));

        return rawRoles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private void addClaimValues(List<String> roles, Object claimValue) {
        if (claimValue == null) {
            return;
        }

        if (claimValue instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (value != null) {
                    roles.add(String.valueOf(value));
                }
            }
            return;
        }

        if (claimValue instanceof String value) {
            if (value.contains(",")) {
                for (String part : value.split(",")) {
                    roles.add(part);
                }
            } else {
                roles.add(value);
            }
            return;
        }

        if (claimValue instanceof Map<?, ?> map) {
            map.values().forEach(value -> {
                if (value != null) {
                    roles.add(String.valueOf(value));
                }
            });
            return;
        }

        roles.add(String.valueOf(claimValue));
    }
}
