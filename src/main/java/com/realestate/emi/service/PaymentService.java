package com.realestate.emi.service;

import com.realestate.emi.dto.request.PaymentRequest;
import com.realestate.emi.dto.response.PaymentResponse;
import com.realestate.emi.entity.Deal;
import com.realestate.emi.entity.EmiSchedule;
import com.realestate.emi.entity.Payment;
import com.realestate.emi.enums.DealStatus;
import com.realestate.emi.enums.EmiStatus;
import com.realestate.emi.enums.PaymentMethod;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.PaymentMapper;
import com.realestate.emi.repository.DealRepository;
import com.realestate.emi.repository.EmiScheduleRepository;
import com.realestate.emi.repository.PaymentRepository;
import com.realestate.emi.security.CustomPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final DealRepository dealRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final DealService dealService;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponse recordPayment(Long dealId, PaymentRequest request) {
        log.debug("Recording payment for dealId: {}", dealId);

        // 1. Load deal
        Deal deal = dealRepository.findByIdWithDetails(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", dealId));

        // 2. Check if deal is already completed
        if (deal.getStatus() == DealStatus.COMPLETED) {
            throw new ServiceException("Deal is already completed", "DEAL_COMPLETED");
        }

        // 3. Validate payment method is not INITIAL_DEPOSIT
        if (request.getPaymentMethod() == PaymentMethod.INITIAL_DEPOSIT) {
            throw new ServiceException("Payment method INITIAL_DEPOSIT is not allowed for manual payments",
                    "INVALID_PAYMENT_METHOD");
        }

        // 4. Get the current admin from SecurityContext
        String adminUsername = getAdminUsername();

        // 5. Create and save Payment
        Payment payment = Payment.builder()
                .deal(deal)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .utrNumber(request.getUtrNumber())
                .notes(request.getNotes())
                .createdByAdmin(adminUsername)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Saved payment {} for deal {}", payment.getId(), dealId);

        // 6. Apply FIFO to schedules
        dealService.applyPaymentToSchedules(deal, request.getAmount());

        // 7. Update next due date
        dealService.updateNextDueDate(deal);

        // 8. Check deal completion
        dealService.checkDealCompletion(deal);

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse paySpecificEmi(Long dealId, Long scheduleId, PaymentRequest request) {
        log.debug("Paying specific EMI schedule {} for dealId: {}", scheduleId, dealId);

        Deal deal = dealRepository.findByIdWithDetails(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", dealId));

        if (deal.getStatus() == DealStatus.COMPLETED) {
            throw new ServiceException("Deal is already completed", "DEAL_COMPLETED");
        }

        if (request.getPaymentMethod() == PaymentMethod.INITIAL_DEPOSIT) {
            throw new ServiceException("Payment method INITIAL_DEPOSIT is not allowed for manual payments",
                    "INVALID_PAYMENT_METHOD");
        }

        EmiSchedule schedule = emiScheduleRepository.findByIdAndDeal(scheduleId, deal)
                .orElseThrow(() -> new ResourceNotFoundException("EmiSchedule", scheduleId));

        if (schedule.getStatus() == EmiStatus.PAID) {
            throw new ServiceException("This EMI is already fully paid", "EMI_ALREADY_PAID");
        }

        BigDecimal remainingDue = schedule.getDueAmount().subtract(schedule.getPaidAmount());
        BigDecimal payAmount = request.getAmount();

        if (payAmount.compareTo(remainingDue) > 0) {
            throw new ServiceException(
                    "Payment amount exceeds remaining due of " + remainingDue, "AMOUNT_EXCEEDS_DUE");
        }

        // Update the EMI schedule
        schedule.setPaidAmount(schedule.getPaidAmount().add(payAmount));
        if (schedule.getPaidAmount().compareTo(schedule.getDueAmount()) >= 0) {
            schedule.setStatus(EmiStatus.PAID);
        } else {
            schedule.setStatus(EmiStatus.PARTIAL);
        }
        emiScheduleRepository.save(schedule);
        log.info("Updated EMI schedule {} to status {}", scheduleId, schedule.getStatus());

        // Create payment record linked to this schedule
        String adminUsername = getAdminUsername();
        Payment payment = Payment.builder()
                .deal(deal)
                .emiSchedule(schedule)
                .amount(payAmount)
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .utrNumber(request.getUtrNumber())
                .notes(request.getNotes())
                .createdByAdmin(adminUsername)
                .build();
        payment = paymentRepository.save(payment);
        log.info("Saved payment {} for EMI schedule {} of deal {}", payment.getId(), scheduleId, dealId);

        // Update next due date and check deal completion
        dealService.updateNextDueDate(deal);
        dealService.checkDealCompletion(deal);

        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByDeal(Long dealId) {
        log.debug("Fetching payments for dealId: {}", dealId);
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", dealId));
        return paymentMapper.toResponseList(paymentRepository.findByDealOrderByPaymentDateAsc(deal));
    }

    private String getAdminUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomPrincipal principal) {
            return principal.getEmail();
        }
        if (authentication != null) {
            return authentication.getName();
        }
        return "unknown";
    }
}
