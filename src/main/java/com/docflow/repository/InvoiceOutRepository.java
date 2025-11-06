package com.docflow.repository;

import com.docflow.domain.entity.InvoiceOut;
import com.docflow.domain.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceOutRepository extends JpaRepository<InvoiceOut, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<InvoiceOut> {

    @Query("SELECT i FROM InvoiceOut i LEFT JOIN FETCH i.client LEFT JOIN FETCH i.ownerUser WHERE i.id = :id")
    Optional<InvoiceOut> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT i FROM InvoiceOut i WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:clientId IS NULL OR i.client.id = :clientId)")
    Page<InvoiceOut> findByFilters(@Param("status") DocumentStatus status,
                                    @Param("clientId") Long clientId,
                                    Pageable pageable);

    @Query("SELECT i FROM InvoiceOut i WHERE i.client.id = :clientId")
    Page<InvoiceOut> findByClientId(@Param("clientId") Long clientId, Pageable pageable);

    @Query("SELECT i FROM InvoiceOut i WHERE i.status = :status")
    Page<InvoiceOut> findByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    boolean existsByInvoiceNo(String invoiceNo);
}
