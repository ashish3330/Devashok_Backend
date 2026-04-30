package com.realestate.emi.repository;

import com.realestate.emi.entity.ResidentUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResidentUserRepository extends JpaRepository<ResidentUser, Long> {

    Optional<ResidentUser> findByResidentId(Long residentId);

    Optional<ResidentUser> findByPhone(String phone);
}
