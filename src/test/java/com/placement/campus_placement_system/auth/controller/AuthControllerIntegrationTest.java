package com.placement.campus_placement_system.auth.controller;

import com.placement.campus_placement_system.auth.dto.request.LoginRequest;
import com.placement.campus_placement_system.auth.dto.request.RegisterRequest;
import com.placement.campus_placement_system.auth.entity.User;
import com.placement.campus_placement_system.auth.enums.Role;
import com.placement.campus_placement_system.auth.repository.UserRepository;
import com.placement.campus_placement_system.auth.security.JwtProperties;
import com.placement.campus_placement_system.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void register_shouldCreateStudentAndHidePassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test Student")
                .email("student.new@example.com")
                .password("Student@123")
                .role(Role.STUDENT)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.name").value("Test Student"))
                .andExpect(jsonPath("$.email").value("student.new@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        User saved = userRepository.findByEmailIgnoreCase("student.new@example.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("Student@123");
        assertThat(passwordEncoder.matches("Student@123", saved.getPassword())).isTrue();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("Student@123");
    }

    @Test
    void register_shouldRejectDuplicateEmail() throws Exception {
        userRepository.save(User.builder()
                .name("Existing Student")
                .email("duplicate@example.com")
                .password(passwordEncoder.encode("Student@123"))
                .role(Role.STUDENT)
                .active(true)
                .build());

        RegisterRequest request = RegisterRequest.builder()
                .name("Another Student")
                .email("duplicate@example.com")
                .password("Student@123")
                .role(Role.STUDENT)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void register_shouldRejectInvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test Student")
                .email("not-an-email")
                .password("Student@123")
                .role(Role.STUDENT)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void register_shouldRejectShortPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test Student")
                .email("shortpass@example.com")
                .password("1234567")
                .role(Role.STUDENT)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void register_shouldRejectPublicTpoRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Admin TPO")
                .email("tpo.public@example.com")
                .password("Admin@123")
                .role(Role.TPO)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void register_shouldRejectPublicRecruiterRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Company Recruiter")
                .email("recruiter.public@example.com")
                .password("Recruiter@123")
                .role(Role.RECRUITER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void login_andGetCurrentUser_shouldSucceedWithValidCredentials() throws Exception {
        userRepository.save(User.builder()
                .name("Active Student")
                .email("login.student@example.com")
                .password(passwordEncoder.encode("Student@123"))
                .role(Role.STUDENT)
                .active(true)
                .build());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("login.student@example.com")
                .password("Student@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.name").value("Active Student"))
                .andExpect(jsonPath("$.email").value("login.student@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = json.get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Active Student"))
                .andExpect(jsonPath("$.email").value("login.student@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void login_shouldReturn401ForIncorrectPassword() throws Exception {
        userRepository.save(User.builder()
                .name("Active Student")
                .email("wrongpass@example.com")
                .password(passwordEncoder.encode("Student@123"))
                .role(Role.STUDENT)
                .active(true)
                .build());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("wrongpass@example.com")
                .password("WrongPassword@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_shouldRejectInactiveAccount() throws Exception {
        userRepository.save(User.builder()
                .name("Inactive Student")
                .email("inactive@example.com")
                .password(passwordEncoder.encode("Student@123"))
                .role(Role.STUDENT)
                .active(false)
                .build());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("inactive@example.com")
                .password("Student@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void me_shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void me_shouldReturn401ForExpiredJwt() throws Exception {
        User saved = userRepository.save(User.builder()
                .name("Expired Token Student")
                .email("expired.jwt@example.com")
                .password(passwordEncoder.encode("Student@123"))
                .role(Role.STUDENT)
                .active(true)
                .build());

        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret("test-only-jwt-secret-key-must-be-at-least-32-chars");
        expiredProps.setExpiration(-5000L);
        String expiredToken = new JwtService(expiredProps).generateToken(saved);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("JWT token has expired"));
    }

    @Test
    void me_shouldReturn401ForInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not.a.valid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid JWT token"));
    }

    @Test
    void actuatorHealth_shouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
