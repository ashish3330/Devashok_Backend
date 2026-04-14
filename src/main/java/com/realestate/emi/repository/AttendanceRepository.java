package com.realestate.emi.repository;

import com.realestate.emi.entity.Attendance;
import com.realestate.emi.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByStaffIdAndDate(Long staffId, LocalDate date);

    List<Attendance> findByStaffIdAndDateBetweenOrderByDateAsc(Long staffId, LocalDate from, LocalDate to);

    List<Attendance> findByDateOrderByStaffFullNameAsc(LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.staff.id = :staffId AND a.status = :status " +
            "AND YEAR(a.date) = :year AND MONTH(a.date) = :month")
    long countByStaffAndStatusAndMonth(@Param("staffId") Long staffId, @Param("status") AttendanceStatus status,
                                       @Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(a.overtimeHours), 0) FROM Attendance a WHERE a.staff.id = :staffId " +
            "AND YEAR(a.date) = :year AND MONTH(a.date) = :month")
    Double sumOvertimeByStaffAndMonth(@Param("staffId") Long staffId, @Param("year") int year, @Param("month") int month);
}
