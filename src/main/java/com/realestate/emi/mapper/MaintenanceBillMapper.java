package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.MaintenanceBillResponse;
import com.realestate.emi.dto.response.MaintenancePaymentResponse;
import com.realestate.emi.entity.MaintenanceBill;
import com.realestate.emi.entity.MaintenancePayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring")
public interface MaintenanceBillMapper {

    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    @Mapping(target = "blockName", source = "flat.block.name")
    @Mapping(target = "outstandingAmount",
            expression = "java(bill.getTotalAmount() == null ? java.math.BigDecimal.ZERO : bill.getTotalAmount().subtract(bill.getPaidAmount() == null ? java.math.BigDecimal.ZERO : bill.getPaidAmount()).max(java.math.BigDecimal.ZERO))")
    @Mapping(target = "daysOverdue",
            expression = "java(bill.getDueDate() == null || !bill.getDueDate().isBefore(java.time.LocalDate.now()) ? 0L : java.time.temporal.ChronoUnit.DAYS.between(bill.getDueDate(), java.time.LocalDate.now()))")
    @Mapping(target = "payments", ignore = true)
    MaintenanceBillResponse toResponse(MaintenanceBill bill);

    @Mapping(target = "billId", source = "bill.id")
    @Mapping(target = "residentId", source = "resident.id")
    @Mapping(target = "residentName", source = "resident.fullName")
    MaintenancePaymentResponse toPaymentResponse(MaintenancePayment payment);

    default BigDecimal computeOutstanding(MaintenanceBill bill) {
        BigDecimal total = bill.getTotalAmount() == null ? BigDecimal.ZERO : bill.getTotalAmount();
        BigDecimal paid = bill.getPaidAmount() == null ? BigDecimal.ZERO : bill.getPaidAmount();
        return total.subtract(paid).max(BigDecimal.ZERO);
    }

    default long computeDaysOverdue(MaintenanceBill bill) {
        if (bill.getDueDate() == null) return 0L;
        LocalDate today = LocalDate.now();
        if (!bill.getDueDate().isBefore(today)) return 0L;
        return ChronoUnit.DAYS.between(bill.getDueDate(), today);
    }
}
