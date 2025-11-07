package com.docflow.service;

import com.docflow.domain.entity.InvoiceIn;
import com.docflow.domain.entity.Payment;
import com.docflow.domain.entity.User;
import com.docflow.domain.entity.Vendor;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.domain.enums.PaymentDirection;
import com.docflow.domain.enums.RoleName;
import com.docflow.dto.filter.InvoiceFilterCriteria;
import com.docflow.dto.filter.PageResponse;
import com.docflow.dto.invoice.InvoiceInRequest;
import com.docflow.dto.invoice.InvoiceInResponse;
import com.docflow.dto.invoice.PaymentRequest;
import com.docflow.exception.ResourceNotFoundException;
import com.docflow.exception.UnauthorizedActionException;
import com.docflow.mapper.InvoiceInMapper;
import com.docflow.repository.InvoiceInRepository;
import com.docflow.repository.PaymentRepository;
import com.docflow.repository.VendorRepository;
import com.docflow.specification.InvoiceInSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceInService {

    private final InvoiceInRepository invoiceInRepository;
    private final VendorRepository vendorRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceInMapper invoiceInMapper;
    private final DocumentStatusMachine statusMachine;
    private final ApprovalService approvalService;

    @Transactional
    public InvoiceInResponse createInvoice(InvoiceInRequest request, User currentUser) {
        // Validate vendor exists
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", request.getVendorId()));

        // Check for duplicate invoice number
        if (invoiceInRepository.existsByInvoiceNo(request.getInvoiceNo())) {
            throw new IllegalArgumentException("Invoice number already exists: " + request.getInvoiceNo());
        }

        // Create invoice in DRAFT status
        InvoiceIn invoice = invoiceInMapper.toEntity(request, vendor, currentUser);
        
        // Set defaults
        if (invoice.getTax() == null) {
            invoice.setTax(java.math.BigDecimal.ZERO);
        }

        invoice = invoiceInRepository.save(invoice);

        log.info("Created incoming invoice {} by user {}", invoice.getId(), currentUser.getEmail());

        return invoiceInMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceInResponse getInvoiceById(Long id) {
        InvoiceIn invoice = invoiceInRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        return invoiceInMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceInResponse> getInvoices(DocumentStatus status, Long vendorId, Pageable pageable) {
        Page<InvoiceIn> invoices = invoiceInRepository.findByFilters(status, vendorId, pageable);
        return invoices.map(invoiceInMapper::toResponse);
    }

    @Transactional
    public InvoiceInResponse updateInvoice(Long id, InvoiceInRequest request, User currentUser) {
        InvoiceIn invoice = invoiceInRepository.findByIdWithDetails(id)
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

        // Validate vendor
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", request.getVendorId()));

        // Update invoice
        invoiceInMapper.updateEntityFromRequest(request, vendor, invoice);
        
        if (invoice.getTax() == null) {
            invoice.setTax(java.math.BigDecimal.ZERO);
        }

        invoice = invoiceInRepository.save(invoice);

        log.info("Updated incoming invoice {} by user {}", id, currentUser.getEmail());

        return invoiceInMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceInResponse submitInvoice(Long id, User currentUser, String note) {
        InvoiceIn invoice = invoiceInRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // ADMIN can submit any invoice, otherwise only owner can submit
        boolean isAdmin = hasRole(currentUser, RoleName.ADMIN);
        if (!isAdmin && !invoice.getOwnerUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("Only the invoice creator or ADMIN can submit it");
        }

        // Transition DRAFT -> PENDING
        statusMachine.submit(invoice, currentUser, note);
        invoice = invoiceInRepository.save(invoice);

        log.info("Submitted incoming invoice {} by user {}", id, currentUser.getEmail());

        return invoiceInMapper.toResponse(invoice);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'FINANCE', 'ADMIN')")
    public InvoiceInResponse approveInvoice(Long id, User currentUser, String note) {
        InvoiceIn invoice = invoiceInRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // Validate approver authorization (checks PO routing if applicable)
        approvalService.validateInvoiceInApprover(invoice, currentUser);

        // Transition PENDING -> APPROVED
        statusMachine.approve(invoice, currentUser, note);
        invoice = invoiceInRepository.save(invoice);

        log.info("Approved incoming invoice {} by user {}", id, currentUser.getEmail());

        return invoiceInMapper.toResponse(invoice);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'FINANCE', 'ADMIN')")
    public InvoiceInResponse rejectInvoice(Long id, User currentUser, String note) {
        InvoiceIn invoice = invoiceInRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // Validate rejecter authorization (checks PO routing if applicable)
        approvalService.validateInvoiceInRejecter(invoice, currentUser);

        // Transition PENDING -> REJECTED
        statusMachine.reject(invoice, currentUser, note);
        invoice = invoiceInRepository.save(invoice);

        log.info("Rejected incoming invoice {} by user {}", id, currentUser.getEmail());

        return invoiceInMapper.toResponse(invoice);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public InvoiceInResponse payInvoice(Long id, PaymentRequest paymentRequest, User currentUser) {
        InvoiceIn invoice = invoiceInRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // Create payment record (OUTBOUND - we pay vendor)
        Payment payment = Payment.builder()
                .document(invoice)
                .direction(PaymentDirection.OUTBOUND)
                .method(paymentRequest.getMethod())
                .amount(paymentRequest.getAmount())
                .paidAt(paymentRequest.getPaidAt())
                .reference(paymentRequest.getReference())
                .build();

        paymentRepository.save(payment);

        // Transition APPROVED -> PAID
        statusMachine.markAsPaid(invoice, currentUser, paymentRequest.getNote());
        invoice = invoiceInRepository.save(invoice);

        log.info("Paid incoming invoice {} by user {} - amount: {}", 
                id, currentUser.getEmail(), paymentRequest.getAmount());

        return invoiceInMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceInResponse> filterInvoices(InvoiceFilterCriteria criteria, Pageable pageable) {
        Specification<InvoiceIn> spec = InvoiceInSpecification.withFilters(criteria);
        Page<InvoiceIn> page = invoiceInRepository.findAll(spec, pageable);
        
        Page<InvoiceInResponse> responsePage = page.map(invoiceInMapper::toResponse);
        
        // Calculate totals
        BigDecimal totalAmount = page.getContent().stream()
                .map(InvoiceIn::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return PageResponse.of(responsePage, totalAmount, page.getTotalElements());
    }

    /**
     * Check if user has a specific role.
     */
    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == roleName);
    }
}
