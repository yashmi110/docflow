package com.docflow.mapper;

import com.docflow.domain.entity.InvoiceIn;
import com.docflow.domain.entity.User;
import com.docflow.domain.entity.Vendor;
import com.docflow.domain.enums.DocumentStatus;
import com.docflow.domain.enums.DocumentType;
import com.docflow.dto.invoice.InvoiceInRequest;
import com.docflow.dto.invoice.InvoiceInResponse;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceInMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "docType", constant = "INVOICE_IN")
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "ownerUser", source = "owner")
    @Mapping(target = "vendor", source = "vendor")
    @Mapping(target = "invoiceNo", source = "request.invoiceNo")
    @Mapping(target = "invoiceDate", source = "request.invoiceDate")
    @Mapping(target = "dueDate", source = "request.dueDate")
    @Mapping(target = "currency", source = "request.currency")
    @Mapping(target = "subtotal", source = "request.subtotal")
    @Mapping(target = "tax", source = "request.tax")
    @Mapping(target = "total", source = "request.total")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    InvoiceIn toEntity(InvoiceInRequest request, Vendor vendor, User owner);

    @Mapping(target = "vendorId", source = "vendor.id")
    @Mapping(target = "vendorName", source = "vendor.name")
    @Mapping(target = "ownerUserId", source = "ownerUser.id")
    @Mapping(target = "ownerUserEmail", source = "ownerUser.email")
    @Mapping(target = "ownerUserName", source = "ownerUser.name")
    InvoiceInResponse toResponse(InvoiceIn invoice);

    List<InvoiceInResponse> toResponseList(List<InvoiceIn> invoices);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "docType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "ownerUser", ignore = true)
    @Mapping(target = "vendor", source = "vendor")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(InvoiceInRequest request, Vendor vendor, @MappingTarget InvoiceIn invoice);

    default BigDecimal mapTax(BigDecimal tax) {
        return tax != null ? tax : BigDecimal.ZERO;
    }
}
