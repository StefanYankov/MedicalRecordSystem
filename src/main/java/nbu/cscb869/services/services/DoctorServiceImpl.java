package nbu.cscb869.services.services;

import jakarta.persistence.criteria.Predicate;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidDoctorException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.utility.DoctorCriteria;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service implementation for managing {@link nbu.cscb869.data.models.Doctor} entities,
 * exposing DTOs instead of entities.
 */
@Service
public class DoctorServiceImpl implements DoctorService {
    private static final Logger logger = LoggerFactory.getLogger(DoctorServiceImpl.class);
    private static final String ENTITY_NAME = "Doctor";
    private static final int MAX_PAGE_SIZE = 100;

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new DoctorServiceImpl with the specified dependencies.
     *
     * @param doctorRepository    repository for doctor-related database operations
     * @param specialtyRepository repository for specialty-related database operations
     * @param patientRepository   repository for patient-related database operations
     * @param visitRepository     repository for visit-related database operations
     * @param modelMapper         utility for mapping Doctor entities to DTOs and vice versa
     */
    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             SpecialtyRepository specialtyRepository,
                             PatientRepository patientRepository,
                             VisitRepository visitRepository,
                             ModelMapper modelMapper) {
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.modelMapper = modelMapper;
    }

    /** {@inheritDoc} */
    @Override
    public DoctorViewDTO create(DoctorCreateDTO dto) {
        if (dto == null) {
            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("DoctorCreateDTO"));
        }
        logger.debug("Creating {} with name: {}", ENTITY_NAME, dto.getName());

        // Check for duplicate uniqueIdNumber
        if (doctorRepository.findByUniqueIdNumber(dto.getUniqueIdNumber()).isPresent()) {
            logger.error("Failed to create {}: unique ID already exists", ENTITY_NAME);
            throw new InvalidDoctorException(MessageFormat.format(ExceptionMessages.DOCTOR_UNIQUE_ID_EXISTS, dto.getUniqueIdNumber()));
        }

        // Validate specialties
        Set<Specialty> specialties = validateAndGetSpecialties(dto.getSpecialtyIds());

        try {
            Doctor doctor = modelMapper.map(dto, Doctor.class);
            doctor.setSpecialties(specialties);
            Doctor created = doctorRepository.save(doctor);
            logger.info("{} created with ID: {}", ENTITY_NAME, created.getId());
            return modelMapper.map(created, DoctorViewDTO.class);
        } catch (Exception e) {
            logger.error("Failed to create {} with name: {}, cause: {}", ENTITY_NAME, dto.getName(), e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToCreateDoctor(e.getMessage()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public DoctorViewDTO update(DoctorUpdateDTO dto) {
        if (dto == null) {
            logger.error("Cannot update {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("DoctorUpdateDTO"));
        }
        if (dto.getId() == null) {
            logger.error("Cannot update {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        // Check if doctor exists
        Doctor existing = doctorRepository.findById(dto.getId())
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, dto.getId());
                    return new EntityNotFoundException(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, dto.getId()));
                });

        // Validate specialties
        Set<Specialty> specialties = validateAndGetSpecialties(dto.getSpecialtyIds());

        try {
            modelMapper.map(dto, existing);
            existing.setSpecialties(specialties);
            Doctor updated = doctorRepository.save(existing);
            logger.info("{} updated with ID: {}", ENTITY_NAME, updated.getId());
            return modelMapper.map(updated, DoctorViewDTO.class);
        } catch (Exception e) {
            logger.error("Failed to update {} with ID: {}, cause: {}", ENTITY_NAME, dto.getId(), e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToUpdateDoctor(dto.getId()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Long id) {
        if (id == null) {
            logger.error("Cannot delete {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);

        // Check if doctor exists
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, id));
                });

        // Check if doctor is a general practitioner for active patients
        if (doctor.isGeneralPractitioner() && patientRepository.countPatientsByGeneralPractitioner().stream()
                .anyMatch(dto -> dto.getDoctor().getId().equals(id) && dto.getPatientCount() > 0)) {
            logger.error("Cannot delete {} with ID: {}: doctor is a general practitioner for active patients", ENTITY_NAME, id);
            throw new InvalidDoctorException(ExceptionMessages.DOCTOR_HAS_ACTIVE_PATIENTS);
        }

        try {
            doctorRepository.delete(doctor);
            logger.info("{} deleted with ID: {}", ENTITY_NAME, id);
        } catch (Exception e) {
            logger.error("Failed to delete {} with ID: {}, cause: {}", ENTITY_NAME, id, e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToDeleteDoctor(id));
        }
    }

    /** {@inheritDoc} */
    @Override
    public DoctorViewDTO getById(Long id) {
        if (id == null) {
            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, id));
                });
        logger.info("{} retrieved with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(doctor, DoctorViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    public DoctorViewDTO getByUniqueIdNumber(String uniqueIdNumber) {
        if (uniqueIdNumber == null || uniqueIdNumber.trim().isEmpty()) {
            logger.error("Cannot retrieve {}: unique ID number is null or empty", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldEmpty("uniqueIdNumber"));
        }
        logger.debug("Retrieving {} with unique ID number: {}", ENTITY_NAME, uniqueIdNumber);

        Doctor doctor = doctorRepository.findByUniqueIdNumber(uniqueIdNumber)
                .orElseThrow(() -> {
                    logger.warn("No {} found with unique ID number: {}", ENTITY_NAME, uniqueIdNumber);
                    return new EntityNotFoundException(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_UNIQUE_ID, uniqueIdNumber));
                });
        logger.info("{} retrieved with unique ID number: {}", ENTITY_NAME, uniqueIdNumber);
        return modelMapper.map(doctor, DoctorViewDTO.class);
    }

    /** {@inheritDoc} */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Page<DoctorViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
        if (page < 0) {
            logger.error("Invalid page number: {}", page);
            throw new InvalidInputException("Page number must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid page size: {}", size);
            throw new InvalidInputException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}, filter={}", ENTITY_NAME, page, size, orderBy, ascending, filter);

        try {
            Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            Page<Doctor> doctors;
            if (filter != null && !filter.trim().isEmpty()) {
                String filterLower = "%" + filter.trim().toLowerCase() + "%";
                doctors = doctorRepository.findByNameOrUniqueIdNumberContaining(filterLower, pageRequest);
            } else {
                doctors = doctorRepository.findAllActive(pageRequest);
            }
            Page<DoctorViewDTO> result = doctors.map(doctor -> modelMapper.map(doctor, DoctorViewDTO.class));

            logger.info("Retrieved {} {}", result.getContent().size(), ENTITY_NAME);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Failed to retrieve all {}, cause: {}", ENTITY_NAME, e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToRetrieveDoctors(e.getMessage()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Page<DoctorViewDTO> findByCriteria(Map<String, Object> conditions, int page, int size, String orderBy, boolean ascending) {
        if (page < 0) {
            logger.error("Invalid page number: {}", page);
            throw new InvalidInputException("Page number must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid page size: {}", size);
            throw new InvalidInputException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        logger.debug("Finding {} by criteria: conditions={}, page={}, size={}, orderBy={}, ascending={}", ENTITY_NAME, conditions, page, size, orderBy, ascending);

        try {
            Specification<Doctor> spec = buildCriteriaSpecification(conditions);
            Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
            Page<Doctor> doctors = doctorRepository.findAll(spec, PageRequest.of(page, size, sort));
            Page<DoctorViewDTO> result = doctors.map(doctor -> modelMapper.map(doctor, DoctorViewDTO.class));

            logger.info("Found {} {} matching criteria", result.getContent().size(), ENTITY_NAME);
            return result;
        } catch (Exception e) {
            logger.error("Failed to find {} by criteria, cause: {}", ENTITY_NAME, e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToRetrieveDoctors(e.getMessage()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Page<PatientViewDTO> getPatientsByGeneralPractitioner(Long doctorId) {
        if (doctorId == null) {
            logger.error("Cannot retrieve patients for {}: doctor ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("doctorId"));
        }
        logger.debug("Retrieving patients for {} with ID: {}", ENTITY_NAME, doctorId);

        // Check if doctor exists
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, doctorId);
                    return new EntityNotFoundException(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, doctorId));
                });

        try {
            Page<PatientViewDTO> patients = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, Integer.MAX_VALUE))
                    .map(patient -> modelMapper.map(patient, PatientViewDTO.class));
            logger.info("Retrieved {} patients for {} with ID: {}", patients.getContent().size(), ENTITY_NAME, doctorId);
            return patients;
        } catch (Exception e) {
            logger.error("Failed to retrieve patients for {} with ID: {}, cause: {}", ENTITY_NAME, doctorId, e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToRetrievePatients(doctorId));
        }
    }

    /** {@inheritDoc} */
    @Override
    public DoctorPatientCountDTO getPatientCountByGeneralPractitioner(Long doctorId) {
        if (doctorId == null) {
            logger.error("Cannot count patients for {}: doctor ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("doctorId"));
        }
        logger.debug("Counting patients for {} with ID: {}", ENTITY_NAME, doctorId);

        // Check if doctor exists
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, doctorId);
                    return new EntityNotFoundException(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, doctorId));
                });

        try {
            DoctorPatientCountDTO result = doctorRepository.findPatientCountByGeneralPractitioner().stream()
                    .filter(dto -> dto.getDoctor().getId().equals(doctorId))
                    .findFirst()
                    .orElse(DoctorPatientCountDTO.builder().doctor(doctor).patientCount(0L).build());
            logger.info("Counted {} patients for {} with ID: {}", result.getPatientCount(), ENTITY_NAME, doctorId);
            return result;
        } catch (Exception e) {
            logger.error("Failed to count patients for {} with ID: {}, cause: {}", ENTITY_NAME, doctorId, e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToCountPatients(doctorId));
        }
    }

    /** {@inheritDoc} */
    @Override
    public DoctorVisitCountDTO getVisitCount(Long doctorId) {
        if (doctorId == null) {
            logger.error("Cannot count visits for {}: doctor ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("doctorId"));
        }
        logger.debug("Counting visits for {} with ID: {}", ENTITY_NAME, doctorId);

        // Check if doctor exists
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, doctorId);
                    return new EntityNotFoundException(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, doctorId));
                });

        try {
            DoctorVisitCountDTO result = doctorRepository.findVisitCountByDoctor().stream()
                    .filter(dto -> dto.getDoctor().getId().equals(doctorId))
                    .findFirst()
                    .orElse(DoctorVisitCountDTO.builder().doctor(doctor).visitCount(0L).build());
            logger.info("Counted {} visits for {} with ID: {}", result.getVisitCount(), ENTITY_NAME, doctorId);
            return result;
        } catch (Exception e) {
            logger.error("Failed to count visits for {} with ID: {}, cause: {}", ENTITY_NAME, doctorId, e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToCountVisits(doctorId));
        }
    }

    /** {@inheritDoc} */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Page<VisitViewDTO>> getVisitsByPeriod(Long doctorId, LocalDate startDate, LocalDate endDate) {
        if (doctorId == null) {
            logger.error("Cannot retrieve visits for {}: doctor ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("doctorId"));
        }
        if (startDate == null) {
            logger.error("Cannot retrieve visits for {}: start date is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("startDate"));
        }
        if (endDate == null) {
            logger.error("Cannot retrieve visits for {}: end date is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("endDate"));
        }
        if (startDate.isAfter(endDate)) {
            logger.error("Invalid date range for {} visits: start date {} is after end date {}", ENTITY_NAME, startDate, endDate);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDateRange(startDate, endDate));
        }
        logger.debug("Retrieving visits for {} with ID: {} from {} to {}", ENTITY_NAME, doctorId, startDate, endDate);

        // Check if doctor exists
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, doctorId);
                    return new EntityNotFoundException(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, doctorId));
                });

        try {
            Page<VisitViewDTO> visits = visitRepository.findByDoctorAndDateRange(doctor, startDate, endDate, PageRequest.of(0, Integer.MAX_VALUE))
                    .map(visit -> modelMapper.map(visit, VisitViewDTO.class));
            logger.info("Retrieved {} visits for {} with ID: {} from {} to {}", visits.getContent().size(), ENTITY_NAME, doctorId, startDate, endDate);
            return CompletableFuture.completedFuture(visits);
        } catch (Exception e) {
            logger.error("Failed to retrieve visits for {} with ID: {} from {} to {}, cause: {}", ENTITY_NAME, doctorId, startDate, endDate, e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToRetrieveVisits(doctorId, startDate, endDate));
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<DoctorSickLeaveCountDTO> getDoctorsWithMostSickLeaves() {
        logger.debug("Retrieving doctors with most sick leaves");

        try {
            List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();
            logger.info("Retrieved {} doctors with sick leave counts", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Failed to retrieve doctors with most sick leaves, cause: {}", e.getMessage(), e);
            throw new InvalidInputException(ExceptionMessages.formatFailedToRetrieveSickLeaveCounts(e.getMessage()));
        }
    }

    private Set<Specialty> validateAndGetSpecialties(Set<Long> specialtyIds) {
        if (specialtyIds == null || specialtyIds.isEmpty()) {
            return Set.of();
        }
        List<Specialty> specialties = specialtyRepository.findAllById(specialtyIds);
        if (specialties.size() != specialtyIds.size()) {
            logger.error("Invalid specialties provided: some IDs do not exist");
            throw new InvalidDoctorException("One or more specialty IDs are invalid");
        }
        return new HashSet<>(specialties);
    }

    private Specification<Doctor> buildCriteriaSpecification(Map<String, Object> conditions) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (conditions != null && !conditions.isEmpty()) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    String field = entry.getKey().toLowerCase();
                    Object value = entry.getValue();
                    switch (field) {
                        case DoctorCriteria.NAME:
                            predicates.add(cb.like(cb.lower(root.get("name")), "%" + value.toString().toLowerCase() + "%"));
                            break;
                        case DoctorCriteria.IS_GENERAL_PRACTITIONER:
                            predicates.add(cb.equal(root.get("isGeneralPractitioner"), Boolean.parseBoolean(value.toString())));
                            break;
                        case DoctorCriteria.SPECIALTY_ID:
                            predicates.add(root.join("specialties").get("id").in(Long.valueOf(value.toString())));
                            break;
                        default:
                            // Ignore unknown fields
                            break;
                    }
                }
            }
            predicates.add(cb.isFalse(root.get("isDeleted")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}