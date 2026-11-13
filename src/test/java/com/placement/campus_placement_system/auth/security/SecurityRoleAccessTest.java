package com.placement.campus_placement_system.auth.security;

import com.placement.campus_placement_system.auth.entity.User;
import com.placement.campus_placement_system.auth.enums.Role;
import com.placement.campus_placement_system.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(SecurityRoleAccessTest.TestRoleProbeConfiguration.class)
@Transactional
class SecurityRoleAccessTest {

    @TestConfiguration
    static class TestRoleProbeConfiguration {

        @RestController
        static class TestRoleProbeController {

            @GetMapping("/api/student/probe")
            public String studentProbe() {
                return "student-ok";
            }

            @GetMapping("/api/tpo/probe")
            public String tpoProbe() {
                return "tpo-ok";
            }

            @GetMapping("/api/recruiter/probe")
            public String recruiterProbe() {
                return "recruiter-ok";
            }
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private String studentToken;
    private String tpoToken;
    private String recruiterToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        User student = createUser("Student Probe", "probe.student@example.com", Role.STUDENT);
        User tpo = createUser("TPO Probe", "probe.tpo@example.com", Role.TPO);
        User recruiter = createUser("Recruiter Probe", "probe.recruiter@example.com", Role.RECRUITER);

        studentToken = jwtService.generateToken(student);
        tpoToken = jwtService.generateToken(tpo);
        recruiterToken = jwtService.generateToken(recruiter);
    }

    @Test
    void protectedRoutes_shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/student/probe")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tpo/probe")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/recruiter/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentRoute_shouldAllowOnlyStudentRole() throws Exception {
        mockMvc.perform(get("/api/student/probe")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(content().string("student-ok"));

        mockMvc.perform(get("/api/student/probe")
                        .header("Authorization", "Bearer " + tpoToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/student/probe")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void tpoRoute_shouldAllowOnlyTpoRole() throws Exception {
        mockMvc.perform(get("/api/tpo/probe")
                        .header("Authorization", "Bearer " + tpoToken))
                .andExpect(status().isOk())
                .andExpect(content().string("tpo-ok"));

        mockMvc.perform(get("/api/tpo/probe")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void recruiterRoute_shouldAllowOnlyRecruiterRole() throws Exception {
        mockMvc.perform(get("/api/recruiter/probe")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(content().string("recruiter-ok"));

        mockMvc.perform(get("/api/recruiter/probe")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private User createUser(String name, String email, Role role) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("Password@123"))
                .role(role)
                .active(true)
                .build());
    }
}
