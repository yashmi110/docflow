package com.docflow.specification;

import com.docflow.domain.entity.ExpenseClaim;
import com.docflow.dto.filter.ExpenseClaimFilterCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ExpenseClaimSpecification {

    public static Specification<ExpenseClaim> withFilters(ExpenseClaimFilterCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employee").get("id"), criteria.getEmployeeId()));
            }

            if (criteria.getClaimDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("claimDate"), criteria.getClaimDateFrom()));
            }

            if (criteria.getClaimDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("claimDate"), criteria.getClaimDateTo()));
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
