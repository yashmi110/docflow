package com.docflow.dto.audit;

import com.docflow.domain.enums.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusTransitionRequest {

    @NotNull(message = "Target status is required")
    private DocumentStatus targetStatus;

    private String note;
}
