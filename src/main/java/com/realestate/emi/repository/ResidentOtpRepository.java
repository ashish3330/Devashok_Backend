package com.realestate.emi.repository;

import com.realestate.emi.entity.ResidentOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentOtpRepository extends JpaRepository<ResidentOtp, Long> {

    Optional<ResidentOtp> findTopByPhoneAndConsumedFalseOrderByExpiresAtDesc(String phone);

    long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime since);

    List<ResidentOtp> findByPhoneAndConsumedFalse(String phone);
}
