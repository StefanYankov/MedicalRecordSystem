package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.TreatmentRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.TreatmentCreateDTO;
import nbu.cscb869.services.data.dtos.TreatmentUpdateDTO;
import nbu.cscb869.services.data.dtos.TreatmentViewDTO;
import nbu.cscb869.services.services.contracts.TreatmentService;
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

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link TreatmentService} for managing treatment operations.
 */
@Service
public class TreatmentServiceImpl implements TreatmentService {
    private static final Logger logger = LoggerFactory.getLogger(TreatmentServiceImpl.class);
    private static final String ENTITY_NAME = "Treatment";
    private static final int MAX_PAGE_SIZE = 100;

    private final TreatmentRepository treatmentRepository;
    private final VisitRepository visitRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new TreatmentServiceImpl with the specified dependencies.
     *
     * @param treatmentRepository the repository for treatment entities
     * @param visitRepository     the repository for visit entities
     * @param modelMapper         the ModelMapper for DTO conversions
     */
    public TreatmentServiceImpl(TreatmentRepository treatmentRepository, VisitRepository visitRepository, ModelMapper modelMapper) {
        this.treatmentRepository = treatmentRepository;
        this.visitRepository = visitRepository;
        this.modelMapper = modelMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TreatmentViewDTO create(TreatmentCreateDTO dto) {
        validateDtoNotNull(dto, "create");
        logger.debug("Creating {} for Visit ID: {}", ENTITY_NAME, dto.getVisitId());

        Visit visit = visitRepository.findById(dto.getVisitId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatVisitNotFoundById(dto.getVisitId())));

        Treatment treatment = new Treatment();
        treatment.setDescription(dto.getDescription());
        treatment.setVisit(visit);

        if (dto.getMedicines() != null) {
            dto.getMedicines().forEach(medDto -> {
                Medicine medicine = new Medicine();
                medicine.setName(medDto.getName());
                medicine.setDosage(medDto.getDosage());
                medicine.setFrequency(medDto.getFrequency());
                medicine.setTreatment(treatment); // Link medicine to the treatment
                treatment.getMedicines().add(medicine);
            });
        }

        Treatment savedTreatment = treatmentRepository.save(treatment);
        logger.info("Created {} with ID: {}", ENTITY_NAME, savedTreatment.getId());
        return modelMapper.map(savedTreatment, TreatmentViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TreatmentViewDTO update(TreatmentUpdateDTO dto) {
        validateDtoNotNull(dto, "update");
        validateIdNotNull(dto.getId(), "update");
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        Treatment treatment = treatmentRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatTreatmentNotFoundById(dto.getId())));

        Visit visit = visitRepository.findById(dto.getVisitId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatVisitNotFoundById(dto.getVisitId())));

        treatment.setDescription(dto.getDescription());
        treatment.setVisit(visit);

        // Reconcile medicines: clear the old list and add the new one
        treatment.getMedicines().clear();
        if (dto.getMedicines() != null) {
            dto.getMedicines().forEach(medDto -> {
                Medicine medicine = new Medicine();
                medicine.setName(medDto.getName());
                medicine.setDosage(medDto.getDosage());
                medicine.setFrequency(medDto.getFrequency());
                medicine.setTreatment(treatment);
                treatment.getMedicines().add(medicine);
            });
        }

        Treatment savedTreatment = treatmentRepository.save(treatment);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, savedTreatment.getId());
        return modelMapper.map(savedTreatment, TreatmentViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        validateIdNotNull(id, "delete");
        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);

        if (!treatmentRepository.existsById(id)) {
            throw new EntityNotFoundException(ExceptionMessages.formatTreatmentNotFoundById(id));
        }

        treatmentRepository.deleteById(id);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public TreatmentViewDTO getById(Long id) {
        validateIdNotNull(id, "getById");
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatTreatmentNotFoundById(id)));

        // Security check for patients
        String currentUserKeycloakId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))) {
            if (!treatment.getVisit().getPatient().getKeycloakId().equals(currentUserKeycloakId)) {
                logger.warn("Patient {} attempted to access treatment {} belonging to another patient.", currentUserKeycloakId, id);
                throw new AccessDeniedException("Patients can only view their own treatment records.");
            }
        }

        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(treatment, TreatmentViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<TreatmentViewDTO>> getAll(int page, int size, String orderBy, boolean ascending) {
        validatePagination(page, size, "getAll");
        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}", ENTITY_NAME, page, size, orderBy, ascending);

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Treatment> treatments = treatmentRepository.findAll(pageable);

        Page<TreatmentViewDTO> result = treatments.map(t -> modelMapper.map(t, TreatmentViewDTO.class));
        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
        return CompletableFuture.completedFuture(result);
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