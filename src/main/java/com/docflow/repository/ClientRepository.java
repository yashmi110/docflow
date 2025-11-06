package com.docflow.repository;

import com.docflow.domain.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByName(String name);

    Optional<Client> findByTaxId(String taxId);

    boolean existsByName(String name);

    boolean existsByTaxId(String taxId);
}
