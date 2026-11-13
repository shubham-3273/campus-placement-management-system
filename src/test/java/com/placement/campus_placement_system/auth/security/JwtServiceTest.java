package com.placement.campus_placement_system.auth.security;

import com.placement.campus_placement_system.auth.entity.User;
import com.placement.campus_placement_system.auth.enums.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-jwt-secret-key-with-at-least-32-characters-12345";

    private JwtService jwtService;
    private User studentUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(TEST_SECRET);
        properties.setExpiration(3600000L);
        jwtService = new JwtService(properties);

        studentUser = User.builder()
                .userId(42L)
                .name("Test Student")
                .email("student@example.com")
                .password("$2a$10$hashedpassword")
                .role(Role.STUDENT)
                .active(true)
                .build();
        userDetails = new CustomUserDetails(studentUser);
    }

    @Test
    void generateToken_shouldIncludeIdentityAndRoleClaims() {
        String token = jwtService.generateToken(studentUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("student@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractRole(token)).isEqualTo("STUDENT");
        assertThat(jwtService.extractExpiration(token)).isInTheFuture();
    }

    @Test
    void isTokenValid_shouldReturnTrueForMatchingUser() {
        String token = jwtService.generateToken(studentUser);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalseForDifferentUser() {
        String token = jwtService.generateToken(studentUser);

        User otherUser = User.builder()
                .userId(99L)
                .name("Other Student")
                .email("other@example.com")
                .password("hashed")
                .role(Role.STUDENT)
                .active(true)
                .build();

        assertThat(jwtService.isTokenValid(token, new CustomUserDetails(otherUser))).isFalse();
    }

    @Test
    void extractEmail_shouldRejectTamperedToken() {
        String token = jwtService.generateToken(studentUser);
        String tamperedToken = token.substring(0, token.length() - 4) + "abcd";

        assertThatThrownBy(() -> jwtService.extractEmail(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractEmail_shouldRejectExpiredToken() {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(TEST_SECRET);
        expiredProps.setExpiration(-1000L);
        JwtService expiredJwtService = new JwtService(expiredProps);

        String expiredToken = expiredJwtService.generateToken(studentUser);

        assertThatThrownBy(() -> jwtService.extractEmail(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
