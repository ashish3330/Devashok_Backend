package com.realestate.emi.dto.response;

import com.realestate.emi.enums.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {

    private Long id;
    private Long flatId;
    private String flatNumber;
    private String blockName;
    private Long residentId;
    private String residentName;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private List<String> photoUrls;
    private ComplaintStatus status;
    private Long assignedStaffId;
    private String assignedStaffName;
    private String resolutionNotes;
    private LocalDateTime slaDeadline;
    private LocalDateTime resolvedAt;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
