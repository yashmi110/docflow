-- ============================================
-- DOCFLOW DOCUMENT FILES
-- File attachments for documents
-- ============================================

CREATE TABLE doc_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_doc_files_document FOREIGN KEY (doc_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_files_user FOREIGN KEY (uploaded_by) REFERENCES users(id),
    
    INDEX idx_doc_files_doc (doc_id),
    INDEX idx_doc_files_uploaded_by (uploaded_by),
    INDEX idx_doc_files_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
