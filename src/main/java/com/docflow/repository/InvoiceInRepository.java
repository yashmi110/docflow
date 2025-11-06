package com.docflow.repository;

import com.docflow.domain.entity.InvoiceIn;
import com.docflow.domain.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceInRepository extends JpaRepository<InvoiceIn, Long>, 
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<InvoiceIn> {

    @Query("SELECT i FROM InvoiceIn i LEFT JOIN FETCH i.vendor LEFT JOIN FETCH i.ownerUser WHERE i.id = :id")
    Optional<InvoiceIn> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT i FROM InvoiceIn i WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:vendorId IS NULL OR i.vendor.id = :vendorId)")
    Page<InvoiceIn> findByFilters(@Param("status") DocumentStatus status,
                                   @Param("vendorId") Long vendorId,
                                   Pageable pageable);

    @Query("SELECT i FROM InvoiceIn i WHERE i.vendor.id = :vendorId")
    Page<InvoiceIn> findByVendorId(@Param("vendorId") Long vendorId, Pageable pageable);

    @Query("SELECT i FROM InvoiceIn i WHERE i.status = :status")
    Page<InvoiceIn> findByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    boolean existsByInvoiceNo(String invoiceNo);
}
