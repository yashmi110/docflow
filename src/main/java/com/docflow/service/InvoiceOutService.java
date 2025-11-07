package com.docflow.service;

import com.docflow.domain.entity.Client;
import com.docflow.domain.entity.InvoiceOut;
import com.docflow.domain.entity.Payment;
import com.docflow.domain.entity.User;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.domain.enums.PaymentDirection;
import com.docflow.domain.enums.RoleName;
import com.docflow.dto.invoice.InvoiceOutRequest;
import com.docflow.dto.invoice.InvoiceOutResponse;
import com.docflow.dto.invoice.PaymentRequest;
import com.docflow.exception.ResourceNotFoundException;
import com.docflow.exception.UnauthorizedActionException;
import com.docflow.mapper.InvoiceOutMapper;
import com.docflow.repository.ClientRepository;
import com.docflow.repository.InvoiceOutRepository;
import com.docflow.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceOutService {

    private final InvoiceOutRepository invoiceOutRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceOutMapper invoiceOutMapper;
    private final DocumentStatusMachine statusMachine;
    private final ApprovalService approvalService;

    @Transactional
    public InvoiceOutResponse createInvoice(InvoiceOutRequest request, User currentUser) {
        // Validate client exists
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", request.getClientId()));

        // Check for duplicate invoice number
        if (invoiceOutRepository.existsByInvoiceNo(request.getInvoiceNo())) {
            throw new IllegalArgumentException("Invoice number already exists: " + request.getInvoiceNo());
        }

        // Create invoice in DRAFT status
        InvoiceOut invoice = invoiceOutMapper.toEntity(request, client, currentUser);
        
        // Set defaults
        if (invoice.getTax() == null) {
            invoice.setTax(java.math.BigDecimal.ZERO);
        }

        invoice = invoiceOutRepository.save(invoice);

        log.info("Created outgoing invoice {} by user {}", invoice.getId(), currentUser.getEmail());

        return invoiceOutMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceOutResponse getInvoiceById(Long id) {
        InvoiceOut invoice = invoiceOutRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        return invoiceOutMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceOutResponse> getInvoices(DocumentStatus status, Long clientId, Pageable pageable) {
        Page<InvoiceOut> invoices = invoiceOutRepository.findByFilters(status, clientId, pageable);
        return invoices.map(invoiceOutMapper::toResponse);
    }

    @Transactional
    public InvoiceOutResponse updateInvoice(Long id, InvoiceOutRequest request, User currentUser) {
        InvoiceOut invoice = invoiceOutRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // ADMIN can edit any invoice in any status, otherwise only DRAFT invoices can be edited by owner
        boolean isAdmin = hasRole(currentUser, RoleName.ADMIN);
        if (!isAdmin) {
            if (invoice.getStatus() != DocumentStatus.DRAFT) {
                throw new IllegalStateException("Only DRAFT invoices can be edited");
            }
            if (!invoice.getOwnerUser().getId().equals(currentUser.getId())) {
                throw new UnauthorizedActionException("Only the invoice creator or ADMIN can edit it");
            }
        }

        // Validate client
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", request.getClientId()));

        // Update invoice
        invoiceOutMapper.updateEntityFromRequest(request, client, invoice);
        
        if (invoice.getTax() == null) {
            invoice.setTax(java.math.BigDecimal.ZERO);
        }

        invoice = invoiceOutRepository.save(invoice);

        log.info("Updated outgoing invoice {} by user {}", id, currentUser.getEmail());

        return invoiceOutMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceOutResponse submitInvoice(Long id, User currentUser, String note) {
        InvoiceOut invoice = invoiceOutRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // ADMIN can submit any invoice, otherwise only owner can submit
        boolean isAdmin = hasRole(currentUser, RoleName.ADMIN);
        if (!isAdmin && !invoice.getOwnerUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("Only the invoice creator or ADMIN can submit it");
        }

        // Transition DRAFT -> PENDING
        statusMachine.submit(invoice, currentUser, note);
        invoice = invoiceOutRepository.save(invoice);

        log.info("Submitted outgoing invoice {} by user {}", id, currentUser.getEmail());

        return invoiceOutMapper.toResponse(invoice);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public InvoiceOutResponse approveInvoice(Long id, User currentUser, String note) {
        InvoiceOut invoice = invoiceOutRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // Validate approver authorization (FINANCE or ADMIN only)
        approvalService.validateInvoiceOutApprover(invoice, currentUser);

        // Transition PENDING -> APPROVED
        statusMachine.approve(invoice, currentUser, note);
        invoice = invoiceOutRepository.save(invoice);

        log.info("Approved outgoing invoice {} by user {}", id, currentUser.getEmail());

        return invoiceOutMapper.toResponse(invoice);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public InvoiceOutResponse rejectInvoice(Long id, User currentUser, String note) {
        InvoiceOut invoice = invoiceOutRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // Validate rejecter authorization (FINANCE or ADMIN only)
        approvalService.validateInvoiceOutRejecter(invoice, currentUser);

        // Transition PENDING -> REJECTED
        statusMachine.reject(invoice, currentUser, note);
        invoice = invoiceOutRepository.save(invoice);

        log.info("Rejected outgoing invoice {} by user {}", id, currentUser.getEmail());

        return invoiceOutMapper.toResponse(invoice);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public InvoiceOutResponse recordPayment(Long id, PaymentRequest paymentRequest, User currentUser) {
        InvoiceOut invoice = invoiceOutRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // Create payment record (INBOUND - client pays us)
        Payment payment = Payment.builder()
                .document(invoice)
                .direction(PaymentDirection.INBOUND)
                .method(paymentRequest.getMethod())
                .amount(paymentRequest.getAmount())
                .paidAt(paymentRequest.getPaidAt())
                .reference(paymentRequest.getReference())
                .build();

        paymentRepository.save(payment);

        // Transition APPROVED -> PAID
        statusMachine.markAsPaid(invoice, currentUser, paymentRequest.getNote());
        invoice = invoiceOutRepository.save(invoice);

        log.info("Recorded payment for outgoing invoice {} by user {} - amount: {}", 
                id, currentUser.getEmail(), paymentRequest.getAmount());

        return invoiceOutMapper.toResponse(invoice);
    }

    /**
     * Check if user has a specific role.
     */
    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == roleName);
    }
}
