package com.docflow.dto.invoice;

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
public class InvoiceInResponse {

    private Long id;
    private Long vendorId;
    private String vendorName;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private DocumentStatus status;
    private Long ownerUserId;
    private String ownerUserEmail;
    private String ownerUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
