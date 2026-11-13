package com.placement.campus_placement_system.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts incoming HTTP requests once per request, extracts and validates Bearer JWT tokens,
 * and populates the Spring SecurityContext when the token is valid and the user is active.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String JWT_ERROR_ATTRIBUTE = "jwt_error_message";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            request.setAttribute(JWT_ERROR_ATTRIBUTE, "Missing JWT token");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (!userDetails.isEnabled()) {
                    SecurityContextHolder.clearContext();
                    request.setAttribute(JWT_ERROR_ATTRIBUTE, "User account is inactive");
                } else if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                    request.setAttribute(JWT_ERROR_ATTRIBUTE, "Invalid JWT token");
                }
            }
        } catch (ExpiredJwtException ex) {
            log.debug("Expired JWT token on request {}: {}", request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
            request.setAttribute(JWT_ERROR_ATTRIBUTE, "JWT token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT token on request {}: {}", request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
            request.setAttribute(JWT_ERROR_ATTRIBUTE, "Invalid JWT token");
        } catch (UsernameNotFoundException ex) {
            log.debug("User referenced by JWT no longer exists: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            request.setAttribute(JWT_ERROR_ATTRIBUTE, "Invalid JWT token: user not found");
        }

        filterChain.doFilter(request, response);
    }
}
