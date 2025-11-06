package com.docflow.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Integration tests for Audit Log functionality.
 * Tests that audit logs are created for all state transitions.
 */
@DisplayName("Audit Log Integration Tests")
public class AuditLogIntegrationTest extends BaseIntegrationTest {

    private String financeToken;
    private String managerToken;
    private String employeeToken;

    @BeforeEach
    @Override
    void setUp() throws Exception {
        financeToken = loginAs("finance1@docflow.com", "Password@123");
        managerToken = loginAs("manager1@docflow.com", "Password@123");
        employeeToken = loginAs("employee1@docflow.com", "Password@123");
    }

    private String loginAs(String email, String password) throws Exception {
        String loginRequest = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    @Test
    @DisplayName("Should create audit log when invoice is created")
    void testAuditLogOnCreate() throws Exception {
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-AUDIT-001",
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

        // Check audit logs
        mockMvc.perform(get("/api/audit-logs/document/" + invoiceId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[0].toStatus").value("DRAFT"))
                .andExpect(jsonPath("$[0].user.email").value("finance1@docflow.com"));
    }

    @Test
    @DisplayName("Should create audit log for each status transition")
    void testAuditLogForStatusTransitions() throws Exception {
        // Create invoice
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-AUDIT-002",
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

        // Submit
        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/submit")
                .header("Authorization", "Bearer " + financeToken));

        // Approve
        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/approve")
                .header("Authorization", "Bearer " + financeToken));

        // Check audit logs - should have 3 entries (CREATED, SUBMITTED, APPROVED)
        mockMvc.perform(get("/api/audit-logs/document/" + invoiceId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[0].toStatus").value("DRAFT"))
                .andExpect(jsonPath("$[1].action").value("SUBMITTED"))
                .andExpect(jsonPath("$[1].fromStatus").value("DRAFT"))
                .andExpect(jsonPath("$[1].toStatus").value("PENDING"))
                .andExpect(jsonPath("$[2].action").value("APPROVED"))
                .andExpect(jsonPath("$[2].fromStatus").value("PENDING"))
                .andExpect(jsonPath("$[2].toStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("Should record user who performed each action in audit log")
    void testAuditLogRecordsUser() throws Exception {
        // Employee creates claim
        String createRequest = """
                {
                    "claimDate": "2024-11-15",
                    "currency": "USD",
                    "items": [
                        {
                            "category": "Meals & Entertainment",
                            "amount": 50.00,
                            "note": "Client lunch"
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

        // Employee submits
        mockMvc.perform(post("/api/claims/" + claimId + "/submit")
                .header("Authorization", "Bearer " + employeeToken));

        // Manager approves
        mockMvc.perform(post("/api/claims/" + claimId + "/approve")
                .header("Authorization", "Bearer " + managerToken));

        // Check audit logs
        mockMvc.perform(get("/api/audit-logs/document/" + claimId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user.email").value("employee1@docflow.com"))
                .andExpect(jsonPath("$[1].user.email").value("employee1@docflow.com"))
                .andExpect(jsonPath("$[2].user.email").value("manager1@docflow.com"));
    }

    @Test
    @DisplayName("Should create audit log when invoice is rejected")
    void testAuditLogOnRejection() throws Exception {
        // Create and submit invoice
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-REJECT-001",
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

        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/submit")
                .header("Authorization", "Bearer " + financeToken));

        // Reject with reason
        String rejectRequest = """
                {
                    "reason": "Invoice number does not match PO"
                }
                """;

        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/reject")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectRequest))
                .andExpect(status().isOk());

        // Check audit log includes rejection reason
        mockMvc.perform(get("/api/audit-logs/document/" + invoiceId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].action").value("REJECTED"))
                .andExpect(jsonPath("$[2].fromStatus").value("PENDING"))
                .andExpect(jsonPath("$[2].toStatus").value("REJECTED"))
                .andExpect(jsonPath("$[2].note").value("Invoice number does not match PO"));
    }

    @Test
    @DisplayName("Should create audit log when payment is recorded")
    void testAuditLogOnPayment() throws Exception {
        // Create, submit, approve invoice
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-PAY-001",
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

        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/submit")
                .header("Authorization", "Bearer " + financeToken));

        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/approve")
                .header("Authorization", "Bearer " + financeToken));

        // Pay
        String paymentRequest = """
                {
                    "paymentDate": "2024-11-20",
                    "amount": 1100.00,
                    "paymentMethod": "BANK_TRANSFER",
                    "referenceNo": "PAY-AUDIT-001",
                    "direction": "OUTBOUND"
                }
                """;

        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/pay")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest))
                .andExpect(status().isOk());

        // Check audit log
        mockMvc.perform(get("/api/audit-logs/document/" + invoiceId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[3].action").value("PAID"))
                .andExpect(jsonPath("$[3].fromStatus").value("APPROVED"))
                .andExpect(jsonPath("$[3].toStatus").value("PAID"))
                .andExpect(jsonPath("$[3].note").value("Payment recorded: PAY-AUDIT-001"));
    }

    @Test
    @DisplayName("Should maintain audit log chronological order")
    void testAuditLogChronologicalOrder() throws Exception {
        // Create invoice
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-CHRONO-001",
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

        // Perform multiple transitions
        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/submit")
                .header("Authorization", "Bearer " + financeToken));

        mockMvc.perform(post("/api/invoices/in/" + invoiceId + "/approve")
                .header("Authorization", "Bearer " + financeToken));

        // Get audit logs
        MvcResult auditResult = mockMvc.perform(get("/api/audit-logs/document/" + invoiceId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode auditLogs = objectMapper.readTree(auditResult.getResponse().getContentAsString());

        // Verify chronological order (oldest first)
        String firstTimestamp = auditLogs.get(0).get("createdAt").asText();
        String secondTimestamp = auditLogs.get(1).get("createdAt").asText();
        String thirdTimestamp = auditLogs.get(2).get("createdAt").asText();

        // Timestamps should be in ascending order
        assert firstTimestamp.compareTo(secondTimestamp) <= 0;
        assert secondTimestamp.compareTo(thirdTimestamp) <= 0;
    }

    @Test
    @DisplayName("Should include all required fields in audit log")
    void testAuditLogRequiredFields() throws Exception {
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-FIELDS-001",
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

        // Check audit log has all required fields
        mockMvc.perform(get("/api/audit-logs/document/" + invoiceId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].docId").value(invoiceId))
                .andExpect(jsonPath("$[0].user").exists())
                .andExpect(jsonPath("$[0].user.id").exists())
                .andExpect(jsonPath("$[0].user.email").exists())
                .andExpect(jsonPath("$[0].action").exists())
                .andExpect(jsonPath("$[0].toStatus").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
    }
}
