package com.realestate.emi.repository;

import com.realestate.emi.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findAllByOrganizationId(Long organizationId);

    List<Vehicle> findAllByResidentIdAndOrganizationId(Long residentId, Long organizationId);

    Optional<Vehicle> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndVehicleNumberIgnoreCase(Long organizationId, String vehicleNumber);

    Optional<Vehicle> findByOrganizationIdAndVehicleNumberIgnoreCase(Long organizationId, String vehicleNumber);
}
