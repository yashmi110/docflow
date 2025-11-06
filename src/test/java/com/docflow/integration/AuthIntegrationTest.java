package com.docflow.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Integration tests for Authentication endpoints.
 * Tests signup, login, and JWT token generation.
 */
@DisplayName("Authentication Integration Tests")
public class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should successfully signup a new user")
    void testSignup() throws Exception {
        String signupRequest = """
                {
                    "name": "Test User",
                    "email": "testuser@example.com",
                    "password": "Test@123"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @DisplayName("Should reject signup with duplicate email")
    void testSignupDuplicateEmail() throws Exception {
        // First signup
        String signupRequest = """
                {
                    "name": "First User",
                    "email": "duplicate@example.com",
                    "password": "Test@123"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupRequest))
                .andExpect(status().isCreated());

        // Second signup with same email
        String duplicateRequest = """
                {
                    "name": "Second User",
                    "email": "duplicate@example.com",
                    "password": "Test@456"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("Should reject signup with invalid email")
    void testSignupInvalidEmail() throws Exception {
        String signupRequest = """
                {
                    "name": "Test User",
                    "email": "invalid-email",
                    "password": "Test@123"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject signup with weak password")
    void testSignupWeakPassword() throws Exception {
        String signupRequest = """
                {
                    "name": "Test User",
                    "email": "test@example.com",
                    "password": "weak"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() throws Exception {
        // First create a user
        String signupRequest = """
                {
                    "name": "Login Test User",
                    "email": "logintest@example.com",
                    "password": "Login@123"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupRequest));

        // Now login
        String loginRequest = """
                {
                    "email": "logintest@example.com",
                    "password": "Login@123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("logintest@example.com"));
    }

    @Test
    @DisplayName("Should reject login with wrong password")
    void testLoginWrongPassword() throws Exception {
        // Create user
        String signupRequest = """
                {
                    "name": "Wrong Pass User",
                    "email": "wrongpass@example.com",
                    "password": "Correct@123"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupRequest));

        // Try login with wrong password
        String loginRequest = """
                {
                    "email": "wrongpass@example.com",
                    "password": "Wrong@123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject login with non-existent email")
    void testLoginNonExistentUser() throws Exception {
        String loginRequest = """
                {
                    "email": "nonexistent@example.com",
                    "password": "Test@123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should login with seed data user (admin)")
    void testLoginSeedDataUser() throws Exception {
        // admin@docflow.com with password Password@123 is created by V8 migration
        String loginRequest = """
                {
                    "email": "admin@docflow.com",
                    "password": "Password@123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("admin@docflow.com"))
                .andExpect(jsonPath("$.name").value("Admin User"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }
}
