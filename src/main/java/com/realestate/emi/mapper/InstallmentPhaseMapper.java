package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.InstallmentPhaseResponse;
import com.realestate.emi.dto.response.InstallmentPlanTemplateResponse;
import com.realestate.emi.entity.InstallmentPhase;
import com.realestate.emi.entity.InstallmentPlanTemplate;
import com.realestate.emi.enums.PhaseStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Mapper(componentModel = "spring")
public interface InstallmentPhaseMapper {

    @Mapping(source = "deal.id", target = "dealId")
    @Mapping(target = "totalDue", expression = "java(calcTotalDue(phase))")
    @Mapping(target = "balanceDue", expression = "java(calcBalanceDue(phase))")
    @Mapping(target = "daysOverdue", expression = "java(calcDaysOverdue(phase))")
    InstallmentPhaseResponse toResponse(InstallmentPhase phase);

    List<InstallmentPhaseResponse> toResponseList(List<InstallmentPhase> phases);

    default BigDecimal calcTotalDue(InstallmentPhase phase) {
        BigDecimal interest = phase.getInterestAmount() != null ? phase.getInterestAmount() : BigDecimal.ZERO;
        return phase.getDueAmount().add(interest);
    }

    default BigDecimal calcBalanceDue(InstallmentPhase phase) {
        return calcTotalDue(phase).subtract(phase.getPaidAmount());
    }

    default Long calcDaysOverdue(InstallmentPhase phase) {
        if (phase.getDueDeadline() != null && phase.getStatus() != PhaseStatus.PAID && phase.getStatus() != PhaseStatus.PENDING) {
            long days = ChronoUnit.DAYS.between(phase.getDueDeadline(), LocalDate.now());
            return days > 0 ? days : 0L;
        }
        return 0L;
    }

    InstallmentPlanTemplateResponse toTemplateResponse(InstallmentPlanTemplate template);

    List<InstallmentPlanTemplateResponse> toTemplateResponseList(List<InstallmentPlanTemplate> templates);
}
