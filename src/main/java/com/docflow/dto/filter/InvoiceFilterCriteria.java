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
public class InvoiceFilterCriteria {

    private DocumentStatus status;
    private Long vendorId;
    private Long clientId;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal amountMin;
    private BigDecimal amountMax;
    private String currency;
    private String invoiceNo;
}
