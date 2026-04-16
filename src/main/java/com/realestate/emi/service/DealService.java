package com.realestate.emi.service;

import com.realestate.emi.dto.request.DealRequest;
import com.realestate.emi.dto.request.DealStatusRequest;
import com.realestate.emi.dto.response.DealDetailResponse;
import com.realestate.emi.dto.response.DealSummaryResponse;
import com.realestate.emi.dto.response.EmiScheduleResponse;
import com.realestate.emi.dto.response.PaymentResponse;
import com.realestate.emi.entity.*;
import com.realestate.emi.enums.*;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.DealMapper;
import com.realestate.emi.mapper.EmiScheduleMapper;
import com.realestate.emi.mapper.InstallmentPhaseMapper;
import com.realestate.emi.mapper.PaymentMapper;
import com.realestate.emi.repository.*;
import com.realestate.emi.security.TenantContext;
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
    private final InstallmentPlanTemplateRepository templateRepository;
    private final InstallmentPhaseRepository installmentPhaseRepository;
    private final EmiCalculator emiCalculator;
    private final DealMapper dealMapper;
    private final EmiScheduleMapper emiScheduleMapper;
    private final InstallmentPhaseMapper installmentPhaseMapper;
    private final PaymentMapper paymentMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public DealDetailResponse createDeal(DealRequest request) {
        log.debug("Creating deal for customerId: {}", request.getCustomerId());

        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));
        if (customer.getOrganization() != null && !customer.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Customer", request.getCustomerId());
        }
        PropertyType propertyType = propertyTypeRepository.findById(request.getPropertyTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("PropertyType", request.getPropertyTypeId()));
        if (propertyType.getOrganization() != null && !propertyType.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("PropertyType", request.getPropertyTypeId());
        }

        if (request.getInitialDeposit().compareTo(request.getTotalAmount()) > 0) {
            throw new ServiceException("Initial deposit cannot exceed total amount", "INVALID_DEPOSIT");
        }

        BigDecimal totalPayableAfterDeposit = request.getTotalAmount().subtract(request.getInitialDeposit());
        PlanType planType = request.getPlanType() != null ? request.getPlanType() : PlanType.EMI;

        if (planType == PlanType.EMI) {
            return createEmiDeal(request, customer, propertyType, totalPayableAfterDeposit, org);
        } else {
            return createConstructionLinkedDeal(request, customer, propertyType, totalPayableAfterDeposit, org);
        }
    }

    private DealDetailResponse createEmiDeal(DealRequest request, Customer customer, PropertyType propertyType,
                                              BigDecimal totalPayableAfterDeposit, Organization org) {
        if (request.getEmiTenureMonths() == null) {
            throw new ServiceException("EMI tenure is required for EMI plan type", "MISSING_TENURE");
        }

        BigDecimal interestRate = request.getInterestRatePercent() != null
                ? request.getInterestRatePercent()
                : BigDecimal.ZERO;
        BigDecimal emiAmount = emiCalculator.calculate(totalPayableAfterDeposit, interestRate, request.getEmiTenureMonths());

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
                .planType(PlanType.EMI)
                .emiAmountPerMonth(emiAmount)
                .totalPayableAfterDeposit(totalPayableAfterDeposit)
                .nextDueDate(request.getDealDate().plusMonths(1))
                .organization(org)
                .build();

        deal = dealRepository.save(deal);

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
        }

        updateNextDueDate(deal);
        checkDealCompletion(deal);

        return buildDealDetailResponse(deal);
    }

    private DealDetailResponse createConstructionLinkedDeal(DealRequest request, Customer customer,
                                                             PropertyType propertyType, BigDecimal totalPayableAfterDeposit, Organization org) {
        List<InstallmentPlanTemplate> templates = templateRepository.findByIsActiveTrueOrderByPhaseOrderAsc();
        if (templates.isEmpty()) {
            throw new ServiceException("No active installment plan templates found. Please configure templates first.", "NO_TEMPLATES");
        }

        Deal deal = Deal.builder()
                .customer(customer)
                .propertyType(propertyType)
                .propertyDescription(request.getPropertyDescription())
                .totalAmount(request.getTotalAmount())
                .initialDeposit(request.getInitialDeposit())
                .dealDate(request.getDealDate())
                .status(DealStatus.ACTIVE)
                .planType(PlanType.CONSTRUCTION_LINKED)
                .totalPayableAfterDeposit(totalPayableAfterDeposit)
                .organization(org)
                .build();

        deal = dealRepository.save(deal);

        // Generate installment phases from templates
        List<InstallmentPhase> phases = new ArrayList<>();
        for (InstallmentPlanTemplate template : templates) {
            // Round off to nearest rupee
            BigDecimal dueAmount = totalPayableAfterDeposit
                    .multiply(template.getPercentageOfTotal())
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);

            InstallmentPhase phase = InstallmentPhase.builder()
                    .deal(deal)
                    .template(template)
                    .phaseName(template.getPhaseName())
                    .phaseOrder(template.getPhaseOrder())
                    .percentage(template.getPercentageOfTotal())
                    .dueAmount(dueAmount)
                    .paidAmount(BigDecimal.ZERO)
                    .status(PhaseStatus.PENDING)
                    .milestoneDescription(template.getDescription())
                    .build();
            phases.add(phase);
        }
        installmentPhaseRepository.saveAll(phases);

        // Auto-activate Phase 1 — token is paid, first installment is due
        if (!phases.isEmpty()) {
            InstallmentPhase firstPhase = phases.get(0);
            firstPhase.setStatus(PhaseStatus.DUE);
            firstPhase.setMarkedDueAt(LocalDateTime.now());
            firstPhase.setDueDeadline(request.getDealDate().plusDays(15));
            installmentPhaseRepository.save(firstPhase);
            deal.setNextDueDate(firstPhase.getDueDeadline());
            dealRepository.save(deal);
            log.info("Phase 1 auto-activated for deal {}. Payment due by {}", deal.getId(), firstPhase.getDueDeadline());
        }

        // Record initial deposit payment if any
        if (request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            Payment initialPayment = Payment.builder()
                    .deal(deal)
                    .amount(request.getInitialDeposit())
                    .paymentDate(LocalDateTime.now())
                    .paymentMethod(PaymentMethod.INITIAL_DEPOSIT)
                    .notes("Initial deposit / token money on deal creation")
                    .createdByAdmin("system")
                    .build();
            paymentRepository.save(initialPayment);
        }

        return buildDealDetailResponse(deal);
    }

    @Transactional(readOnly = true)
    public List<DealSummaryResponse> findAll() {
        log.debug("Fetching all deals");
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<Deal> deals = dealRepository.findAllByOrganization(orgId);
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
        Long orgId = tenantContext.getCurrentOrganizationId();
        Deal deal = dealRepository.findByIdAndOrganization(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", id));
        return buildDealDetailResponse(deal);
    }

    @Transactional
    public DealDetailResponse changeStatus(Long dealId, DealStatusRequest request) {
        log.debug("Changing status of deal id: {} to {}", dealId, request.getStatus());
        Deal deal = dealRepository.findByIdWithDetails(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", dealId));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (deal.getOrganization() != null && !deal.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Deal", dealId);
        }
        if (deal.getStatus() == DealStatus.COMPLETED) {
            throw new ServiceException("Cannot change status of a completed deal", "DEAL_COMPLETED");
        }
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
        List<Payment> payments = paymentRepository.findByDealOrderByPaymentDateAsc(deal);

        BigDecimal nonDepositPaid = payments.stream()
                .filter(p -> p.getPaymentMethod() != PaymentMethod.INITIAL_DEPOSIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = deal.getTotalPayableAfterDeposit() != null
                ? deal.getTotalPayableAfterDeposit().subtract(nonDepositPaid)
                : BigDecimal.ZERO;
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }

        List<PaymentResponse> paymentResponses = paymentMapper.toResponseList(payments);

        DealDetailResponse.DealDetailResponseBuilder<?, ?> builder = DealDetailResponse.builder()
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
                .planType(deal.getPlanType())
                .status(deal.getStatus())
                .nextDueDate(deal.getNextDueDate())
                .totalPaid(nonDepositPaid)
                .outstanding(outstanding)
                .payments(paymentResponses);

        if (deal.getPlanType() == PlanType.CONSTRUCTION_LINKED) {
            List<InstallmentPhase> phases = installmentPhaseRepository.findByDealOrderByPhaseOrderAsc(deal);
            builder.installmentPhases(installmentPhaseMapper.toResponseList(phases));
        } else {
            List<EmiSchedule> schedules = emiScheduleRepository.findByDealOrderByDueDateAsc(deal);
            builder.emiSchedules(emiScheduleMapper.toResponseList(schedules));
        }

        return builder.build();
    }
}
