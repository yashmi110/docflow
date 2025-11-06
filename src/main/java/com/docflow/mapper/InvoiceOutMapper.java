package com.docflow.mapper;

import com.docflow.domain.entity.Client;
import com.docflow.domain.entity.InvoiceOut;
import com.docflow.domain.entity.User;
import com.docflow.dto.invoice.InvoiceOutRequest;
import com.docflow.dto.invoice.InvoiceOutResponse;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceOutMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "docType", constant = "INVOICE_OUT")
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "ownerUser", source = "owner")
    @Mapping(target = "client", source = "client")
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
    InvoiceOut toEntity(InvoiceOutRequest request, Client client, User owner);

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "ownerUserId", source = "ownerUser.id")
    @Mapping(target = "ownerUserEmail", source = "ownerUser.email")
    @Mapping(target = "ownerUserName", source = "ownerUser.name")
    InvoiceOutResponse toResponse(InvoiceOut invoice);

    List<InvoiceOutResponse> toResponseList(List<InvoiceOut> invoices);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "docType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "ownerUser", ignore = true)
    @Mapping(target = "client", source = "client")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(InvoiceOutRequest request, Client client, @MappingTarget InvoiceOut invoice);

    default BigDecimal mapTax(BigDecimal tax) {
        return tax != null ? tax : BigDecimal.ZERO;
    }
}
