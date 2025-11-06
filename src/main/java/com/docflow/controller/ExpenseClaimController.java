package com.docflow.controller;

import com.docflow.domain.entity.User;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.dto.audit.StatusTransitionRequest;
import com.docflow.dto.claim.ExpenseClaimRequest;
import com.docflow.dto.claim.ExpenseClaimResponse;
import com.docflow.security.SecurityUtils;
import com.docflow.service.ExpenseClaimService;
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
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ExpenseClaimController {

    private final ExpenseClaimService expenseClaimService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseClaimResponse> createClaim(
            @Valid @RequestBody ExpenseClaimRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        ExpenseClaimResponse response = expenseClaimService.createClaim(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseClaimResponse> getClaim(@PathVariable Long id) {
        ExpenseClaimResponse response = expenseClaimService.getClaimById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ExpenseClaimResponse>> getClaims(
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

        Page<ExpenseClaimResponse> response = expenseClaimService.getClaims(status, employeeId, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseClaimResponse> submitClaim(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        String note = request != null ? request.getNote() : null;
        ExpenseClaimResponse response = expenseClaimService.submitClaim(id, currentUser, note);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ExpenseClaimResponse> approveClaim(
            @PathVariable Long id,
            @RequestBody(required = false) StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        String note = request != null ? request.getNote() : null;
        ExpenseClaimResponse response = expenseClaimService.approveClaim(id, currentUser, note);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ExpenseClaimResponse> rejectClaim(
            @PathVariable Long id,
            @Valid @RequestBody StatusTransitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        ExpenseClaimResponse response = expenseClaimService.rejectClaim(id, currentUser, request.getNote());
        return ResponseEntity.ok(response);
    }
}
