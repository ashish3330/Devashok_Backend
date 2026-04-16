package com.realestate.emi.service;

import com.realestate.emi.dto.request.SupplierPaymentRequest;
import com.realestate.emi.dto.response.SupplierPaymentResponse;
import com.realestate.emi.dto.response.SupplierPaymentSummaryResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Supplier;
import com.realestate.emi.entity.SupplierPayment;
import com.realestate.emi.enums.PaymentMethod;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.StockTransactionRepository;
import com.realestate.emi.repository.SupplierPaymentRepository;
import com.realestate.emi.repository.SupplierRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierPaymentService {

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierRepository supplierRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final TenantContext tenantContext;

    @Transactional
    public SupplierPaymentResponse recordPayment(Long supplierId, SupplierPaymentRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));

        if (supplier.getOrganization() != null && !supplier.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Supplier", supplierId);
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("Payment amount must be greater than zero", "INVALID_AMOUNT");
        }

        PaymentMethod method = null;
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()) {
            try {
                method = PaymentMethod.valueOf(request.getPaymentMethod());
            } catch (IllegalArgumentException e) {
                throw new ServiceException("Invalid payment method: " + request.getPaymentMethod(), "INVALID_PAYMENT_METHOD");
            }
        }

        Organization org = supplier.getOrganization();

        SupplierPayment payment = SupplierPayment.builder()
                .supplier(supplier)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now())
                .paymentMethod(method)
                .referenceNumber(request.getReferenceNumber())
                .remarks(request.getRemarks())
                .organization(org)
                .build();

        payment = supplierPaymentRepository.save(payment);
        log.info("Recorded supplier payment {} of amount {} for supplier {}", payment.getId(), payment.getAmount(), supplierId);

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<SupplierPaymentResponse> getPaymentsBySupplier(Long supplierId) {
        Long orgId = tenantContext.getCurrentOrganizationId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));

        if (supplier.getOrganization() != null && !supplier.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Supplier", supplierId);
        }

        return supplierPaymentRepository.findBySupplierIdAndOrganizationIdOrderByPaymentDateDesc(supplierId, orgId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaid(Long supplierId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return supplierPaymentRepository.sumTotalPaidBySupplier(supplierId, orgId);
    }

    @Transactional(readOnly = true)
    public SupplierPaymentSummaryResponse getPaymentSummary(Long supplierId) {
        Long orgId = tenantContext.getCurrentOrganizationId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));

        if (supplier.getOrganization() != null && !supplier.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Supplier", supplierId);
        }

        BigDecimal totalPaid = supplierPaymentRepository.sumTotalPaidBySupplier(supplierId, orgId);
        BigDecimal totalInwardCost = stockTransactionRepository.sumInwardCostBySupplier(supplierId, orgId);
        BigDecimal outstanding = totalInwardCost.subtract(totalPaid);

        return SupplierPaymentSummaryResponse.builder()
                .totalInwardCost(totalInwardCost)
                .totalPaid(totalPaid)
                .outstandingBalance(outstanding)
                .build();
    }

    private SupplierPaymentResponse toResponse(SupplierPayment payment) {
        return SupplierPaymentResponse.builder()
                .id(payment.getId())
                .supplierId(payment.getSupplier().getId())
                .supplierName(payment.getSupplier().getName())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                .referenceNumber(payment.getReferenceNumber())
                .remarks(payment.getRemarks())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
