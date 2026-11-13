package com.placement.campus_placement_system.auth.service;

import com.placement.campus_placement_system.auth.dto.request.LoginRequest;
import com.placement.campus_placement_system.auth.dto.request.RegisterRequest;
import com.placement.campus_placement_system.auth.dto.response.AuthResponse;
import com.placement.campus_placement_system.auth.dto.response.UserResponse;
import com.placement.campus_placement_system.auth.entity.User;
import com.placement.campus_placement_system.auth.enums.Role;
import com.placement.campus_placement_system.auth.repository.UserRepository;
import com.placement.campus_placement_system.auth.security.CustomUserDetails;
import com.placement.campus_placement_system.auth.security.JwtService;
import com.placement.campus_placement_system.exception.DuplicateEmailException;
import com.placement.campus_placement_system.exception.InvalidRoleRegistrationException;
import com.placement.campus_placement_system.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Handles user registration, credential authentication, and current-user retrieval.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.getRole() != Role.STUDENT) {
            throw new InvalidRoleRegistrationException(
                    "Public registration is permitted only for the STUDENT role"
            );
        }

        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .active(Boolean.TRUE)
                .build();

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        log.info("Registered new student user with id={} and email={}", savedUser.getUserId(), savedUser.getEmail());
        return toUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmailIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        log.info("User authenticated successfully: id={}, role={}", user.getUserId(), user.getRole());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .tokenType("Bearer")
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUser) {
            return UserResponse.builder()
                    .userId(customUser.getUserId())
                    .name(customUser.getName())
                    .email(customUser.getEmail())
                    .role(customUser.getRole())
                    .build();
        }

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
