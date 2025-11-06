package com.docflow.exception;

import com.docflow.domain.enums.DocumentStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(DocumentStatus from, DocumentStatus to) {
        super(String.format("Invalid status transition from %s to %s", from, to));
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
