package com.realestate.emi.service;

import com.realestate.emi.dto.request.CreateUserRequest;
import com.realestate.emi.dto.response.UserResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Staff;
import com.realestate.emi.entity.User;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.StaffRepository;
import com.realestate.emi.repository.UserRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return userRepository.findByOrganizationId(orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ServiceException("Username already exists", "DUPLICATE_USERNAME");
        }
        if (request.getMobile() != null && userRepository.findByMobile(request.getMobile()).isPresent()) {
            throw new ServiceException("Mobile number already registered", "DUPLICATE_MOBILE");
        }

        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .mobile(request.getMobile())
                .role(request.getRole())
                .organization(org)
                .build();
        user = userRepository.save(user);

        // Link to staff if staffId provided
        if (request.getStaffId() != null) {
            Staff staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff", request.getStaffId()));
            staff.setUser(user);
            staffRepository.save(staff);
        }

        log.info("Created user: {} with role {}", user.getUsername(), user.getRole());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, CreateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (user.getOrganization() != null && !user.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("User", id);
        }

        user.setFullName(request.getFullName());
        user.setMobile(request.getMobile());
        user.setRole(request.getRole());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user = userRepository.save(user);
        log.info("Updated user: {}", user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (user.getOrganization() != null && !user.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("User", id);
        }
        if (user.getUsername().equals(currentUsername)) {
            throw new ServiceException("You cannot delete your own account", "SELF_DELETE_NOT_ALLOWED");
        }
        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    private UserResponse toResponse(User user) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .mobile(user.getMobile())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt());

        // Check if any staff is linked
        staffRepository.findByUserId(user.getId())
                .ifPresent(staff -> {
                    builder.staffId(staff.getId());
                    builder.staffName(staff.getFullName());
                });

        return builder.build();
    }
}
