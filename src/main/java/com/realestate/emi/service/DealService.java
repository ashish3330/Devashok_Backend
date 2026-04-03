package com.realestate.emi.service;

import com.realestate.emi.dto.request.DealRequest;
import com.realestate.emi.dto.request.DealStatusRequest;
import com.realestate.emi.dto.response.DealDetailResponse;
import com.realestate.emi.dto.response.DealSummaryResponse;
import com.realestate.emi.dto.response.EmiScheduleResponse;
import com.realestate.emi.dto.response.PaymentResponse;
import com.realestate.emi.entity.Customer;
import com.realestate.emi.entity.Deal;
import com.realestate.emi.entity.EmiSchedule;
import com.realestate.emi.entity.Payment;
import com.realestate.emi.entity.PropertyType;
import com.realestate.emi.enums.DealStatus;
import com.realestate.emi.enums.EmiStatus;
import com.realestate.emi.enums.PaymentMethod;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.DealMapper;
import com.realestate.emi.mapper.EmiScheduleMapper;
import com.realestate.emi.mapper.PaymentMapper;
import com.realestate.emi.repository.CustomerRepository;
import com.realestate.emi.repository.DealRepository;
import com.realestate.emi.repository.EmiScheduleRepository;
import com.realestate.emi.repository.PaymentRepository;
import com.realestate.emi.repository.PropertyTypeRepository;
import com.realestate.emi.util.EmiCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepository;
    private final CustomerRepository customerRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final PaymentRepository paymentRepository;
    private final EmiCalculator emiCalculator;
    private final DealMapper dealMapper;
    private final EmiScheduleMapper emiScheduleMapper;
    private final PaymentMapper paymentMapper;

    @Transactional
    public DealDetailResponse createDeal(DealRequest request) {
        log.debug("Creating deal for customerId: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));
        PropertyType propertyType = propertyTypeRepository.findById(request.getPropertyTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("PropertyType", request.getPropertyTypeId()));

        if (request.getInitialDeposit().compareTo(request.getTotalAmount()) > 0) {
            throw new ServiceException("Initial deposit cannot exceed total amount", "INVALID_DEPOSIT");
        }

        BigDecimal totalPayableAfterDeposit = request.getTotalAmount().subtract(request.getInitialDeposit());

        // 4. Calculate EMI
        BigDecimal interestRate = request.getInterestRatePercent() != null
                ? request.getInterestRatePercent()
                : BigDecimal.ZERO;
        BigDecimal emiAmount = emiCalculator.calculate(totalPayableAfterDeposit, interestRate, request.getEmiTenureMonths());

        // 5. Build and save Deal
        Deal deal = Deal.builder()
                .customer(customer)
                .propertyType(propertyType)
                .propertyDescription(request.getPropertyDescription())
                .totalAmount(request.getTotalAmount())
                .initialDeposit(request.getInitialDeposit())
                .emiTenureMonths(request.getEmiTenureMonths())
                .interestRatePercent(interestRate)
                .dealDate(request.getDealDate())
                .status(DealStatus.ACTIVE)
                .emiAmountPerMonth(emiAmount)
                .totalPayableAfterDeposit(totalPayableAfterDeposit)
                .nextDueDate(request.getDealDate().plusMonths(1))
                .build();

        deal = dealRepository.save(deal);

        // 6. Generate EMI schedule records
        List<EmiSchedule> schedules = new ArrayList<>();
        for (int i = 1; i <= request.getEmiTenureMonths(); i++) {
            EmiSchedule schedule = EmiSchedule.builder()
                    .deal(deal)
                    .dueDate(request.getDealDate().plusMonths(i))
                    .dueAmount(emiAmount)
                    .paidAmount(BigDecimal.ZERO)
                    .status(EmiStatus.PENDING)
                    .build();
            schedules.add(schedule);
        }
        emiScheduleRepository.saveAll(schedules);

        // 7. If initialDeposit > 0: create Payment(INITIAL_DEPOSIT) and apply to schedules
        if (request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            Payment initialPayment = Payment.builder()
                    .deal(deal)
                    .amount(request.getInitialDeposit())
                    .paymentDate(LocalDateTime.now())
                    .paymentMethod(PaymentMethod.INITIAL_DEPOSIT)
                    .notes("Initial deposit on deal creation")
                    .createdByAdmin("system")
                    .build();
            paymentRepository.save(initialPayment);
            // Note: initial deposit does NOT apply to EMI schedules; it reduces totalPayableAfterDeposit
        }

        // 8. Update next due date and check completion
        updateNextDueDate(deal);
        checkDealCompletion(deal);

        // 9. Return DealDetailResponse
        return buildDealDetailResponse(deal);
    }

    @Transactional(readOnly = true)
    public List<DealSummaryResponse> findAll() {
        log.debug("Fetching all deals");
        List<Deal> deals = dealRepository.findAllWithDetails();
        return deals.stream()
                .map(deal -> {
                    DealSummaryResponse summary = dealMapper.toSummaryResponse(deal);

                    // EMI payments only (INITIAL_DEPOSIT excluded by repository query)
                    BigDecimal emiPaid = paymentRepository.sumPaymentsByDealId(deal.getId());

                    BigDecimal outstandingAmount = deal.getTotalPayableAfterDeposit() != null
                            ? deal.getTotalPayableAfterDeposit().subtract(emiPaid)
                            : BigDecimal.ZERO;
                    if (outstandingAmount.compareTo(BigDecimal.ZERO) < 0) {
                        outstandingAmount = BigDecimal.ZERO;
                    }

                    summary.setTotalPaid(emiPaid);
                    summary.setOutstanding(outstandingAmount);
                    return summary;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DealDetailResponse findById(Long id) {
        log.debug("Fetching deal with id: {}", id);
        Deal deal = dealRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", id));
        return buildDealDetailResponse(deal);
    }

    @Transactional
    public DealDetailResponse changeStatus(Long dealId, DealStatusRequest request) {
        log.debug("Changing status of deal id: {} to {}", dealId, request.getStatus());
        Deal deal = dealRepository.findByIdWithDetails(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", dealId));
        deal.setStatus(request.getStatus());
        dealRepository.save(deal);
        log.info("Changed deal {} status to {}", dealId, request.getStatus());
        return buildDealDetailResponse(deal);
    }

    /**
     * FIFO apply payments to EMI schedules.
     * Applies the given amount to PENDING/PARTIAL schedules ordered by dueDate ASC.
     */
    public void applyPaymentToSchedules(Deal deal, BigDecimal amount) {
        List<EmiSchedule> schedules = emiScheduleRepository.findByDealAndStatusInOrderByDueDateAsc(
                deal, List.of(EmiStatus.PENDING, EmiStatus.PARTIAL));

        BigDecimal remaining = amount;
        List<EmiSchedule> toSave = new ArrayList<>();

        for (EmiSchedule schedule : schedules) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal totalDue = schedule.getDueAmount().add(
                    schedule.getBounceCharges() != null ? schedule.getBounceCharges() : BigDecimal.ZERO);
            BigDecimal scheduleRemaining = totalDue.subtract(schedule.getPaidAmount());
            if (remaining.compareTo(scheduleRemaining) >= 0) {
                // Fully pay this schedule (base + bounce charges)
                schedule.setPaidAmount(totalDue);
                schedule.setStatus(EmiStatus.PAID);
                remaining = remaining.subtract(scheduleRemaining);
            } else {
                // Partially pay this schedule
                schedule.setPaidAmount(schedule.getPaidAmount().add(remaining));
                schedule.setStatus(EmiStatus.PARTIAL);
                remaining = BigDecimal.ZERO;
            }
            toSave.add(schedule);
        }

        if (!toSave.isEmpty()) {
            emiScheduleRepository.saveAll(toSave);
        }
    }

    public void updateNextDueDate(Deal deal) {
        List<EmiSchedule> pendingOrPartial = emiScheduleRepository
                .findByDealAndStatusInOrderByDueDateAsc(deal, List.of(EmiStatus.PENDING, EmiStatus.PARTIAL));

        if (!pendingOrPartial.isEmpty()) {
            deal.setNextDueDate(pendingOrPartial.get(0).getDueDate());
        } else {
            deal.setNextDueDate(null);
        }
        dealRepository.save(deal);
    }

    public void checkDealCompletion(Deal deal) {
        List<EmiSchedule> allSchedules = emiScheduleRepository.findByDealOrderByDueDateAsc(deal);
        boolean allPaid = !allSchedules.isEmpty()
                && allSchedules.stream().allMatch(s -> s.getStatus() == EmiStatus.PAID);
        if (allPaid) {
            deal.setStatus(DealStatus.COMPLETED);
            deal.setNextDueDate(null);
            dealRepository.save(deal);
            log.info("Deal {} marked as COMPLETED", deal.getId());
        }
    }

    private DealDetailResponse buildDealDetailResponse(Deal deal) {
        List<EmiSchedule> schedules = emiScheduleRepository.findByDealOrderByDueDateAsc(deal);
        List<Payment> payments = paymentRepository.findByDealOrderByPaymentDateAsc(deal);

        // Sum only EMI payments (exclude INITIAL_DEPOSIT — tracked separately for display)
        BigDecimal emiPaid = payments.stream()
                .filter(p -> p.getPaymentMethod() != PaymentMethod.INITIAL_DEPOSIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = deal.getTotalPayableAfterDeposit() != null
                ? deal.getTotalPayableAfterDeposit().subtract(emiPaid)
                : BigDecimal.ZERO;
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }
        // Note: all payments (including INITIAL_DEPOSIT) are included in paymentResponses for display

        List<EmiScheduleResponse> scheduleResponses = emiScheduleMapper.toResponseList(schedules);
        List<PaymentResponse> paymentResponses = paymentMapper.toResponseList(payments);

        return DealDetailResponse.builder()
                .id(deal.getId())
                .customerId(deal.getCustomer().getId())
                .customerName(deal.getCustomer().getFullName())
                .propertyTypeId(deal.getPropertyType().getId())
                .propertyTypeName(deal.getPropertyType().getName())
                .propertyDescription(deal.getPropertyDescription())
                .totalAmount(deal.getTotalAmount())
                .initialDeposit(deal.getInitialDeposit())
                .emiTenureMonths(deal.getEmiTenureMonths())
                .interestRatePercent(deal.getInterestRatePercent())
                .emiAmountPerMonth(deal.getEmiAmountPerMonth())
                .totalPayableAfterDeposit(deal.getTotalPayableAfterDeposit())
                .dealDate(deal.getDealDate())
                .status(deal.getStatus())
                .nextDueDate(deal.getNextDueDate())
                .totalPaid(emiPaid)
                .outstanding(outstanding)
                .emiSchedules(scheduleResponses)
                .payments(paymentResponses)
                .build();
    }
}
