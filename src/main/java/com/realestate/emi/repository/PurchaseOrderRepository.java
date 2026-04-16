package com.realestate.emi.repository;

import com.realestate.emi.entity.PurchaseOrder;
import com.realestate.emi.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    List<PurchaseOrder> findByOrganizationIdOrderByOrderDateDesc(Long orgId);

    List<PurchaseOrder> findBySupplierIdAndOrganizationIdOrderByOrderDateDesc(Long supplierId, Long orgId);

    List<PurchaseOrder> findByStatusAndOrganizationId(PurchaseOrderStatus status, Long orgId);

    Optional<PurchaseOrder> findByPoNumberAndOrganizationId(String poNumber, Long orgId);

    Optional<PurchaseOrder> findByIdAndOrganizationId(Long id, Long orgId);

    @Query("SELECT MAX(po.poNumber) FROM PurchaseOrder po WHERE po.organization.id = :orgId AND po.poNumber LIKE :prefix%")
    String findMaxPoNumberByPrefix(@Param("orgId") Long orgId, @Param("prefix") String prefix);
}
