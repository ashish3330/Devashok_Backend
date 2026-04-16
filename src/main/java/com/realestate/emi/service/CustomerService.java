package com.realestate.emi.service;

import com.realestate.emi.dto.request.CustomerRequest;
import com.realestate.emi.dto.response.CustomerResponse;
import com.realestate.emi.entity.Customer;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.CustomerMapper;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.repository.CustomerRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        log.debug("Fetching all customers");
        Long orgId = tenantContext.getCurrentOrganizationId();
        return customerRepository.findByOrganizationIdOrderByFullNameAsc(orgId).stream()
                .map(customer -> {
                    CustomerResponse resp = customerMapper.toResponse(customer);
                    maskSensitiveFields(resp);
                    return resp;
                })
                .collect(Collectors.toList());
    }

    private void maskSensitiveFields(CustomerResponse resp) {
        if (resp.getAadharNumber() != null && resp.getAadharNumber().length() == 12) {
            resp.setAadharNumber("XXXX-XXXX-" + resp.getAadharNumber().substring(8));
        }
        if (resp.getPanNumber() != null && resp.getPanNumber().length() == 10) {
            resp.setPanNumber("XXXXXX" + resp.getPanNumber().substring(6));
        }
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        log.debug("Fetching customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (customer.getOrganization() != null && !customer.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Customer", id);
        }
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        log.debug("Creating customer with phone: {}", request.getPhoneNumber());

        Long orgId = tenantContext.getCurrentOrganizationId();

        if (customerRepository.existsByPhoneNumberAndOrganizationId(request.getPhoneNumber(), orgId)) {
            throw new ServiceException("Customer with phone number '" + request.getPhoneNumber() + "' already exists",
                    "DUPLICATE_PHONE");
        }

        if (StringUtils.hasText(request.getAadharNumber())
                && customerRepository.existsByAadharNumberAndOrganizationId(request.getAadharNumber(), orgId)) {
            throw new ServiceException("Customer with Aadhar number already exists", "DUPLICATE_AADHAR");
        }

        if (StringUtils.hasText(request.getPanNumber())
                && customerRepository.existsByPanNumberAndOrganizationId(request.getPanNumber(), orgId)) {
            throw new ServiceException("Customer with PAN number already exists", "DUPLICATE_PAN");
        }

        Customer customer = customerMapper.toEntity(request);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        customer.setOrganization(org);
        Customer saved = customerRepository.save(customer);
        log.info("Created customer with id: {}", saved.getId());
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        log.debug("Updating customer with id: {}", id);
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (existing.getOrganization() != null && !existing.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Customer", id);
        }

        if (!existing.getPhoneNumber().equals(request.getPhoneNumber())
                && customerRepository.existsByPhoneNumberAndIdNotAndOrganizationId(request.getPhoneNumber(), id, orgId)) {
            throw new ServiceException("Customer with phone number '" + request.getPhoneNumber() + "' already exists",
                    "DUPLICATE_PHONE");
        }

        if (StringUtils.hasText(request.getAadharNumber())
                && !request.getAadharNumber().equals(existing.getAadharNumber())
                && customerRepository.existsByAadharNumberAndIdNotAndOrganizationId(request.getAadharNumber(), id, orgId)) {
            throw new ServiceException("Customer with Aadhar number already exists", "DUPLICATE_AADHAR");
        }

        if (StringUtils.hasText(request.getPanNumber())
                && !request.getPanNumber().equals(existing.getPanNumber())
                && customerRepository.existsByPanNumberAndIdNotAndOrganizationId(request.getPanNumber(), id, orgId)) {
            throw new ServiceException("Customer with PAN number already exists", "DUPLICATE_PAN");
        }

        customerMapper.updateEntityFromRequest(request, existing);
        Customer saved = customerRepository.save(existing);
        log.info("Updated customer with id: {}", saved.getId());
        return customerMapper.toResponse(saved);
    }
}
