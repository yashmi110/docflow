package com.docflow.mapper;

import com.docflow.domain.entity.Employee;
import com.docflow.domain.entity.ExpenseClaim;
import com.docflow.domain.entity.ExpenseItem;
import com.docflow.domain.entity.User;
import com.docflow.dto.claim.ExpenseClaimRequest;
import com.docflow.dto.claim.ExpenseClaimResponse;
import com.docflow.dto.claim.ExpenseItemRequest;
import com.docflow.dto.claim.ExpenseItemResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        builder = @Builder(disableBuilder = true))
public interface ExpenseClaimMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "docType", constant = "EXPENSE_CLAIM")
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "ownerUser", source = "owner")
    @Mapping(target = "employee", source = "employee")
    @Mapping(target = "claimDate", source = "request.claimDate")
    @Mapping(target = "currency", source = "request.currency")
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    ExpenseClaim toEntity(ExpenseClaimRequest request, Employee employee, User owner);

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.user.name")
    @Mapping(target = "employeeEmail", source = "employee.user.email")
    @Mapping(target = "ownerUserId", source = "ownerUser.id")
    @Mapping(target = "ownerUserEmail", source = "ownerUser.email")
    @Mapping(target = "ownerUserName", source = "ownerUser.name")
    ExpenseClaimResponse toResponse(ExpenseClaim claim);

    List<ExpenseClaimResponse> toResponseList(List<ExpenseClaim> claims);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "claim", ignore = true)
    ExpenseItem toItemEntity(ExpenseItemRequest request);

    ExpenseItemResponse toItemResponse(ExpenseItem item);

    List<ExpenseItemResponse> toItemResponseList(List<ExpenseItem> items);
}
