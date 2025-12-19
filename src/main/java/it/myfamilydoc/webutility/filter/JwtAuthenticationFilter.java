
package it.myfamilydoc.webutility.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.myfamilydoc.webutility.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                   @NonNull HttpServletResponse response, 
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && validateToken(jwt)) {
                Claims claims = getClaimsFromToken(jwt);
                UserPrincipal userPrincipal = createUserPrincipal(claims);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                logger.debug("Authentication set for user: {} with authorities: {}", 
                           userPrincipal.getUsername(), userPrincipal.getAuthorities());
            }
        } catch (ExpiredJwtException ex) {
            logger.warn("JWT token expired: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT Token expired");
            return;
        } catch (JwtException ex) {
            logger.warn("Invalid JWT token: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");
            return;
        } catch (Exception ex) {
            logger.error("Cannot set user authentication", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            if (!StringUtils.hasText(jwtSecret)) {
                logger.error("JWT secret is not configured");
                return false;
            }
            
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private UserPrincipal createUserPrincipal(Claims claims) {
        Long userId = null;
        String username = claims.getSubject();
        String email = claims.get("email", String.class);
        
        // Estrazione sicura dell'userId
        Object userIdClaim = claims.get("userId");
        if (userIdClaim != null) {
            if (userIdClaim instanceof Number) {
                userId = ((Number) userIdClaim).longValue();
            } else if (userIdClaim instanceof String) {
                try {
                    userId = Long.parseLong((String) userIdClaim);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid userId format in JWT: {}", userIdClaim);
                }
            }
        }
        
        // Estrazione ruoli dal JWT
        Collection<SimpleGrantedAuthority> authorities = extractAuthorities(claims);
        
        return new UserPrincipal(userId, username, email, "", authorities, true);
    }
    
    @SuppressWarnings("unchecked")
    private Collection<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        // Prova diversi formati comuni per i ruoli nel JWT
        Object rolesObj = claims.get("roles");
        if (rolesObj == null) {
            rolesObj = claims.get("authorities");
        }
        if (rolesObj == null) {
            rolesObj = claims.get("role");
        }
        
        if (rolesObj instanceof List) {
            List<String> roles = (List<String>) rolesObj;
            return roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else if (rolesObj instanceof String) {
            String role = (String) rolesObj;
            role = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            return Collections.singletonList(new SimpleGrantedAuthority(role));
        }
        
        // Default: nessun ruolo specifico, solo USER
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}