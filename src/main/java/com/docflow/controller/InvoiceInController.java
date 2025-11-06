package com.docflow.controller;

import com.docflow.domain.entity.User;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.dto.audit.StatusTransitionRequest;
import com.docflow.dto.filter.InvoiceFilterCriteria;
import com.docflow.dto.filter.PageResponse;
import com.docflow.dto.invoice.InvoiceInRequest;
import com.docflow.dto.invoice.InvoiceInResponse;
import com.docflow.dto.invoice.PaymentRequest;
import com.docflow.security.SecurityUtils;
import com.docflow.service.InvoiceInService;
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
@RequestMapping("/api/invoices/in")
@RequiredArgsConstructor
public class InvoiceInController {

    private final InvoiceInService invoiceInService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceInResponse> createInvoice(
            @Valid @RequestBody InvoiceInRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        InvoiceInResponse response = invoiceInService.createInvoice(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceInResponse> getInvoice(@PathVariable Long id) {
        InvoiceInResponse response = invoiceInService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<InvoiceInResponse>> getInvoices(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<InvoiceInResponse> response = invoiceInService.getInvoices(status, vendorId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceInResponse> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceInRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        InvoiceInResponse response = invoiceInService.updateInvoice(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceInResponse> submitInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        String note = request != null ? request.getNote() : null;
        InvoiceInResponse response = invoiceInService.submitInvoice(id, currentUser, note);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'FINANCE', 'ADMIN')")
    public ResponseEntity<InvoiceInResponse> approveInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        String note = request != null ? request.getNote() : null;
        InvoiceInResponse response = invoiceInService.approveInvoice(id, currentUser, note);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'FINANCE', 'ADMIN')")
    public ResponseEntity<InvoiceInResponse> rejectInvoice(
            @PathVariable Long id,
            @Valid @RequestBody StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        InvoiceInResponse response = invoiceInService.rejectInvoice(id, currentUser, request.getNote());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<InvoiceInResponse> payInvoice(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        InvoiceInResponse response = invoiceInService.payInvoice(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<InvoiceInResponse>> filterInvoices(
            @RequestBody InvoiceFilterCriteria criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<InvoiceInResponse> response = invoiceInService.filterInvoices(criteria, pageable);
        return ResponseEntity.ok(response);
    }
}
