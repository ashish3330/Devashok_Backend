package com.realestate.emi.service;

import com.realestate.emi.dto.request.MaintenanceBillGenerateRequest;
import com.realestate.emi.dto.request.MaintenancePaymentRequest;
import com.realestate.emi.dto.response.MaintenanceBillResponse;
import com.realestate.emi.dto.response.MaintenancePaymentResponse;
import com.realestate.emi.entity.*;
import com.realestate.emi.enums.MaintenanceBillStatus;
import com.realestate.emi.enums.PaymentMethod;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.MaintenanceBillMapper;
import com.realestate.emi.repository.*;
import com.realestate.emi.security.TenantContext;
import com.realestate.emi.specification.DateRangeSpec;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceBillService {

    private final MaintenanceBillRepository billRepository;
    private final MaintenancePaymentRepository paymentRepository;
    private final FlatRepository flatRepository;
    private final ResidentRepository residentRepository;
    private final OrganizationRepository organizationRepository;
    private final MaintenanceBillMapper billMapper;
    private final TenantContext tenantContext;

    @Transactional
    public List<MaintenanceBillResponse> generateBills(MaintenanceBillGenerateRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        BigDecimal sinking = request.getSinkingFund() == null ? BigDecimal.ZERO : request.getSinkingFund();
        BigDecimal other = request.getOtherCharges() == null ? BigDecimal.ZERO : request.getOtherCharges();
        BigDecimal total = request.getBaseAmount().add(sinking).add(other);

        List<MaintenanceBill> created = new ArrayList<>();
        for (Long flatId : request.getFlatIds()) {
            Flat flat = flatRepository.findByIdAndOrganizationId(flatId, orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Flat", flatId));
            if (billRepository.findByOrganizationIdAndFlatIdAndBillMonth(orgId, flatId, request.getBillMonth()).isPresent()) {
                continue;
            }
            MaintenanceBill bill = MaintenanceBill.builder()
                    .organization(org)
                    .flat(flat)
                    .billMonth(request.getBillMonth())
                    .baseAmount(request.getBaseAmount())
                    .sinkingFund(sinking)
                    .otherCharges(other)
                    .lateFee(BigDecimal.ZERO)
                    .totalAmount(total)
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(request.getDueDate())
                    .status(MaintenanceBillStatus.DUE)
                    .generatedAt(LocalDateTime.now())
                    .build();
            created.add(billRepository.save(bill));
        }
        log.info("Generated {} bills for month={}", created.size(), request.getBillMonth());
        return created.stream().map(billMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceBillResponse> listAdmin(Long blockId, Long flatId, MaintenanceBillStatus status,
                                                   String billMonth, LocalDate from, LocalDate to) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Specification<MaintenanceBill> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("organization").get("id"), orgId));
            if (flatId != null) ps.add(cb.equal(root.get("flat").get("id"), flatId));
            if (blockId != null) ps.add(cb.equal(root.get("flat").get("block").get("id"), blockId));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (billMonth != null) ps.add(cb.equal(root.get("billMonth"), billMonth));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        spec = spec.and(DateRangeSpec.dateRange("dueDate", from, to));
        return billRepository.findAll(spec).stream()
                .sorted((a, b) -> b.getBillMonth().compareTo(a.getBillMonth()))
                .map(billMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaintenanceBillResponse findByIdAdmin(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        MaintenanceBill bill = billRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceBill", id));
        return toResponseWithPayments(bill);
    }

    @Transactional
    public MaintenancePaymentResponse adminRecordPayment(Long billId, MaintenancePaymentRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        MaintenanceBill bill = billRepository.findByIdAndOrganizationId(billId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceBill", billId));
        return recordPayment(bill, request, null);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceBillResponse> listForResident() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        if (flatId == null) {
            throw new ServiceException("Resident flat context missing", "RESIDENT_CONTEXT_MISSING");
        }
        return billRepository.findByOrganizationIdAndFlatIdOrderByBillMonthDesc(orgId, flatId).stream()
                .map(billMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaintenanceBillResponse findByIdForResident(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        MaintenanceBill bill = billRepository.findByIdAndOrganizationIdAndFlatId(id, orgId, flatId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceBill", id));
        return toResponseWithPayments(bill);
    }

    @Transactional
    public MaintenancePaymentResponse residentPay(Long billId, MaintenancePaymentRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        Long residentId = tenantContext.getCurrentResidentId();
        MaintenanceBill bill = billRepository.findByIdAndOrganizationIdAndFlatId(billId, orgId, flatId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceBill", billId));
        // TODO: integrate Razorpay verification before recording payment
        if (request.getPaymentMethod() == null) {
            request.setPaymentMethod(PaymentMethod.UPI);
        }
        return recordPayment(bill, request, residentId);
    }

    private MaintenancePaymentResponse recordPayment(MaintenanceBill bill, MaintenancePaymentRequest request, Long residentIdOverride) {
        Long orgId = bill.getOrganization().getId();
        BigDecimal outstanding = billMapper.computeOutstanding(bill);
        if (request.getAmount().compareTo(outstanding) > 0) {
            throw new ServiceException("Payment amount exceeds outstanding " + outstanding, "OVERPAYMENT");
        }
        Long residentId = residentIdOverride != null ? residentIdOverride : tenantContext.getCurrentResidentId();
        Resident resident = null;
        if (residentId != null) {
            resident = residentRepository.findByIdAndOrganizationId(residentId, orgId).orElse(null);
        }
        // Fall back: pick any resident on the flat
        if (resident == null) {
            List<Resident> flatResidents = residentRepository.findByOrganizationIdAndFlatIdOrderByFullNameAsc(orgId, bill.getFlat().getId());
            if (!flatResidents.isEmpty()) {
                resident = flatResidents.get(0);
            } else {
                throw new ServiceException("No resident on flat to attribute payment", "NO_RESIDENT");
            }
        }

        MaintenancePayment payment = MaintenancePayment.builder()
                .organization(bill.getOrganization())
                .bill(bill)
                .resident(resident)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .utrNumber(request.getUtrNumber())
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now())
                .receiptNumber(generateReceiptNumber(bill))
                .build();
        MaintenancePayment saved = paymentRepository.save(payment);

        BigDecimal newPaid = (bill.getPaidAmount() == null ? BigDecimal.ZERO : bill.getPaidAmount()).add(request.getAmount());
        bill.setPaidAmount(newPaid);
        if (newPaid.compareTo(bill.getTotalAmount()) >= 0) {
            bill.setStatus(MaintenanceBillStatus.PAID);
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            bill.setStatus(MaintenanceBillStatus.PARTIAL);
        }
        billRepository.save(bill);

        log.info("Recorded payment id={} bill={} amount={}", saved.getId(), bill.getId(), saved.getAmount());
        return billMapper.toPaymentResponse(saved);
    }

    private MaintenanceBillResponse toResponseWithPayments(MaintenanceBill bill) {
        MaintenanceBillResponse response = billMapper.toResponse(bill);
        List<MaintenancePaymentResponse> payments = paymentRepository
                .findByOrganizationIdAndBillIdOrderByPaymentDateDesc(bill.getOrganization().getId(), bill.getId())
                .stream()
                .map(billMapper::toPaymentResponse)
                .collect(Collectors.toList());
        response.setPayments(payments);
        return response;
    }

    private String generateReceiptNumber(MaintenanceBill bill) {
        return "MNT/" + bill.getBillMonth() + "/" + bill.getFlat().getId() + "/" + System.currentTimeMillis();
    }
}
