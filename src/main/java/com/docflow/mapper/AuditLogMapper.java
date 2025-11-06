package com.docflow.mapper;

import com.docflow.domain.entity.AuditLog;
import com.docflow.dto.audit.AuditLogResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .documentId(auditLog.getDocument().getId())
                .userId(auditLog.getUser().getId())
                .userEmail(auditLog.getUser().getEmail())
                .userName(auditLog.getUser().getName())
                .action(auditLog.getAction())
                .fromStatus(auditLog.getFromStatus())
                .toStatus(auditLog.getToStatus())
                .note(auditLog.getNote())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    public List<AuditLogResponse> toResponseList(List<AuditLog> auditLogs) {
        if (auditLogs == null) {
            return List.of();
        }

        return auditLogs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
