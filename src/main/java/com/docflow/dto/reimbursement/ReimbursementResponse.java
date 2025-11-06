package com.docflow.dto.reimbursement;

import com.docflow.domain.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private Long expenseClaimId;
    private LocalDate requestedDate;
    private String currency;
    private BigDecimal total;
    private DocumentStatus status;
    private Long ownerUserId;
    private String ownerUserEmail;
    private String ownerUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
