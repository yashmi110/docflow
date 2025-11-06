package com.docflow.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Integration tests for file upload functionality.
 * Tests file upload, download, list, and delete operations.
 */
@DisplayName("File Upload Integration Tests")
public class FileUploadIntegrationTest extends BaseIntegrationTest {

    private String financeToken;
    private Long testInvoiceId;

    @BeforeEach
    @Override
    void setUp() throws Exception {
        // Login as finance
        String loginRequest = """
                {
                    "email": "finance1@docflow.com",
                    "password": "Password@123"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        financeToken = loginResponse.get("token").asText();

        // Create a test invoice to attach files to
        String createRequest = """
                {
                    "vendorId": 1,
                    "invoiceNo": "INV-FILE-TEST",
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
        testInvoiceId = invoice.get("id").asLong();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up test storage directory
        Path testStorage = Path.of("./test-storage");
        if (Files.exists(testStorage)) {
            Files.walk(testStorage)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
        }
    }

    @Test
    @DisplayName("Should successfully upload a PDF file")
    void testUploadPdfFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "Test PDF content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fileName").value("invoice.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.fileSize").value(16))
                .andExpect(jsonPath("$.uploadedBy.email").value("finance1@docflow.com"));
    }

    @Test
    @DisplayName("Should successfully upload an image file")
    void testUploadImageFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "Fake JPEG content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("receipt.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    }

    @Test
    @DisplayName("Should reject file with disallowed content type")
    void testRejectDisallowedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/x-msdownload",
                "Executable content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject file exceeding size limit")
    void testRejectOversizedFile() throws Exception {
        // Create a file larger than 10MB (configured limit)
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeContent
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should list all files for a document")
    void testListFiles() throws Exception {
        // Upload two files
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                .file(file1)
                .header("Authorization", "Bearer " + financeToken));

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "JPEG content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                .file(file2)
                .header("Authorization", "Bearer " + financeToken));

        // List files
        mockMvc.perform(get("/api/docs/" + testInvoiceId + "/files")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fileName").exists())
                .andExpect(jsonPath("$[1].fileName").exists());
    }

    @Test
    @DisplayName("Should download uploaded file")
    void testDownloadFile() throws Exception {
        // Upload file
        String fileContent = "Test PDF content for download";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "download-test.pdf",
                "application/pdf",
                fileContent.getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode uploadResponse = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        Long fileId = uploadResponse.get("id").asLong();

        // Download file
        mockMvc.perform(get("/api/docs/" + testInvoiceId + "/files/" + fileId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"download-test.pdf\""))
                .andExpect(content().bytes(fileContent.getBytes()));
    }

    @Test
    @DisplayName("Should delete uploaded file")
    void testDeleteFile() throws Exception {
        // Upload file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "delete-test.pdf",
                "application/pdf",
                "Content to delete".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode uploadResponse = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        Long fileId = uploadResponse.get("id").asLong();

        // Delete file
        mockMvc.perform(delete("/api/docs/" + testInvoiceId + "/files/" + fileId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isNoContent());

        // Verify file is deleted
        mockMvc.perform(get("/api/docs/" + testInvoiceId + "/files/" + fileId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should upload multiple files to same document")
    void testUploadMultipleFiles() throws Exception {
        // Upload 3 files
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "file" + i + ".pdf",
                    "application/pdf",
                    ("Content " + i).getBytes()
            );

            mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                            .file(file)
                            .header("Authorization", "Bearer " + financeToken))
                    .andExpect(status().isOk());
        }

        // Verify all 3 files are listed
        mockMvc.perform(get("/api/docs/" + testInvoiceId + "/files")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Should reject file upload to non-existent document")
    void testUploadToNonExistentDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/99999/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject file upload without authentication")
    void testUploadWithoutAuth() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should upload Excel file")
    void testUploadExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "expenses.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "Excel content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("expenses.xlsx"))
                .andExpect(jsonPath("$.contentType").value("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @DisplayName("Should upload Word document")
    void testUploadWordDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Word content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("contract.docx"));
    }

    @Test
    @DisplayName("Should preserve original filename")
    void testPreserveFilename() throws Exception {
        String originalFilename = "My Invoice 2024-11-15.pdf";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                originalFilename,
                "application/pdf",
                "Content".getBytes()
        );

        mockMvc.perform(multipart("/api/docs/" + testInvoiceId + "/files")
                        .file(file)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value(originalFilename));
    }
}
