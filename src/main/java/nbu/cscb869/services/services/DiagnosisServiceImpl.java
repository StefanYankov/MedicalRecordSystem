package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
public class DiagnosisServiceImpl implements DiagnosisService {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosisServiceImpl.class);
    private static final String ENTITY_NAME = "Diagnosis";
    private static final int MAX_PAGE_SIZE = 100;

    private final DiagnosisRepository diagnosisRepository;
    private final ModelMapper modelMapper;

    public DiagnosisServiceImpl(DiagnosisRepository diagnosisRepository, ModelMapper modelMapper) {
        this.diagnosisRepository = diagnosisRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public DiagnosisViewDTO create(DiagnosisCreateDTO dto) {
        if (dto == null) {
            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("DiagnosisCreateDTO"));
        }
        logger.debug("Creating {} with name: {}", ENTITY_NAME, dto.getName());

        Diagnosis diagnosis = modelMapper.map(dto, Diagnosis.class);
        diagnosis = diagnosisRepository.save(diagnosis);
        logger.info("Created {} with ID: {}", ENTITY_NAME, diagnosis.getId());
        return modelMapper.map(diagnosis, DiagnosisViewDTO.class);
    }

    @Override
    @Transactional
    public DiagnosisViewDTO update(DiagnosisUpdateDTO dto) {
        if (dto == null) {
            logger.error("Cannot update {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("DiagnosisUpdateDTO"));
        }
        if (dto.getId() == null) {
            logger.error("Cannot update {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        Diagnosis diagnosis = diagnosisRepository.findById(dto.getId())
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, dto.getId());
                    return new EntityNotFoundException("Diagnosis not found with ID: " + dto.getId());
                });

        modelMapper.map(dto, diagnosis);
        diagnosis = diagnosisRepository.save(diagnosis);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, diagnosis.getId());
        return modelMapper.map(diagnosis, DiagnosisViewDTO.class);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            logger.error("Cannot delete {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);

        Diagnosis diagnosis = diagnosisRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException("Diagnosis not found with ID: " + id);
                });

        diagnosisRepository.delete(diagnosis);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    @Override
    public DiagnosisViewDTO getById(Long id) {
        if (id == null) {
            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        Diagnosis diagnosis = diagnosisRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException("Diagnosis not found with ID: " + id);
                });
        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(diagnosis, DiagnosisViewDTO.class);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Async
    public CompletableFuture<Page<DiagnosisViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
        if (page < 0) {
            logger.error("Invalid page number: {}", page);
            throw new InvalidDTOException("Page number must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid page size: {}", size);
            throw new InvalidDTOException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}, filter={}", ENTITY_NAME, page, size, orderBy, ascending, filter);

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Diagnosis> diagnoses;
        if (filter == null || filter.trim().isEmpty()) {
            logger.debug("Fetching all active diagnoses");
            diagnoses = diagnosisRepository.findAllActive(pageable);
        } else {
            logger.debug("Fetching diagnoses with name containing: {}", filter.trim());
            diagnoses = diagnosisRepository.findByNameContainingIgnoreCase(filter.trim(), pageable);
        }
        Page<DiagnosisViewDTO> result = diagnoses.map(d -> modelMapper.map(d, DiagnosisViewDTO.class));

        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
        return CompletableFuture.completedFuture(result);
    }
}