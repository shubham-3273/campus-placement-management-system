package com.placement.campus_placement_system.auth.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly typed, startup-validated JWT configuration bound from environment variables.
 * Missing or invalid values fail fast on startup with a clear validation message.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotBlank(message = "JWT_SECRET environment variable must be set and non-blank.")
    @Size(min = 32, message = "JWT_SECRET must be at least 32 characters (256 bits) long for HS256 signing.")
    private String secret;

    @NotNull(message = "JWT_EXPIRATION environment variable must be set (in milliseconds, e.g., 86400000).")
    @Positive(message = "JWT_EXPIRATION must be a positive number of milliseconds.")
    private Long expiration;
}
