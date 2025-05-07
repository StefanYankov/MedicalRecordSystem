package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.MedicineRepository;
import nbu.cscb869.data.repositories.TreatmentRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.TreatmentCreateDTO;
import nbu.cscb869.services.data.dtos.TreatmentUpdateDTO;
import nbu.cscb869.services.data.dtos.TreatmentViewDTO;
import nbu.cscb869.services.services.contracts.TreatmentService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link TreatmentService} for managing treatment-related operations.
 */
@Service
public class TreatmentServiceImpl implements TreatmentService {
    private static final Logger logger = LoggerFactory.getLogger(TreatmentServiceImpl.class);
    private static final String ENTITY_NAME = "Treatment";
    private static final int MAX_PAGE_SIZE = 100;

    private final TreatmentRepository treatmentRepository;
    private final VisitRepository visitRepository;
    private final MedicineRepository medicineRepository;
    private final ModelMapper modelMapper;

    public TreatmentServiceImpl(TreatmentRepository treatmentRepository, VisitRepository visitRepository,
                                MedicineRepository medicineRepository, ModelMapper modelMapper) {
        this.treatmentRepository = treatmentRepository;
        this.visitRepository = visitRepository;
        this.medicineRepository = medicineRepository;
        this.modelMapper = modelMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TreatmentViewDTO create(TreatmentCreateDTO dto) {
        if (dto == null) {
            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("TreatmentCreateDTO"));
        }
        logger.debug("Creating {} for visit ID: {}", ENTITY_NAME, dto.getVisitId());

        Visit visit = visitRepository.findById(dto.getVisitId())
                .orElseThrow(() -> {
                    logger.warn("No Visit found with ID: {}", dto.getVisitId());
                    return new EntityNotFoundException("Visit not found with ID: " + dto.getVisitId());
                });

        List<Medicine> medicines = validateAndGetMedicines(dto.getMedicineIds());

        Treatment treatment = modelMapper.map(dto, Treatment.class);
        treatment.setVisit(visit);
        treatment.setMedicines(medicines);
        treatment = treatmentRepository.save(treatment);
        logger.info("Created {} with ID: {}", ENTITY_NAME, treatment.getId());
        return modelMapper.map(treatment, TreatmentViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TreatmentViewDTO update(TreatmentUpdateDTO dto) {
        if (dto == null) {
            logger.error("Cannot update {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("TreatmentUpdateDTO"));
        }
        if (dto.getId() == null) {
            logger.error("Cannot update {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        Treatment treatment = treatmentRepository.findById(dto.getId())
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, dto.getId());
                    return new EntityNotFoundException("Treatment not found with ID: " + dto.getId());
                });

        Visit visit = visitRepository.findById(dto.getVisitId())
                .orElseThrow(() -> {
                    logger.warn("No Visit found with ID: {}", dto.getVisitId());
                    return new EntityNotFoundException("Visit not found with ID: " + dto.getVisitId());
                });

        List<Medicine> medicines = validateAndGetMedicines(dto.getMedicineIds());

        modelMapper.map(dto, treatment);
        treatment.setVisit(visit);
        treatment.setMedicines(medicines);
        treatment = treatmentRepository.save(treatment);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, treatment.getId());
        return modelMapper.map(treatment, TreatmentViewDTO.class);
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

        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException("Treatment not found with ID: " + id);
                });

        treatmentRepository.delete(treatment);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public TreatmentViewDTO getById(Long id) {
        if (id == null) {
            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException("Treatment not found with ID: " + id);
                });
        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(treatment, TreatmentViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<TreatmentViewDTO>> getAll(int page, int size, String orderBy, boolean ascending) {
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
        Page<Treatment> treatments = treatmentRepository.findAll(pageable);
        Page<TreatmentViewDTO> result = treatments.map(t -> modelMapper.map(t, TreatmentViewDTO.class));

        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
        return CompletableFuture.completedFuture(result);
    }

    private List<Medicine> validateAndGetMedicines(List<Long> medicineIds) {
        if (medicineIds == null || medicineIds.isEmpty()) {
            return List.of();
        }
        List<Medicine> medicines = medicineRepository.findAllById(medicineIds);
        if (medicines.size() != medicineIds.size()) {
            logger.error("Invalid medicines provided: some IDs do not exist");
            throw new InvalidDTOException("One or more medicine IDs are invalid");
        }
        return medicines;
    }
}