package com.smarted.ed.controller;

import tools.jackson.databind.ObjectMapper;
import com.smarted.ed.dto.LoginRequest;
import com.smarted.ed.dto.SignupRequest;
import com.smarted.ed.entity.User;
import com.smarted.ed.entity.VerificationToken;
import com.smarted.ed.enums.RoleType;
import com.smarted.ed.repository.UserRepository;
import com.smarted.ed.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testSignupSuccess() throws Exception {
        SignupRequest request = new SignupRequest(
                "signup_test@example.com",
                "password123",
                "Signup Test User",
                "0987654321",
                RoleType.TUTOR
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Đăng ký tài khoản thành công")));

        // Verify user is created in DB with emailVerified = false
        User user = userRepository.findByEmail("signup_test@example.com").orElse(null);
        assertNotNull(user);
        assertEquals("Signup Test User", user.getFullName());
        assertFalse(user.getEmailVerified());

        // Verify verification token exists
        long tokenCount = verificationTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .count();
        assertEquals(1, tokenCount);
    }

    @Test
    public void testSignupEmailAlreadyExists() throws Exception {
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword("pwd");
        existingUser.setFullName("Existing");
        existingUser.setRole(RoleType.PARENT);
        existingUser.setEmailVerified(true);
        existingUser.setIsActive(true);
        userRepository.save(existingUser);

        SignupRequest request = new SignupRequest(
                "existing@example.com",
                "password123",
                "Signup Test User",
                "0987654321",
                RoleType.TUTOR
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Email này đã được sử dụng")));
    }

    @Test
    public void testVerifyEmailSuccess() throws Exception {
        User user = new User();
        user.setEmail("verify@example.com");
        user.setPassword("pwd");
        user.setFullName("Verify User");
        user.setRole(RoleType.PARENT);
        user.setEmailVerified(false);
        user.setIsActive(true);
        userRepository.save(user);

        VerificationToken token = new VerificationToken("valid-token", user, LocalDateTime.now().plusHours(1));
        verificationTokenRepository.save(token);

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Xác thực địa chỉ email thành công")));

        // Verify User state and Token state
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updatedUser.getEmailVerified());

        VerificationToken updatedToken = verificationTokenRepository.findById(token.getId()).orElseThrow();
        assertTrue(updatedToken.getUsed());
    }

    @Test
    public void testVerifyEmailExpired() throws Exception {
        User user = new User();
        user.setEmail("expired@example.com");
        user.setPassword("pwd");
        user.setFullName("Expired User");
        user.setRole(RoleType.PARENT);
        user.setEmailVerified(false);
        user.setIsActive(true);
        userRepository.save(user);

        VerificationToken token = new VerificationToken("expired-token", user, LocalDateTime.now().minusHours(1));
        verificationTokenRepository.save(token);

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "expired-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Mã xác thực đã hết hạn")));
    }

    @Test
    public void testLoginSuccess() throws Exception {
        User user = new User();
        user.setEmail("login_success@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFullName("Login Success User");
        user.setRole(RoleType.PARENT);
        user.setEmailVerified(true);
        user.setIsActive(true);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("login_success@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("login_success@example.com")))
                .andExpect(jsonPath("$.data.user.role", is("PARENT")));
    }

    @Test
    public void testLoginUnverified() throws Exception {
        User user = new User();
        user.setEmail("login_unverified@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFullName("Login Unverified User");
        user.setRole(RoleType.PARENT);
        user.setEmailVerified(false);
        user.setIsActive(true);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("login_unverified@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Tài khoản chưa được xác thực email")));
    }
}
