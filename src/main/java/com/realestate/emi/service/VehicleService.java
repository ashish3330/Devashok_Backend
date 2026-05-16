package com.realestate.emi.service;

import com.realestate.emi.dto.request.VehicleRequest;
import com.realestate.emi.dto.response.VehicleResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Resident;
import com.realestate.emi.entity.Vehicle;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.VehicleMapper;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.ResidentRepository;
import com.realestate.emi.repository.VehicleRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;
    private final ResidentRepository residentRepository;

    @Transactional(readOnly = true)
    public List<VehicleResponse> listForResident() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        if (residentId == null) {
            throw new ServiceException("Resident context missing", "RESIDENT_CONTEXT_MISSING");
        }
        return vehicleRepository.findAllByResidentIdAndOrganizationId(residentId, orgId).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(vehicleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehicleResponse createForResident(VehicleRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        if (residentId == null) {
            throw new ServiceException("Resident context missing", "RESIDENT_CONTEXT_MISSING");
        }

        String normalized = normalize(request.getVehicleNumber());
        if (vehicleRepository.existsByOrganizationIdAndVehicleNumberIgnoreCase(orgId, normalized)) {
            throw new ServiceException("A vehicle with this number is already registered", "VEHICLE_DUPLICATE");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        Resident resident = residentRepository.findByIdAndOrganizationId(residentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", residentId));

        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setVehicleNumber(normalized);
        vehicle.setOrganization(org);
        vehicle.setResident(resident);
        vehicle.setIsActive(true);

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Resident {} registered vehicle id={} number={}", residentId, saved.getId(), saved.getVehicleNumber());
        return vehicleMapper.toResponse(saved);
    }

    @Transactional
    public VehicleResponse updateForResident(Long id, VehicleRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        if (residentId == null) {
            throw new ServiceException("Resident context missing", "RESIDENT_CONTEXT_MISSING");
        }

        Vehicle vehicle = vehicleRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        if (vehicle.getResident() == null || !residentId.equals(vehicle.getResident().getId())) {
            throw new ServiceException("You can only edit your own vehicles", "VEHICLE_FORBIDDEN");
        }

        String normalized = normalize(request.getVehicleNumber());
        if (!normalized.equalsIgnoreCase(vehicle.getVehicleNumber())) {
            Optional<Vehicle> dupe = vehicleRepository.findByOrganizationIdAndVehicleNumberIgnoreCase(orgId, normalized);
            if (dupe.isPresent() && !dupe.get().getId().equals(vehicle.getId())) {
                throw new ServiceException("A vehicle with this number is already registered", "VEHICLE_DUPLICATE");
            }
            vehicle.setVehicleNumber(normalized);
        }

        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setColor(request.getColor());

        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteForResident(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        if (residentId == null) {
            throw new ServiceException("Resident context missing", "RESIDENT_CONTEXT_MISSING");
        }
        Vehicle vehicle = vehicleRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        if (vehicle.getResident() == null || !residentId.equals(vehicle.getResident().getId())) {
            throw new ServiceException("You can only remove your own vehicles", "VEHICLE_FORBIDDEN");
        }
        vehicle.setIsActive(false);
        vehicleRepository.save(vehicle);
        log.info("Resident {} deactivated vehicle id={}", residentId, vehicle.getId());
    }

    private String normalize(String raw) {
        if (raw == null) return null;
        return raw.trim().toUpperCase().replaceAll("\\s+", " ");
    }
}
