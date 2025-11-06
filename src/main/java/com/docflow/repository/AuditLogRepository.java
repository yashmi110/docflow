package com.docflow.repository;

import com.docflow.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific document, ordered by creation time descending.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.document.id = :documentId ORDER BY a.createdAt DESC")
    List<AuditLog> findByDocumentIdOrderByCreatedAtDesc(@Param("documentId") Long documentId);

    /**
     * Find audit logs for a document with pagination.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.document.id = :documentId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDocumentId(@Param("documentId") Long documentId, Pageable pageable);

    /**
     * Find audit logs by user.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find audit logs by action type.
     */
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

    /**
     * Find audit logs within a date range.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Find audit logs for a document by action.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.document.id = :documentId AND a.action = :action ORDER BY a.createdAt DESC")
    List<AuditLog> findByDocumentIdAndAction(@Param("documentId") Long documentId, 
                                              @Param("action") String action);

    /**
     * Count audit logs for a document.
     */
    long countByDocumentId(Long documentId);
}
