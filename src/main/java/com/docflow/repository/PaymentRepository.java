package com.docflow.repository;

import com.docflow.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE p.document.id = :documentId ORDER BY p.paidAt DESC")
    List<Payment> findByDocumentId(@Param("documentId") Long documentId);

    @Query("SELECT p FROM Payment p WHERE p.document.id = :documentId AND p.reference = :reference")
    List<Payment> findByDocumentIdAndReference(@Param("documentId") Long documentId, 
                                                @Param("reference") String reference);
}
