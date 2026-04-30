package com.realestate.emi.service;

import com.realestate.emi.dto.request.NoticeRequest;
import com.realestate.emi.dto.response.NoticeResponse;
import com.realestate.emi.entity.Notice;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.NoticeMapper;
import com.realestate.emi.repository.NoticeRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeMapper noticeMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<NoticeResponse> findAllAdmin() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return noticeRepository.findByOrganizationIdOrderByPublishedAtDesc(orgId).stream()
                .map(noticeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoticeResponse findById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Notice notice = noticeRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        return noticeMapper.toResponse(notice);
    }

    @Transactional
    public NoticeResponse create(NoticeRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Notice notice = noticeMapper.toEntity(request);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        notice.setOrganization(org);
        notice.setPostedByUserId(tenantContext.getCurrentUserId());
        if (notice.getPublishedAt() == null) {
            notice.setPublishedAt(LocalDateTime.now());
        }
        Notice saved = noticeRepository.save(notice);
        log.info("Created notice id={} title={}", saved.getId(), saved.getTitle());
        return noticeMapper.toResponse(saved);
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Notice existing = noticeRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        noticeMapper.updateEntityFromRequest(request, existing);
        Notice saved = noticeRepository.save(existing);
        return noticeMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Notice existing = noticeRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        noticeRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> findResidentFeed() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        LocalDateTime now = LocalDateTime.now();
        return noticeRepository.findByOrganizationIdOrderByPublishedAtDesc(orgId).stream()
                .filter(n -> n.getPublishedAt() == null || !n.getPublishedAt().isAfter(now))
                .filter(n -> n.getExpiresAt() == null || n.getExpiresAt().isAfter(now))
                .map(noticeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoticeResponse findResidentById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Notice notice = noticeRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        LocalDateTime now = LocalDateTime.now();
        if (notice.getPublishedAt() != null && notice.getPublishedAt().isAfter(now)) {
            throw new ResourceNotFoundException("Notice", id);
        }
        return noticeMapper.toResponse(notice);
    }
}
