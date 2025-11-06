package com.docflow.repository;

import com.docflow.domain.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByName(String name);

    Optional<Vendor> findByTaxId(String taxId);

    boolean existsByName(String name);

    boolean existsByTaxId(String taxId);
}
