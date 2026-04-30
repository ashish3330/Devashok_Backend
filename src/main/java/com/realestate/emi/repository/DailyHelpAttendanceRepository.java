package com.realestate.emi.repository;

import com.realestate.emi.entity.DailyHelpAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DailyHelpAttendanceRepository extends JpaRepository<DailyHelpAttendance, Long> {

    List<DailyHelpAttendance> findByOrganizationIdAndFlatIdAndCheckedInAtBetween(Long orgId, Long flatId, LocalDateTime from, LocalDateTime to);

    List<DailyHelpAttendance> findByOrganizationIdAndHelpIdAndCheckedInAtBetween(Long orgId, Long helpId, LocalDateTime from, LocalDateTime to);
}
