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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_shouldEncodePasswordAndSaveStudent() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test Student")
                .email("Student@Example.com")
                .password("Student@123")
                .role(Role.STUDENT)
                .build();

        when(userRepository.existsByEmailIgnoreCase("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Student@123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(1L);
            return u;
        });

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("student@example.com");
        assertThat(saved.getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getActive()).isTrue();

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Student");
        assertThat(response.getEmail()).isEqualTo("student@example.com");
        assertThat(response.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test Student")
                .email("student@example.com")
                .password("Student@123")
                .role(Role.STUDENT)
                .build();

        when(userRepository.existsByEmailIgnoreCase("student@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_shouldRejectPublicTpoRegistration() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Admin TPO")
                .email("tpo@example.com")
                .password("Admin@123")
                .role(Role.TPO)
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(InvalidRoleRegistrationException.class);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void register_shouldRejectPublicRecruiterRegistration() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Company Recruiter")
                .email("recruiter@example.com")
                .password("Recruiter@123")
                .role(Role.RECRUITER)
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(InvalidRoleRegistrationException.class);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void login_shouldReturnAuthResponseWithJwt() {
        LoginRequest request = LoginRequest.builder()
                .email("student@example.com")
                .password("Student@123")
                .build();

        User user = User.builder()
                .userId(1L)
                .name("Test Student")
                .email("student@example.com")
                .password("$2a$10$encoded")
                .role(Role.STUDENT)
                .active(true)
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("signed-jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Student");
        assertThat(response.getEmail()).isEqualTo("student@example.com");
        assertThat(response.getRole()).isEqualTo(Role.STUDENT);
        assertThat(response.getToken()).isEqualTo("signed-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_shouldPropagateBadCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("student@example.com")
                .password("WrongPass")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_shouldPropagateDisabledAccount() {
        LoginRequest request = LoginRequest.builder()
                .email("inactive@example.com")
                .password("Student@123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void getCurrentUser_shouldReadPrincipalFromSecurityContext() {
        User user = User.builder()
                .userId(5L)
                .name("Current Student")
                .email("current@example.com")
                .password("$2a$10$encoded")
                .role(Role.STUDENT)
                .active(true)
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        UserResponse currentUser = authService.getCurrentUser();

        assertThat(currentUser.getUserId()).isEqualTo(5L);
        assertThat(currentUser.getName()).isEqualTo("Current Student");
        assertThat(currentUser.getEmail()).isEqualTo("current@example.com");
        assertThat(currentUser.getRole()).isEqualTo(Role.STUDENT);
    }
}
