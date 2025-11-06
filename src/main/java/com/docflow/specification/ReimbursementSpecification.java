package com.docflow.specification;

import com.docflow.domain.entity.Reimbursement;
import com.docflow.dto.filter.ReimbursementFilterCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ReimbursementSpecification {

    public static Specification<Reimbursement> withFilters(ReimbursementFilterCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employee").get("id"), criteria.getEmployeeId()));
            }

            if (criteria.getRequestedDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("requestedDate"), criteria.getRequestedDateFrom()));
            }

            if (criteria.getRequestedDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("requestedDate"), criteria.getRequestedDateTo()));
            }

            if (criteria.getAmountMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("total"), criteria.getAmountMin()));
            }

            if (criteria.getAmountMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("total"), criteria.getAmountMax()));
            }

            if (criteria.getCurrency() != null) {
                predicates.add(criteriaBuilder.equal(root.get("currency"), criteria.getCurrency()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
