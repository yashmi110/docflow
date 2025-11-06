package com.docflow.repository;

import com.docflow.domain.entity.ExpenseClaim;
import com.docflow.domain.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<ExpenseClaim> {

    @Query("SELECT e FROM ExpenseClaim e LEFT JOIN FETCH e.employee LEFT JOIN FETCH e.ownerUser LEFT JOIN FETCH e.items WHERE e.id = :id")
    Optional<ExpenseClaim> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT e FROM ExpenseClaim e WHERE " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:employeeId IS NULL OR e.employee.id = :employeeId)")
    Page<ExpenseClaim> findByFilters(@Param("status") DocumentStatus status,
                                      @Param("employeeId") Long employeeId,
                                      Pageable pageable);

    @Query("SELECT e FROM ExpenseClaim e WHERE e.employee.id = :employeeId")
    Page<ExpenseClaim> findByEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

    @Query("SELECT e FROM ExpenseClaim e WHERE e.status = :status")
    Page<ExpenseClaim> findByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    @Query("SELECT e FROM ExpenseClaim e WHERE e.ownerUser.id = :userId")
    Page<ExpenseClaim> findByOwnerId(@Param("userId") Long userId, Pageable pageable);
}
