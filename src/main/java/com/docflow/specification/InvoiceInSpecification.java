package com.docflow.specification;

import com.docflow.domain.entity.InvoiceIn;
import com.docflow.dto.filter.InvoiceFilterCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class InvoiceInSpecification {

    public static Specification<InvoiceIn> withFilters(InvoiceFilterCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getVendorId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("vendor").get("id"), criteria.getVendorId()));
            }

            if (criteria.getDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("invoiceDate"), criteria.getDateFrom()));
            }

            if (criteria.getDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("invoiceDate"), criteria.getDateTo()));
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

            if (criteria.getInvoiceNo() != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("invoiceNo")),
                        "%" + criteria.getInvoiceNo().toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
