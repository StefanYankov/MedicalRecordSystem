package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.SickLeaveRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import nbu.cscb869.services.services.contracts.SickLeaveService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link SickLeaveService} for managing sick leave-related operations.
 */
@Service
public class SickLeaveServiceImpl implements SickLeaveService {
    private static final Logger logger = LoggerFactory.getLogger(SickLeaveServiceImpl.class);
    private static final String ENTITY_NAME = "SickLeave";
    private static final int MAX_PAGE_SIZE = 100;

    private final SickLeaveRepository sickLeaveRepository;
    private final VisitRepository visitRepository;
    private final ModelMapper modelMapper;

    public SickLeaveServiceImpl(SickLeaveRepository sickLeaveRepository, VisitRepository visitRepository, ModelMapper modelMapper) {
        this.sickLeaveRepository = sickLeaveRepository;
        this.visitRepository = visitRepository;
        this.modelMapper = modelMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SickLeaveViewDTO create(SickLeaveCreateDTO dto) {
        if (dto == null) {
            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("SickLeaveCreateDTO"));
        }
        logger.debug("Creating {} for visit ID: {}", ENTITY_NAME, dto.getVisitId());

        Visit visit = visitRepository.findById(dto.getVisitId())
                .orElseThrow(() -> {
                    logger.warn("No Visit found with ID: {}", dto.getVisitId());
                    return new EntityNotFoundException("Visit not found with ID: " + dto.getVisitId());
                });

        SickLeave sickLeave = modelMapper.map(dto, SickLeave.class);
        sickLeave.setVisit(visit);
        sickLeave = sickLeaveRepository.save(sickLeave);
        logger.info("Created {} with ID: {}", ENTITY_NAME, sickLeave.getId());
        return modelMapper.map(sickLeave, SickLeaveViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SickLeaveViewDTO update(SickLeaveUpdateDTO dto) {
        if (dto == null) {
            logger.error("Cannot update {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("SickLeaveUpdateDTO"));
        }
        if (dto.getId() == null) {
            logger.error("Cannot update {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        SickLeave sickLeave = sickLeaveRepository.findById(dto.getId())
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, dto.getId());
                    return new EntityNotFoundException("SickLeave not found with ID: " + dto.getId());
                });

        Visit visit = visitRepository.findById(dto.getVisitId())
                .orElseThrow(() -> {
                    logger.warn("No Visit found with ID: {}", dto.getVisitId());
                    return new EntityNotFoundException("Visit not found with ID: " + dto.getVisitId());
                });

        modelMapper.map(dto, sickLeave);
        sickLeave.setVisit(visit);
        sickLeave = sickLeaveRepository.save(sickLeave);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, sickLeave.getId());
        return modelMapper.map(sickLeave, SickLeaveViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            logger.error("Cannot delete {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);

        SickLeave sickLeave = sickLeaveRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException("SickLeave not found with ID: " + id);
                });

        sickLeaveRepository.delete(sickLeave);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public SickLeaveViewDTO getById(Long id) {
        if (id == null) {
            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        SickLeave sickLeave = sickLeaveRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException("SickLeave not found with ID: " + id);
                });
        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(sickLeave, SickLeaveViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<SickLeaveViewDTO>> getAll(int page, int size, String orderBy, boolean ascending) {
        if (page < 0) {
            logger.error("Invalid page number: {}", page);
            throw new InvalidDTOException("Page number must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid page size: {}", size);
            throw new InvalidDTOException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}", ENTITY_NAME, page, size, orderBy, ascending);

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SickLeave> sickLeaves = sickLeaveRepository.findAll(pageable);
        Page<SickLeaveViewDTO> result = sickLeaves.map(sl -> modelMapper.map(sl, SickLeaveViewDTO.class));

        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
        return CompletableFuture.completedFuture(result);
    }
}