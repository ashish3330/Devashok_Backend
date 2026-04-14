package com.realestate.emi.repository;

import com.realestate.emi.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    List<StockAlert> findByAcknowledgedFalseOrderByCreatedAtDesc();

    List<StockAlert> findByMaterialIdOrderByCreatedAtDesc(Long materialId);
}
