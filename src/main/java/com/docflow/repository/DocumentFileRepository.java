package com.docflow.repository;

import com.docflow.domain.entity.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {

    @Query("SELECT f FROM DocumentFile f WHERE f.document.id = :docId ORDER BY f.createdAt DESC")
    List<DocumentFile> findByDocumentId(@Param("docId") Long docId);

    @Query("SELECT f FROM DocumentFile f LEFT JOIN FETCH f.uploadedBy WHERE f.id = :id")
    Optional<DocumentFile> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT f FROM DocumentFile f WHERE f.id = :fileId AND f.document.id = :docId")
    Optional<DocumentFile> findByIdAndDocumentId(@Param("fileId") Long fileId, @Param("docId") Long docId);

    long countByDocumentId(Long docId);
}
