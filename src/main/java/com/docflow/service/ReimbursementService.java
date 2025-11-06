package com.docflow.service;

import com.docflow.domain.entity.ExpenseClaim;
import com.docflow.domain.entity.Payment;
import com.docflow.domain.entity.Reimbursement;
import com.docflow.domain.entity.User;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.domain.enums.PaymentDirection;
import com.docflow.dto.invoice.PaymentRequest;
import com.docflow.dto.reimbursement.ReimbursementRequest;
import com.docflow.dto.reimbursement.ReimbursementResponse;
import com.docflow.exception.ResourceNotFoundException;
import com.docflow.mapper.ReimbursementMapper;
import com.docflow.repository.ExpenseClaimRepository;
import com.docflow.repository.PaymentRepository;
import com.docflow.repository.ReimbursementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReimbursementService {

    private final ReimbursementRepository reimbursementRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final PaymentRepository paymentRepository;
    private final ReimbursementMapper reimbursementMapper;
    private final DocumentStatusMachine statusMachine;

    @Value("${reimbursement.tolerance:0.01}")
    private BigDecimal tolerance;

    @Transactional
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ReimbursementResponse createReimbursement(ReimbursementRequest request, User currentUser) {
        // Validate expense claim exists and is APPROVED
        ExpenseClaim claim = expenseClaimRepository.findByIdWithDetails(request.getExpenseClaimId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense claim", request.getExpenseClaimId()));

        if (claim.getStatus() != DocumentStatus.APPROVED) {
            throw new IllegalStateException("Can only create reimbursement for APPROVED expense claims");
        }

        // Check for duplicate reimbursement
        if (reimbursementRepository.existsActiveReimbursementForClaim(request.getExpenseClaimId())) {
            throw new IllegalStateException("An active reimbursement already exists for this expense claim");
        }

        // Validate total matches claim total (with tolerance)
        BigDecimal claimTotal = claim.getTotal();
        BigDecimal requestTotal = request.getTotal();
        BigDecimal difference = claimTotal.subtract(requestTotal).abs();

        if (difference.compareTo(tolerance) > 0) {
            throw new IllegalArgumentException(
                    String.format("Reimbursement total %.2f does not match approved claim total %.2f (tolerance: %.2f)",
                            requestTotal, claimTotal, tolerance)
            );
        }

        // Validate currency matches
        if (!claim.getCurrency().equals(request.getCurrency())) {
            throw new IllegalArgumentException(
                    String.format("Reimbursement currency %s does not match claim currency %s",
                            request.getCurrency(), claim.getCurrency())
            );
        }

        // Create reimbursement in PENDING status
        Reimbursement reimbursement = reimbursementMapper.toEntity(request, claim.getEmployee(), currentUser);

        reimbursement = reimbursementRepository.save(reimbursement);

        log.info("Created reimbursement {} for claim {} by user {}, total: {}",
                reimbursement.getId(), claim.getId(), currentUser.getEmail(), requestTotal);

        ReimbursementResponse response = reimbursementMapper.toResponse(reimbursement);
        response.setExpenseClaimId(claim.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public ReimbursementResponse getReimbursementById(Long id) {
        Reimbursement reimbursement = reimbursementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reimbursement", id));

        return reimbursementMapper.toResponse(reimbursement);
    }

    @Transactional(readOnly = true)
    public Page<ReimbursementResponse> getReimbursements(DocumentStatus status, Long employeeId, Pageable pageable) {
        Page<Reimbursement> reimbursements = reimbursementRepository.findByFilters(status, employeeId, pageable);
        return reimbursements.map(reimbursementMapper::toResponse);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ReimbursementResponse approveReimbursement(Long id, User currentUser, String note) {
        Reimbursement reimbursement = reimbursementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reimbursement", id));

        // Transition PENDING -> APPROVED
        statusMachine.approve(reimbursement, currentUser, note);
        reimbursement = reimbursementRepository.save(reimbursement);

        log.info("Approved reimbursement {} by user {}", id, currentUser.getEmail());

        return reimbursementMapper.toResponse(reimbursement);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ReimbursementResponse payReimbursement(Long id, PaymentRequest paymentRequest, User currentUser) {
        Reimbursement reimbursement = reimbursementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reimbursement", id));

        // Create payment record (OUTBOUND - we pay employee)
        Payment payment = Payment.builder()
                .document(reimbursement)
                .direction(PaymentDirection.OUTBOUND)
                .method(paymentRequest.getMethod())
                .amount(paymentRequest.getAmount())
                .paidAt(paymentRequest.getPaidAt())
                .reference(paymentRequest.getReference())
                .build();

        paymentRepository.save(payment);

        // Transition APPROVED -> PAID
        statusMachine.markAsPaid(reimbursement, currentUser, paymentRequest.getNote());
        reimbursement = reimbursementRepository.save(reimbursement);

        log.info("Paid reimbursement {} by user {} - amount: {}",
                id, currentUser.getEmail(), paymentRequest.getAmount());

        return reimbursementMapper.toResponse(reimbursement);
    }
}
