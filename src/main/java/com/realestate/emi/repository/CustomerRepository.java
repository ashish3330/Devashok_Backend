package com.realestate.emi.repository;

import com.realestate.emi.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    List<Customer> findByOrganizationIdOrderByFullNameAsc(Long orgId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    boolean existsByAadharNumber(String aadharNumber);

    boolean existsByAadharNumberAndIdNot(String aadharNumber, Long id);

    boolean existsByPanNumber(String panNumber);

    boolean existsByPanNumberAndIdNot(String panNumber, Long id);

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    long countByOrganizationId(Long orgId);

    boolean existsByPhoneNumberAndOrganizationId(String phoneNumber, Long orgId);

    boolean existsByAadharNumberAndOrganizationId(String aadharNumber, Long orgId);

    boolean existsByPanNumberAndOrganizationId(String panNumber, Long orgId);

    boolean existsByPhoneNumberAndIdNotAndOrganizationId(String phoneNumber, Long id, Long orgId);

    boolean existsByAadharNumberAndIdNotAndOrganizationId(String aadharNumber, Long id, Long orgId);

    boolean existsByPanNumberAndIdNotAndOrganizationId(String panNumber, Long id, Long orgId);
}
