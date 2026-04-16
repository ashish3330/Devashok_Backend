package com.realestate.emi.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {

    private Long staffId;
    private String staffName;
    private String staffRole;
    private int year;
    private int month;
    private int totalDays;
    private int workingDays;
    private int presentDays;
    private int absentDays;
    private int halfDays;
    private int leaveDays;
    private int holidays;
    private int unmarkedDays;
    private double totalOvertimeHours;
    private int lateCount;
    private int earlyLeaveCount;
    private double attendancePercentage;
}
