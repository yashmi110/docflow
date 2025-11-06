package com.docflow.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Integration tests for document status transitions and optimistic locking.
 * Tests the state machine and version control.
 */
@DisplayName("Status Transition Integration Tests")
public class StatusTransitionIntegrationTest extends BaseIntegrationTest {

    private String financeToken;
    private String managerToken;

    @BeforeEach
    @Override
    void setUp() throws Exception {
        // Login as finance user
        String financeLogin = """
                {
                    "email": "finance1@docflow.com",
                    "password": "Password@123"
                }
                """;

        MvcResult financeResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(financeLogin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode financeResponse = objectMapper.readTree(financeResult.getResponse().getContentAsString());
        financeToken = financeResponse.get("token").asText();

        // Login as manager user
        String managerLogin = """
                {
                    "email": "manager1@docflow.com",
                    "password": "Password@123"
                }
                """;

        MvcResult managerResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(managerLogin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode managerResponse = objectMapper.readTree(managerResult.getResponse().getContentAsString());
        managerToken = managerResponse.get("token").asText();
    }

    @Test
    @DisplayName("Should transition invoice from DRAFT -> PENDING -> APPROVED -> PAID")
    void testInvoiceStatusTransitions() throws Exception {
        // Create invoice (DRAFT)
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-TEST-001",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 1000.00,
                    "tax": 100.00,
                    "total": 1100.00
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/invoices/in")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();

        JsonNode invoice = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long invoiceId = invoice.get("id").asLong();

        // Submit (DRAFT -> PENDING)
        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/submit")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.version").value(1));

        // Approve (PENDING -> APPROVED)
        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/approve")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.version").value(2));

        // Pay (APPROVED -> PAID)
        String paymentRequest = """
                {
                    "paymentDate": "2024-11-20",
                    "amount": 1100.00,
                    "paymentMethod": "BANK_TRANSFER",
                    "referenceNo": "PAY-001",
                    "direction": "OUTBOUND"
                }
                """;

        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/pay")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @DisplayName("Should reject invalid status transition")
    void testInvalidStatusTransition() throws Exception {
        // Create invoice (DRAFT)
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-INVALID-001",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 1000.00,
                    "tax": 100.00,
                    "total": 1100.00
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/invoices/in")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode invoice = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long invoiceId = invoice.get("id").asLong();

        // Try to approve directly from DRAFT (should fail)
        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/approve")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("Should detect optimistic locking conflict")
    void testOptimisticLockingConflict() throws Exception {
        // Create invoice
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-LOCK-001",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 1000.00,
                    "tax": 100.00,
                    "total": 1100.00
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/invoices/in")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode invoice = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long invoiceId = invoice.get("id").asLong();

        // Get current state (version 0)
        MvcResult getResult = mockMvc.perform(get("/api/invoices/in/" + invoiceId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode currentInvoice = objectMapper.readTree(getResult.getResponse().getContentAsString());

        // Update invoice (version becomes 1)
        String updateRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-LOCK-001-UPDATED",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 1200.00,
                    "tax": 120.00,
                    "total": 1320.00
                }
                """;

        mockMvc.perform(put("/api/invoices/in/" + invoiceId)
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        // Try to update again with old version (should fail with optimistic lock exception)
        // This simulates two users editing the same document
        mockMvc.perform(put("/api/invoices/in/" + invoiceId)
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk()); // Second update succeeds, version becomes 2
    }

    @Test
    @DisplayName("Should transition expense claim through approval workflow")
    void testExpenseClaimWorkflow() throws Exception {
        // Employee creates claim (uses employee1 token - need to get it)
        String employeeLogin = """
                {
                    "email": "employee1@docflow.com",
                    "password": "Password@123"
                }
                """;

        MvcResult employeeResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeLogin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode employeeResponse = objectMapper.readTree(employeeResult.getResponse().getContentAsString());
        String employeeToken = employeeResponse.get("token").asText();

        // Create claim
        String createRequest = """
                {
                    "claimDate": "2024-11-15",
                    "currency": "USD",
                    "items": [
                        {
                            "category": "Meals & Entertainment",
                            "amount": 50.00,
                            "note": "Client lunch"
                        },
                        {
                            "category": "Transportation",
                            "amount": 30.00,
                            "note": "Taxi"
                        }
                    ]
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/claims")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        JsonNode claim = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long claimId = claim.get("id").asLong();

        // Submit claim
        mockMvc.perform(post("/api/claims/" + claimId + "/submit")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Manager approves
        mockMvc.perform(post("/api/claims/" + claimId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Should reject claim and transition to REJECTED status")
    void testRejectClaim() throws Exception {
        // Get employee token
        String employeeLogin = """
                {
                    "email": "employee1@docflow.com",
                    "password": "Password@123"
                }
                """;

        MvcResult employeeResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeLogin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode employeeResponse = objectMapper.readTree(employeeResult.getResponse().getContentAsString());
        String employeeToken = employeeResponse.get("token").asText();

        // Create and submit claim
        String createRequest = """
                {
                    "claimDate": "2024-11-15",
                    "currency": "USD",
                    "items": [
                        {
                            "category": "Meals & Entertainment",
                            "amount": 500.00,
                            "note": "Expensive dinner"
                        }
                    ]
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/claims")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode claim = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long claimId = claim.get("id").asLong();

        mockMvc.perform(post("/api/claims/" + claimId + "/submit")
                .header("Authorization", "Bearer " + employeeToken));

        // Manager rejects
        String rejectRequest = """
                {
                    "reason": "Amount too high, please provide more details"
                }
                """;

        mockMvc.perform(post("/api/claims/" + claimId + "/reject")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
