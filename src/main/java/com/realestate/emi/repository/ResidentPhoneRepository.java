package com.realestate.emi.repository;

import com.realestate.emi.entity.ResidentPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentPhoneRepository extends JpaRepository<ResidentPhone, Long> {

    List<ResidentPhone> findByResidentIdOrderByLabelAsc(Long residentId);

    Optional<ResidentPhone> findByPhone(String phone);

    Optional<ResidentPhone> findByOrganizationIdAndPhone(Long orgId, String phone);

    boolean existsByOrganizationIdAndPhone(Long orgId, String phone);

    Optional<ResidentPhone> findByIdAndResidentId(Long id, Long residentId);
}
