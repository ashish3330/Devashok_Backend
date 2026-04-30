package com.realestate.emi.repository;

import com.realestate.emi.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    List<Amenity> findByOrganizationIdAndIsActiveTrueOrderByNameAsc(Long orgId);

    Optional<Amenity> findByIdAndOrganizationId(Long id, Long orgId);
}
