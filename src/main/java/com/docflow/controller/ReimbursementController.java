package com.docflow.controller;

import com.docflow.domain.entity.User;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.dto.audit.StatusTransitionRequest;
import com.docflow.dto.invoice.PaymentRequest;
import com.docflow.dto.reimbursement.ReimbursementRequest;
import com.docflow.dto.reimbursement.ReimbursementResponse;
import com.docflow.security.SecurityUtils;
import com.docflow.service.ReimbursementService;
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
@RequestMapping("/api/reimbursements")
@RequiredArgsConstructor
public class ReimbursementController {

    private final ReimbursementService reimbursementService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<ReimbursementResponse> createReimbursement(
            @Valid @RequestBody ReimbursementRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        ReimbursementResponse response = reimbursementService.createReimbursement(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReimbursementResponse> getReimbursement(@PathVariable Long id) {
        ReimbursementResponse response = reimbursementService.getReimbursementById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReimbursementResponse>> getReimbursements(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReimbursementResponse> response = reimbursementService.getReimbursements(status, employeeId, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<ReimbursementResponse> approveReimbursement(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        String note = request != null ? request.getNote() : null;
        ReimbursementResponse response = reimbursementService.approveReimbursement(id, currentUser, note);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<ReimbursementResponse> payReimbursement(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        ReimbursementResponse response = reimbursementService.payReimbursement(id, request, currentUser);
        return ResponseEntity.ok(response);
    }
}
