package com.docflow.dto.claim;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaimRequest {

    @NotNull(message = "Claim date is required")
    private LocalDate claimDate;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;

    @NotEmpty(message = "At least one expense item is required")
    @Valid
    private List<ExpenseItemRequest> items;
}
