package com.docflow.controller;

import com.docflow.domain.entity.User;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.dto.audit.StatusTransitionRequest;
import com.docflow.dto.invoice.InvoiceOutRequest;
import com.docflow.dto.invoice.InvoiceOutResponse;
import com.docflow.dto.invoice.PaymentRequest;
import com.docflow.security.SecurityUtils;
import com.docflow.service.InvoiceOutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices/out")
@RequiredArgsConstructor
public class InvoiceOutController {

    private final InvoiceOutService invoiceOutService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceOutResponse> createInvoice(
            @Valid @RequestBody InvoiceOutRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        InvoiceOutResponse response = invoiceOutService.createInvoice(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceOutResponse> getInvoice(@PathVariable Long id) {
        InvoiceOutResponse response = invoiceOutService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<InvoiceOutResponse>> getInvoices(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<InvoiceOutResponse> response = invoiceOutService.getInvoices(status, clientId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceOutResponse> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceOutRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        InvoiceOutResponse response = invoiceOutService.updateInvoice(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceOutResponse> submitInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        String note = request != null ? request.getNote() : null;
        InvoiceOutResponse response = invoiceOutService.submitInvoice(id, currentUser, note);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<InvoiceOutResponse> approveInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        String note = request != null ? request.getNote() : null;
        InvoiceOutResponse response = invoiceOutService.approveInvoice(id, currentUser, note);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<InvoiceOutResponse> rejectInvoice(
            @PathVariable Long id,
            @Valid @RequestBody StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        InvoiceOutResponse response = invoiceOutService.rejectInvoice(id, currentUser, request.getNote());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<InvoiceOutResponse> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        InvoiceOutResponse response = invoiceOutService.recordPayment(id, request, currentUser);
        return ResponseEntity.ok(response);
    }
}
