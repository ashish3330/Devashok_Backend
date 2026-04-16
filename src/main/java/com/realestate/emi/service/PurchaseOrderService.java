package com.realestate.emi.service;

import com.realestate.emi.dto.request.PurchaseOrderItemRequest;
import com.realestate.emi.dto.request.PurchaseOrderReceiveRequest;
import com.realestate.emi.dto.request.PurchaseOrderRequest;
import com.realestate.emi.dto.response.PurchaseOrderResponse;
import com.realestate.emi.entity.Material;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.PurchaseOrder;
import com.realestate.emi.entity.PurchaseOrderItem;
import com.realestate.emi.entity.StockTransaction;
import com.realestate.emi.entity.Supplier;
import com.realestate.emi.enums.PurchaseOrderStatus;
import com.realestate.emi.enums.TransactionType;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.MaterialRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.PurchaseOrderRepository;
import com.realestate.emi.repository.StockTransactionRepository;
import com.realestate.emi.repository.SupplierRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final OrganizationRepository organizationRepository;
    private final StockTransactionRepository transactionRepository;
    private final MaterialService materialService;
    private final TenantContext tenantContext;

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", request.getSupplierId()));
        if (supplier.getOrganization() != null && !supplier.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Supplier", request.getSupplierId());
        }

        String poNumber = generatePoNumber(org);

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(poNumber)
                .supplier(supplier)
                .orderDate(request.getOrderDate())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status(PurchaseOrderStatus.DRAFT)
                .remarks(request.getRemarks())
                .organization(org)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            Material material = materialRepository.findById(itemReq.getMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Material", itemReq.getMaterialId()));
            if (material.getOrganization() != null && !material.getOrganization().getId().equals(orgId)) {
                throw new ResourceNotFoundException("Material", itemReq.getMaterialId());
            }

            BigDecimal itemTotal = itemReq.getUnitCost().multiply(itemReq.getQuantity());
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .material(material)
                    .quantity(itemReq.getQuantity())
                    .unitCost(itemReq.getUnitCost())
                    .totalCost(itemTotal)
                    .receivedQuantity(BigDecimal.ZERO)
                    .remarks(itemReq.getRemarks())
                    .build();
            po.getItems().add(item);
            totalAmount = totalAmount.add(itemTotal);
        }
        po.setTotalAmount(totalAmount);

        po = poRepository.save(po);
        log.info("Purchase Order created: {} for supplier {}", poNumber, supplier.getName());
        return toResponse(po);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findAll(PurchaseOrderStatus status) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<PurchaseOrder> orders;
        if (status != null) {
            orders = poRepository.findByStatusAndOrganizationId(status, orgId);
        } else {
            orders = poRepository.findByOrganizationIdOrderByOrderDateDesc(orgId);
        }
        return orders.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse findById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        PurchaseOrder po = poRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));
        return toResponse(po);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findBySupplier(Long supplierId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return poRepository.findBySupplierIdAndOrganizationIdOrderByOrderDateDesc(supplierId, orgId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PurchaseOrderResponse approve(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        PurchaseOrder po = poRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));

        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ServiceException("Only DRAFT purchase orders can be approved", "INVALID_PO_STATUS");
        }

        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedBy(tenantContext.getCurrentUsername());
        po.setApprovedDate(LocalDate.now());
        po = poRepository.save(po);

        log.info("Purchase Order {} approved by {}", po.getPoNumber(), po.getApprovedBy());
        return toResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse receiveItems(Long id, PurchaseOrderReceiveRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        PurchaseOrder po = poRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));

        if (po.getStatus() != PurchaseOrderStatus.APPROVED && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ServiceException("Only APPROVED or PARTIALLY_RECEIVED purchase orders can receive items", "INVALID_PO_STATUS");
        }

        Map<Long, PurchaseOrderItem> itemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, item -> item));

        String username = tenantContext.getCurrentUsername();

        for (PurchaseOrderReceiveRequest.ReceiveItem receiveItem : request.getItems()) {
            PurchaseOrderItem poItem = itemMap.get(receiveItem.getItemId());
            if (poItem == null) {
                throw new ResourceNotFoundException("PurchaseOrderItem", receiveItem.getItemId());
            }

            BigDecimal newReceived = receiveItem.getReceivedQuantity();
            if (newReceived == null || newReceived.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal totalAfterReceive = poItem.getReceivedQuantity().add(newReceived);
            if (totalAfterReceive.compareTo(poItem.getQuantity()) > 0) {
                throw new ServiceException(
                        "Received quantity exceeds ordered quantity for " + poItem.getMaterial().getName(),
                        "EXCESS_RECEIVED");
            }

            poItem.setReceivedQuantity(totalAfterReceive);

            // Create INWARD stock transaction for received quantity
            Material material = poItem.getMaterial();
            material.setCurrentQuantity(material.getCurrentQuantity().add(newReceived));
            materialRepository.save(material);

            StockTransaction txn = StockTransaction.builder()
                    .material(material)
                    .type(TransactionType.INWARD)
                    .quantity(newReceived)
                    .unitCostAtTime(poItem.getUnitCost())
                    .totalCost(poItem.getUnitCost().multiply(newReceived))
                    .supplier(po.getSupplier())
                    .referenceNumber(po.getPoNumber())
                    .remarks("Goods receipt against PO " + po.getPoNumber())
                    .transactionDate(LocalDateTime.now())
                    .transactedBy(username)
                    .build();
            transactionRepository.save(txn);

            materialService.checkAndCreateAlerts(material);

            log.info("Received {} of {} against PO {}", newReceived, material.getName(), po.getPoNumber());
        }

        // Determine PO status based on received quantities
        boolean allFullyReceived = po.getItems().stream()
                .allMatch(item -> item.getReceivedQuantity().compareTo(item.getQuantity()) >= 0);
        boolean anyReceived = po.getItems().stream()
                .anyMatch(item -> item.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0);

        if (allFullyReceived) {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
        } else if (anyReceived) {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        po = poRepository.save(po);
        return toResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse cancel(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        PurchaseOrder po = poRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));

        if (po.getStatus() != PurchaseOrderStatus.DRAFT && po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new ServiceException("Only DRAFT or APPROVED purchase orders can be cancelled", "INVALID_PO_STATUS");
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        po = poRepository.save(po);

        log.info("Purchase Order {} cancelled", po.getPoNumber());
        return toResponse(po);
    }

    private String generatePoNumber(Organization org) {
        String prefix = "PO-" + org.getCode() + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        String maxPoNumber = poRepository.findMaxPoNumberByPrefix(org.getId(), prefix);

        int nextSeq = 1;
        if (maxPoNumber != null) {
            String seqPart = maxPoNumber.substring(prefix.length());
            try {
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                nextSeq = 1;
            }
        }
        return prefix + String.format("%03d", nextSeq);
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        List<PurchaseOrderResponse.PurchaseOrderItemResponse> itemResponses = po.getItems().stream()
                .map(item -> PurchaseOrderResponse.PurchaseOrderItemResponse.builder()
                        .id(item.getId())
                        .materialId(item.getMaterial().getId())
                        .materialName(item.getMaterial().getName())
                        .category(item.getMaterial().getCategory().name())
                        .unit(item.getMaterial().getUnit().name())
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .totalCost(item.getTotalCost())
                        .receivedQuantity(item.getReceivedQuantity())
                        .remarks(item.getRemarks())
                        .build())
                .collect(Collectors.toList());

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier().getId())
                .supplierName(po.getSupplier().getName())
                .orderDate(po.getOrderDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .remarks(po.getRemarks())
                .approvedBy(po.getApprovedBy())
                .approvedDate(po.getApprovedDate())
                .items(itemResponses)
                .createdAt(po.getCreatedAt())
                .build();
    }
}
