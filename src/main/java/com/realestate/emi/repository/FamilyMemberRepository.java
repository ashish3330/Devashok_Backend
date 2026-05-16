package com.realestate.emi.repository;

import com.realestate.emi.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findAllByOrganizationId(Long organizationId);

    List<FamilyMember> findAllByResidentIdAndOrganizationId(Long residentId, Long organizationId);

    List<FamilyMember> findAllByFlatIdAndOrganizationId(Long flatId, Long organizationId);

    Optional<FamilyMember> findByIdAndOrganizationId(Long id, Long organizationId);
}
