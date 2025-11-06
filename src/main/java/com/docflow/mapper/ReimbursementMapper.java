package com.docflow.mapper;

import com.docflow.domain.entity.Employee;
import com.docflow.domain.entity.Reimbursement;
import com.docflow.domain.entity.User;
import com.docflow.dto.reimbursement.ReimbursementRequest;
import com.docflow.dto.reimbursement.ReimbursementResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true))
public interface ReimbursementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "docType", constant = "REIMBURSEMENT")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "ownerUser", source = "owner")
    @Mapping(target = "employee", source = "employee")
    @Mapping(target = "requestedDate", source = "request.requestedDate")
    @Mapping(target = "currency", source = "request.currency")
    @Mapping(target = "total", source = "request.total")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Reimbursement toEntity(ReimbursementRequest request, Employee employee, User owner);

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.user.name")
    @Mapping(target = "employeeEmail", source = "employee.user.email")
    @Mapping(target = "expenseClaimId", ignore = true)
    @Mapping(target = "ownerUserId", source = "ownerUser.id")
    @Mapping(target = "ownerUserEmail", source = "ownerUser.email")
    @Mapping(target = "ownerUserName", source = "ownerUser.name")
    ReimbursementResponse toResponse(Reimbursement reimbursement);

    List<ReimbursementResponse> toResponseList(List<Reimbursement> reimbursements);
}
