package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link SickLeaveService} for managing sick leave operations.
 */
@Service
public class SickLeaveServiceImpl implements SickLeaveService {
    private static final Logger logger = LoggerFactory.getLogger(SickLeaveServiceImpl.class);
    private static final String ENTITY_NAME = "Sick Leave";
    private static final int MAX_PAGE_SIZE = 100;

    private final SickLeaveRepository sickLeaveRepository;
    private final VisitRepository visitRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new SickLeaveServiceImpl with the specified dependencies.
     *
     * @param sickLeaveRepository the repository for sick leave entities
     * @param visitRepository     the repository for visit entities
     * @param modelMapper         the ModelMapper for DTO conversions
     */
    public SickLeaveServiceImpl(SickLeaveRepository sickLeaveRepository, VisitRepository visitRepository, ModelMapper modelMapper) {
        this.sickLeaveRepository = sickLeaveRepository;
        this.visitRepository = visitRepository;
        this.modelMapper = modelMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SickLeaveViewDTO create(SickLeaveCreateDTO dto) {
        validateDtoNotNull(dto, "create");
        logger.debug("Creating {} for Visit ID: {}", ENTITY_NAME, dto.getVisitId());

        Visit visit = visitRepository.findById(dto.getVisitId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatVisitNotFoundById(dto.getVisitId())));

        SickLeave sickLeave = new SickLeave();
        sickLeave.setVisit(visit);
        sickLeave.setStartDate(dto.getStartDate());
        sickLeave.setDurationDays(dto.getDurationDays());

        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);
        logger.info("Created {} with ID: {}", ENTITY_NAME, savedSickLeave.getId());
        return modelMapper.map(savedSickLeave, SickLeaveViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SickLeaveViewDTO update(SickLeaveUpdateDTO dto) {
        validateDtoNotNull(dto, "update");
        validateIdNotNull(dto.getId(), "update");
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        SickLeave sickLeave = sickLeaveRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatSickLeaveNotFoundById(dto.getId())));

        Visit visit = visitRepository.findById(dto.getVisitId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatVisitNotFoundById(dto.getVisitId())));

        sickLeave.setVisit(visit);
        sickLeave.setStartDate(dto.getStartDate());
        sickLeave.setDurationDays(dto.getDurationDays());

        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, savedSickLeave.getId());
        return modelMapper.map(savedSickLeave, SickLeaveViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        validateIdNotNull(id, "delete");
        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);

        SickLeave sickLeave = sickLeaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatSickLeaveNotFoundById(id)));

        Visit visit = sickLeave.getVisit();
        if (visit != null) {
            visit.setSickLeave(null);
            visitRepository.save(visit);
        }

        sickLeaveRepository.delete(sickLeave);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public SickLeaveViewDTO getById(Long id) {
        validateIdNotNull(id, "getById");
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        SickLeave sickLeave = sickLeaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatSickLeaveNotFoundById(id)));

        // Security check for patients
        String currentUserKeycloakId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))) {
            if (!sickLeave.getVisit().getPatient().getKeycloakId().equals(currentUserKeycloakId)) {
                logger.warn("Patient {} attempted to access sick leave {} belonging to another patient.", currentUserKeycloakId, id);
                throw new AccessDeniedException("Patients can only view their own sick leave records.");
            }
        }

        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(sickLeave, SickLeaveViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<SickLeaveViewDTO>> getAll(int page, int size, String orderBy, boolean ascending) {
        validatePagination(page, size, "getAll");
        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}", ENTITY_NAME, page, size, orderBy, ascending);

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SickLeave> sickLeaves = sickLeaveRepository.findAll(pageable);

        Page<SickLeaveViewDTO> result = sickLeaves.map(sl -> modelMapper.map(sl, SickLeaveViewDTO.class));
        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
        return CompletableFuture.completedFuture(result);
    }

    /** {@inheritDoc} */
    @Override
    public List<YearMonthSickLeaveCountDTO> getMonthsWithMostSickLeaves() {
        logger.debug("Retrieving months with most sick leaves");
        List<YearMonthSickLeaveCountDTO> result = sickLeaveRepository.findYearMonthWithMostSickLeaves();
        logger.info("Retrieved {} months with sick leave counts", result.size());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public long getTotalSickLeavesCount() {
        logger.debug("Retrieving total count of sick leaves.");
        return sickLeaveRepository.count();
    }

    private void validateDtoNotNull(Object dto, String operation) {
        if (dto == null) {
            logger.error("Cannot {} {}: DTO is null", operation, ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull(ENTITY_NAME));
        }
    }

    private void validateIdNotNull(Long id, String operation) {
        if (id == null) {
            logger.error("Cannot {} {}: ID is null", operation, ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
    }

    private void validatePagination(int page, int size, String operation) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Cannot {} {}: Invalid pagination, page={}, size={}", operation, ENTITY_NAME, page, size);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("Pagination parameters"));
        }
    }
}
