package com.docflow.dto.filter;

import com.docflow.domain.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementFilterCriteria {

    private DocumentStatus status;
    private Long employeeId;
    private LocalDate requestedDateFrom;
    private LocalDate requestedDateTo;
    private BigDecimal amountMin;
    private BigDecimal amountMax;
    private String currency;
}
