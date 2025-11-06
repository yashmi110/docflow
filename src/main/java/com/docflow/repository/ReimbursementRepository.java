package com.docflow.repository;

import com.docflow.domain.entity.Reimbursement;
import com.docflow.domain.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReimbursementRepository extends JpaRepository<Reimbursement, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Reimbursement> {

    @Query("SELECT r FROM Reimbursement r LEFT JOIN FETCH r.employee LEFT JOIN FETCH r.ownerUser WHERE r.id = :id")
    Optional<Reimbursement> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT r FROM Reimbursement r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:employeeId IS NULL OR r.employee.id = :employeeId)")
    Page<Reimbursement> findByFilters(@Param("status") DocumentStatus status,
                                       @Param("employeeId") Long employeeId,
                                       Pageable pageable);

    @Query("SELECT r FROM Reimbursement r WHERE r.employee.id = :employeeId")
    Page<Reimbursement> findByEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

    @Query("SELECT r FROM Reimbursement r WHERE r.status = :status")
    Page<Reimbursement> findByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    @Query("SELECT COUNT(r) > 0 FROM Reimbursement r JOIN ExpenseClaim e ON r.employee.id = e.employee.id " +
           "WHERE e.id = :claimId AND r.status != 'REJECTED' AND r.status != 'CANCELLED'")
    boolean existsActiveReimbursementForClaim(@Param("claimId") Long claimId);
}
