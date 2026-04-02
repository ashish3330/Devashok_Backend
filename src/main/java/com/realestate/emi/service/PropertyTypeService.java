package com.realestate.emi.service;

import com.realestate.emi.dto.request.PropertyTypeRequest;
import com.realestate.emi.dto.response.PropertyTypeResponse;
import com.realestate.emi.entity.PropertyType;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.PropertyTypeMapper;
import com.realestate.emi.repository.DealRepository;
import com.realestate.emi.repository.PropertyTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyTypeService {

    private final PropertyTypeRepository propertyTypeRepository;
    private final DealRepository dealRepository;
    private final PropertyTypeMapper propertyTypeMapper;

    @Transactional(readOnly = true)
    public List<PropertyTypeResponse> findAll() {
        log.debug("Fetching all property types");
        return propertyTypeRepository.findAll().stream()
                .map(propertyTypeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PropertyTypeResponse create(PropertyTypeRequest request) {
        log.debug("Creating property type with name: {}", request.getName());
        if (propertyTypeRepository.existsByName(request.getName())) {
            throw new ServiceException("Property type with name '" + request.getName() + "' already exists",
                    "DUPLICATE_PROPERTY_TYPE");
        }
        PropertyType propertyType = propertyTypeMapper.toEntity(request);
        PropertyType saved = propertyTypeRepository.save(propertyType);
        log.info("Created property type with id: {}", saved.getId());
        return propertyTypeMapper.toResponse(saved);
    }

    @Transactional
    public PropertyTypeResponse update(Long id, PropertyTypeRequest request) {
        log.debug("Updating property type with id: {}", id);
        PropertyType existing = propertyTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PropertyType", id));

        if (!existing.getName().equals(request.getName())
                && propertyTypeRepository.existsByName(request.getName())) {
            throw new ServiceException("Property type with name '" + request.getName() + "' already exists",
                    "DUPLICATE_PROPERTY_TYPE");
        }

        propertyTypeMapper.updateEntityFromRequest(request, existing);
        PropertyType saved = propertyTypeRepository.save(existing);
        log.info("Updated property type with id: {}", saved.getId());
        return propertyTypeMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.debug("Deleting property type with id: {}", id);
        PropertyType existing = propertyTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PropertyType", id));

        if (dealRepository.existsByPropertyTypeId(id)) {
            throw new ServiceException("Cannot delete property type because it is associated with existing deals",
                    "PROPERTY_TYPE_IN_USE");
        }

        propertyTypeRepository.delete(existing);
        log.info("Deleted property type with id: {}", id);
    }
}
