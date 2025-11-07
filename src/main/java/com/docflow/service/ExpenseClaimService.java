package com.docflow.service;

import com.docflow.domain.entity.Employee;
import com.docflow.domain.entity.ExpenseClaim;
import com.docflow.domain.entity.ExpenseItem;
import com.docflow.domain.entity.User;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.domain.enums.RoleName;
import com.docflow.dto.claim.ExpenseClaimRequest;
import com.docflow.dto.claim.ExpenseClaimResponse;
import com.docflow.exception.ResourceNotFoundException;
import com.docflow.exception.UnauthorizedActionException;
import com.docflow.mapper.ExpenseClaimMapper;
import com.docflow.repository.EmployeeRepository;
import com.docflow.repository.ExpenseClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseClaimService {

    private final ExpenseClaimRepository expenseClaimRepository;
    private final EmployeeRepository employeeRepository;
    private final ExpenseClaimMapper expenseClaimMapper;
    private final DocumentStatusMachine statusMachine;
    private final ApprovalService approvalService;

    @Transactional
    public ExpenseClaimResponse createClaim(ExpenseClaimRequest request, User currentUser) {
        // Get employee for current user
        Employee employee = employeeRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("User is not associated with an employee record"));

        // Create claim in DRAFT status
        ExpenseClaim claim = expenseClaimMapper.toEntity(request, employee, currentUser);
        
        // Create and add items
        List<ExpenseItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        
        for (var itemRequest : request.getItems()) {
            ExpenseItem item = expenseClaimMapper.toItemEntity(itemRequest);
            item.setClaim(claim);
            items.add(item);
            total = total.add(itemRequest.getAmount());
        }
        
        claim.setItems(items);
        claim.setTotal(total);

        claim = expenseClaimRepository.save(claim);

        log.info("Created expense claim {} by user {} with {} items, total: {}", 
                claim.getId(), currentUser.getEmail(), items.size(), total);

        return toResponseWithItems(claim);
    }

    @Transactional(readOnly = true)
    public ExpenseClaimResponse getClaimById(Long id) {
        ExpenseClaim claim = expenseClaimRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense claim", id));

        return toResponseWithItems(claim);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseClaimResponse> getClaims(DocumentStatus status, Long employeeId, Pageable pageable) {
        Page<ExpenseClaim> claims = expenseClaimRepository.findByFilters(status, employeeId, pageable);
        return claims.map(this::toResponseWithItems);
    }

    @Transactional
    public ExpenseClaimResponse submitClaim(Long id, User currentUser, String note) {
        ExpenseClaim claim = expenseClaimRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense claim", id));

        // ADMIN can submit any claim, otherwise only owner can submit
        boolean isAdmin = hasRole(currentUser, RoleName.ADMIN);
        if (!isAdmin && !claim.getOwnerUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("Only the claim creator or ADMIN can submit it");
        }

        // Transition DRAFT -> PENDING
        statusMachine.submit(claim, currentUser, note);
        claim = expenseClaimRepository.save(claim);

        log.info("Submitted expense claim {} by user {}", id, currentUser.getEmail());

        return toResponseWithItems(claim);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ExpenseClaimResponse approveClaim(Long id, User currentUser, String note) {
        ExpenseClaim claim = expenseClaimRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense claim", id));

        // Validate approver authorization
        approvalService.validateExpenseClaimApprover(claim, currentUser);

        // Transition PENDING -> APPROVED
        statusMachine.approve(claim, currentUser, note);
        claim = expenseClaimRepository.save(claim);

        log.info("Approved expense claim {} by user {}", id, currentUser.getEmail());

        return toResponseWithItems(claim);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ExpenseClaimResponse rejectClaim(Long id, User currentUser, String note) {
        ExpenseClaim claim = expenseClaimRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense claim", id));

        // Validate rejecter authorization
        approvalService.validateExpenseClaimRejecter(claim, currentUser);

        // Transition PENDING -> REJECTED
        statusMachine.reject(claim, currentUser, note);
        claim = expenseClaimRepository.save(claim);

        log.info("Rejected expense claim {} by user {}", id, currentUser.getEmail());

        return toResponseWithItems(claim);
    }

    private ExpenseClaimResponse toResponseWithItems(ExpenseClaim claim) {
        ExpenseClaimResponse response = expenseClaimMapper.toResponse(claim);
        if (claim.getItems() != null) {
            response.setItems(expenseClaimMapper.toItemResponseList(claim.getItems()));
        }
        return response;
    }

    /**
     * Check if user has a specific role.
     */
    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == roleName);
    }
}
