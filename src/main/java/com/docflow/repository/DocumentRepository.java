package com.docflow.repository;

import com.docflow.domain.entity.Document;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.domain.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Find documents by type.
     */
    List<Document> findByDocType(DocumentType docType);

    /**
     * Find documents by status.
     */
    List<Document> findByStatus(DocumentStatus status);

    /**
     * Find documents by owner.
     */
    @Query("SELECT d FROM Document d WHERE d.ownerUser.id = :userId")
    Page<Document> findByOwnerId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find documents by type and status.
     */
    List<Document> findByDocTypeAndStatus(DocumentType docType, DocumentStatus status);

    /**
     * Find documents by type with pagination.
     */
    Page<Document> findByDocType(DocumentType docType, Pageable pageable);

    /**
     * Find documents by status with pagination.
     */
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    /**
     * Count documents by status.
     */
    long countByStatus(DocumentStatus status);

    /**
     * Count documents by type.
     */
    long countByDocType(DocumentType docType);
}
