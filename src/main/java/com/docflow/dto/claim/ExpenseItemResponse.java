package com.docflow.dto.claim;

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
public class ExpenseItemResponse {

    private Long id;
    private String description;
    private LocalDate date;
    private String category;
    private BigDecimal amount;
}
