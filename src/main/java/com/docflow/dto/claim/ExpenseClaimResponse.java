package com.docflow.dto.claim;

import com.docflow.domain.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaimResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private LocalDate claimDate;
    private String currency;
    private BigDecimal total;
    private DocumentStatus status;
    private List<ExpenseItemResponse> items;
    private Long ownerUserId;
    private String ownerUserEmail;
    private String ownerUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
