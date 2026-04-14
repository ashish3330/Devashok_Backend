package com.realestate.emi.repository;

import com.realestate.emi.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByIsActiveTrueOrderByNameAsc();

    List<Supplier> findByOrganizationIdAndIsActiveTrueOrderByNameAsc(Long orgId);

    Optional<Supplier> findByGstNumber(String gstNumber);
}
