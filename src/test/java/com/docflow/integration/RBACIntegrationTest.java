package com.docflow.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Integration tests for Role-Based Access Control (RBAC).
 * Tests authorization rules and 403 Forbidden responses.
 */
@DisplayName("RBAC Integration Tests")
public class RBACIntegrationTest extends BaseIntegrationTest {

    private String adminToken;
    private String financeToken;
    private String managerToken;
    private String employeeToken;

    @BeforeEach
    @Override
    void setUp() throws Exception {
        // Login as different users
        adminToken = loginAs("admin@docflow.com", "Password@123");
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
    @DisplayName("Should allow FINANCE to create incoming invoice")
    void testFinanceCanCreateInvoiceIn() throws Exception {
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-RBAC-001",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 1000.00,
                    "tax": 100.00,
                    "total": 1100.00
                }
                """;

        mockMvc.perform(post("/api/invoices/in")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should reject EMPLOYEE creating incoming invoice (403)")
    void testEmployeeCannotCreateInvoiceIn() throws Exception {
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-FORBIDDEN-001",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 1000.00,
                    "tax": 100.00,
                    "total": 1100.00
                }
                """;

        mockMvc.perform(post("/api/invoices/in")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject MANAGER creating incoming invoice (403)")
    void testManagerCannotCreateInvoiceIn() throws Exception {
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-MANAGER-001",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 1000.00,
                    "tax": 100.00,
                    "total": 1100.00
                }
                """;

        mockMvc.perform(post("/api/invoices/in")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow ADMIN to override and create invoice")
    void testAdminCanCreateInvoice() throws Exception {
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-ADMIN-001",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 1000.00,
                    "tax": 100.00,
                    "total": 1100.00
                }
                """;

        mockMvc.perform(post("/api/invoices/in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject MANAGER approving outgoing invoice (403) - FINANCE only")
    void testManagerCannotApproveInvoiceOut() throws Exception {
        // Finance creates outgoing invoice
        String createRequest = """
                {
                    "clientId": 1,
                    "invoiceNo": "OUT-RBAC-001",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 5000.00,
                    "tax": 500.00,
                    "total": 5500.00
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/invoices/out")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode invoice = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long invoiceId = invoice.get("id").asLong();

        // Submit
        mockMvc.perform(post("/api/invoices/out/" + invoiceId + "/submit")
                .header("Authorization", "Bearer " + financeToken));

        // Manager tries to approve (should fail - only FINANCE can approve outgoing invoices)
        mockMvc.perform(post("/api/invoices/out/" + invoiceId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow FINANCE to approve outgoing invoice")
    void testFinanceCanApproveInvoiceOut() throws Exception {
        // Create and submit
        String createRequest = """
                {
                    "clientId": 1,
                    "invoiceNo": "OUT-FINANCE-001",
                    "invoiceDate": "2024-11-15",
                    "dueDate": "2024-12-15",
                    "currency": "USD",
                    "subtotal": 5000.00,
                    "tax": 500.00,
                    "total": 5500.00
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/invoices/out")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode invoice = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long invoiceId = invoice.get("id").asLong();

        mockMvc.perform(post("/api/invoices/out/" + invoiceId + "/submit")
                .header("Authorization", "Bearer " + financeToken));

        // Finance approves
        mockMvc.perform(post("/api/invoices/out/" + invoiceId + "/approve")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Should reject wrong manager approving expense claim (403)")
    void testWrongManagerCannotApproveClaim() throws Exception {
        // employee1 reports to manager1
        // Create claim as employee1
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

        mockMvc.perform(post("/api/claims/" + claimId + "/submit")
                .header("Authorization", "Bearer " + employeeToken));

        // Finance tries to approve (not the employee's manager)
        mockMvc.perform(post("/api/claims/" + claimId + "/approve")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow correct manager to approve expense claim")
    void testCorrectManagerCanApproveClaim() throws Exception {
        // employee1 reports to manager1
        String createRequest = """
                {
                    "claimDate": "2024-11-15",
                    "currency": "USD",
                    "items": [
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
                .andReturn();

        JsonNode claim = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long claimId = claim.get("id").asLong();

        mockMvc.perform(post("/api/claims/" + claimId + "/submit")
                .header("Authorization", "Bearer " + employeeToken));

        // manager1 approves (correct manager)
        mockMvc.perform(post("/api/claims/" + claimId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Should allow ADMIN to override manager hierarchy")
    void testAdminCanOverrideManagerHierarchy() throws Exception {
        // Create claim as employee1
        String createRequest = """
                {
                    "claimDate": "2024-11-15",
                    "currency": "USD",
                    "items": [
                        {
                            "category": "Lodging",
                            "amount": 200.00,
                            "note": "Hotel"
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

        // Admin approves (override)
        mockMvc.perform(post("/api/claims/" + claimId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Should reject unauthenticated request (401)")
    void testUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/invoices/in"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject request with invalid token (401)")
    void testInvalidToken() throws Exception {
        mockMvc.perform(get("/api/invoices/in")
                        .header("Authorization", "Bearer invalid-token-here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject EMPLOYEE viewing all invoices (403)")
    void testEmployeeCannotViewAllInvoices() throws Exception {
        mockMvc.perform(get("/api/invoices/in")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow FINANCE to view all invoices")
    void testFinanceCanViewAllInvoices() throws Exception {
        mockMvc.perform(get("/api/invoices/in")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
