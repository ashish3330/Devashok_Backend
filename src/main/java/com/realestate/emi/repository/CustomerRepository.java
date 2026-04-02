package com.realestate.emi.repository;

import com.realestate.emi.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    boolean existsByAadharNumber(String aadharNumber);

    boolean existsByAadharNumberAndIdNot(String aadharNumber, Long id);

    boolean existsByPanNumber(String panNumber);

    boolean existsByPanNumberAndIdNot(String panNumber, Long id);

    Optional<Customer> findByPhoneNumber(String phoneNumber);
}
