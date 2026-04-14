package com.realestate.emi.repository;

import com.realestate.emi.entity.InstallmentPlanTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallmentPlanTemplateRepository extends JpaRepository<InstallmentPlanTemplate, Long> {

    List<InstallmentPlanTemplate> findByIsActiveTrueOrderByPhaseOrderAsc();
}
