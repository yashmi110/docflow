package com.docflow.service;

import com.docflow.domain.entity.*;
import com.docflow.domain.enums.RoleName;
import com.docflow.exception.UnauthorizedActionException;
import com.docflow.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Centralized approval authorization service.
 * Enforces approver assignment rules for different document types.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final EmployeeRepository employeeRepository;

    /**
     * Check if user can approve an expense claim.
     * Rules:
     * - ADMIN can always approve
     * - Manager of the claimant can approve
     * 
     * @param claim The expense claim
     * @param approver The user attempting to approve
     * @throws UnauthorizedActionException if user is not authorized
     */
    public void validateExpenseClaimApprover(ExpenseClaim claim, User approver) {
        // ADMIN can always approve
        if (hasRole(approver, RoleName.ADMIN)) {
            log.debug("User {} authorized as ADMIN to approve claim {}", approver.getEmail(), claim.getId());
            return;
        }

        // Check if approver is the claimant's manager
        Employee claimant = claim.getEmployee();
        if (claimant.getManager() == null) {
            throw new UnauthorizedActionException(
                    "Expense claim cannot be approved: claimant has no assigned manager"
            );
        }

        if (!claimant.getManager().getId().equals(approver.getId())) {
            throw new UnauthorizedActionException(
                    String.format("Only the claimant's manager (%s) or ADMIN can approve this expense claim",
                            claimant.getManager().getEmail())
            );
        }

        log.debug("User {} authorized as manager to approve claim {}", approver.getEmail(), claim.getId());
    }

    /**
     * Check if user can reject an expense claim.
     * Same rules as approval.
     */
    public void validateExpenseClaimRejecter(ExpenseClaim claim, User rejecter) {
        validateExpenseClaimApprover(claim, rejecter);
    }

    /**
     * Check if user can approve an incoming invoice.
     * Rules:
     * - ADMIN can always approve
     * - FINANCE role can approve
     * - MANAGER role can approve
     * - If PO exists and has assigned approver, only that approver (or ADMIN) can approve
     * 
     * @param invoice The incoming invoice
     * @param approver The user attempting to approve
     * @throws UnauthorizedActionException if user is not authorized
     */
    public void validateInvoiceInApprover(InvoiceIn invoice, User approver) {
        // ADMIN can always approve
        if (hasRole(approver, RoleName.ADMIN)) {
            log.debug("User {} authorized as ADMIN to approve invoice {}", approver.getEmail(), invoice.getId());
            return;
        }

        // If PO exists with assigned approver, only that approver can approve
        if (invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getApprover() != null) {
            User poApprover = invoice.getPurchaseOrder().getApprover();
            if (!poApprover.getId().equals(approver.getId())) {
                throw new UnauthorizedActionException(
                        String.format("This invoice is linked to PO %s which requires approval from %s",
                                invoice.getPurchaseOrder().getPoNo(),
                                poApprover.getEmail())
                );
            }
            log.debug("User {} authorized as PO approver to approve invoice {}", approver.getEmail(), invoice.getId());
            return;
        }

        // Otherwise, MANAGER or FINANCE can approve
        if (hasRole(approver, RoleName.MANAGER) || hasRole(approver, RoleName.FINANCE)) {
            log.debug("User {} authorized with role to approve invoice {}", approver.getEmail(), invoice.getId());
            return;
        }

        throw new UnauthorizedActionException(
                "Only MANAGER, FINANCE, or ADMIN roles can approve incoming invoices"
        );
    }

    /**
     * Check if user can reject an incoming invoice.
     * Same rules as approval.
     */
    public void validateInvoiceInRejecter(InvoiceIn invoice, User rejecter) {
        validateInvoiceInApprover(invoice, rejecter);
    }

    /**
     * Check if user can approve an outgoing invoice.
     * Rules:
     * - ADMIN can always approve
     * - FINANCE role can approve
     * 
     * @param invoice The outgoing invoice
     * @param approver The user attempting to approve
     * @throws UnauthorizedActionException if user is not authorized
     */
    public void validateInvoiceOutApprover(InvoiceOut invoice, User approver) {
        // ADMIN can always approve
        if (hasRole(approver, RoleName.ADMIN)) {
            log.debug("User {} authorized as ADMIN to approve invoice {}", approver.getEmail(), invoice.getId());
            return;
        }

        // FINANCE can approve
        if (hasRole(approver, RoleName.FINANCE)) {
            log.debug("User {} authorized as FINANCE to approve invoice {}", approver.getEmail(), invoice.getId());
            return;
        }

        throw new UnauthorizedActionException(
                "Only FINANCE or ADMIN roles can approve outgoing invoices"
        );
    }

    /**
     * Check if user can reject an outgoing invoice.
     * Same rules as approval.
     */
    public void validateInvoiceOutRejecter(InvoiceOut invoice, User rejecter) {
        validateInvoiceOutApprover(invoice, rejecter);
    }

    /**
     * Check if user has a specific role.
     */
    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == roleName);
    }
}
