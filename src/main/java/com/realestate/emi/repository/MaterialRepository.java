package com.realestate.emi.repository;

import com.realestate.emi.entity.Material;
import com.realestate.emi.enums.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByIsActiveTrueOrderByNameAsc();

    List<Material> findByOrganizationIdAndIsActiveTrueOrderByNameAsc(Long orgId);

    List<Material> findByCategoryAndIsActiveTrue(MaterialCategory category);

    @Query("SELECT m FROM Material m WHERE m.isActive = true AND m.currentQuantity <= m.reorderLevel")
    List<Material> findLowStockMaterials();

    @Query("SELECT m FROM Material m WHERE m.isActive = true AND m.currentQuantity = 0")
    List<Material> findOutOfStockMaterials();

    @Query("SELECT COALESCE(SUM(m.currentQuantity * m.unitCost), 0) FROM Material m WHERE m.isActive = true")
    BigDecimal calculateTotalInventoryValue();

    @Query("SELECT m FROM Material m WHERE m.isActive = true AND m.currentQuantity <= m.reorderLevel AND m.organization.id = :orgId")
    List<Material> findLowStockMaterialsByOrg(@Param("orgId") Long orgId);

    @Query("SELECT m FROM Material m WHERE m.isActive = true AND m.currentQuantity = 0 AND m.organization.id = :orgId")
    List<Material> findOutOfStockMaterialsByOrg(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(m.currentQuantity * m.unitCost), 0) FROM Material m WHERE m.isActive = true AND m.organization.id = :orgId")
    BigDecimal calculateTotalInventoryValueByOrg(@Param("orgId") Long orgId);

    List<Material> findByCategoryAndOrganizationIdAndIsActiveTrue(MaterialCategory category, Long orgId);

    List<Material> findByIdInAndOrganizationId(List<Long> ids, Long orgId);
}
