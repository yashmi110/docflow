package com.docflow.domain.enums;

public enum CreditNoteDirection {
    VENDOR,     // Credit note from vendor (reduces our payable)
    CLIENT      // Credit note to client (reduces their payable)
}
