package nbu.cscb869.services.services;

import jakarta.validation.Validator;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.common.exceptions.InvalidPatientException;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.contracts.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of {@link VisitService} for managing visit-related operations.
 */
@Service
public class VisitServiceImpl implements VisitService {
    private static final Logger logger = LoggerFactory.getLogger(VisitServiceImpl.class);
    private static final String ENTITY_NAME = "Visit";
    private static final int MAX_PAGE_SIZE = 100;

    private final VisitRepository visitRepository;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final DiagnosisService diagnosisService;
    private final SickLeaveService sickLeaveService;
    private final TreatmentService treatmentService;
    private final ModelMapper modelMapper;
    private final Validator validator;

    public VisitServiceImpl(VisitRepository visitRepository, PatientService patientService,
                            DoctorService doctorService, DiagnosisService diagnosisService,
                            SickLeaveService sickLeaveService, TreatmentService treatmentService,
                            ModelMapper modelMapper, Validator validator) {
        this.visitRepository = visitRepository;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.diagnosisService = diagnosisService;
        this.sickLeaveService = sickLeaveService;
        this.treatmentService = treatmentService;
        this.modelMapper = modelMapper;
        this.validator = validator;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public VisitViewDTO create(VisitCreateDTO dto) {
        if (dto == null) {
            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("VisitCreateDTO"));
        }
        logger.debug("Creating {} for patient ID: {}, doctor ID: {}", ENTITY_NAME, dto.getPatientId(), dto.getDoctorId());

        // Validate DTO constraints
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            logger.error("Invalid {} DTO: {}", ENTITY_NAME, violations);
            throw new InvalidDTOException("Invalid visit data: " + violations);
        }

        // Validate entities
        PatientViewDTO patient = patientService.getById(dto.getPatientId());
        DoctorViewDTO doctor = doctorService.getById(dto.getDoctorId());
        DiagnosisViewDTO diagnosis = diagnosisService.getById(dto.getDiagnosisId());

        // Validate insurance
        if (patient.getLastInsurancePaymentDate() == null ||
                patient.getLastInsurancePaymentDate().isBefore(LocalDate.now().minusMonths(6))) {
            logger.error("Invalid insurance for patient ID: {}", dto.getPatientId());
            throw new InvalidPatientException("Patient insurance is not valid (must be paid within last 6 months)");
        }

        // Validate scheduling
        validateScheduling(dto.getDoctorId(), dto.getVisitDate(), dto.getVisitTime());

        Visit visit = modelMapper.map(dto, Visit.class);
        visit.setPatient(modelMapper.map(patient, Patient.class));
        visit.setDoctor(modelMapper.map(doctor, Doctor.class));
        visit.setDiagnosis(modelMapper.map(diagnosis, nbu.cscb869.data.models.Diagnosis.class));
        visit = visitRepository.save(visit);

        // Handle sick leave
        if (dto.isSickLeaveIssued()) {
            SickLeaveCreateDTO sickLeaveDTO = SickLeaveCreateDTO.builder()
                    .startDate(dto.getVisitDate())
                    .durationDays(5) // Default duration, adjustable
                    .visitId(visit.getId())
                    .build();
            SickLeaveViewDTO sickLeave = sickLeaveService.create(sickLeaveDTO);
            visit.setSickLeave(modelMapper.map(sickLeave, SickLeave.class));
            visit = visitRepository.save(visit);
        }

        logger.info("Created {} with ID: {}", ENTITY_NAME, visit.getId());
        return modelMapper.map(visit, VisitViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public VisitViewDTO update(VisitUpdateDTO dto) {
        if (dto == null) {
            logger.error("Cannot update {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("VisitUpdateDTO"));
        }
        if (dto.getId() == null) {
            logger.error("Cannot update {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        // Validate DTO constraints
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            logger.error("Invalid {} DTO: {}", ENTITY_NAME, violations);
            throw new InvalidDTOException("Invalid visit data: " + violations);
        }

        Visit visit = visitRepository.findById(dto.getId())
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, dto.getId());
                    return new EntityNotFoundException("Visit not found with ID: " + dto.getId());
                });

        // Validate entities
        PatientViewDTO patient = patientService.getById(dto.getPatientId());
        DoctorViewDTO doctor = doctorService.getById(dto.getDoctorId());
        DiagnosisViewDTO diagnosis = diagnosisService.getById(dto.getDiagnosisId());

        // Validate insurance
        if (patient.getLastInsurancePaymentDate() == null ||
                patient.getLastInsurancePaymentDate().isBefore(LocalDate.now().minusMonths(6))) {
            logger.error("Invalid insurance for patient ID: {}", dto.getPatientId());
            throw new InvalidPatientException("Patient insurance is not valid (must be paid within last 6 months)");
        }

        // Validate scheduling (skip if time unchanged)
        if (!dto.getVisitDate().equals(visit.getVisitDate()) || !dto.getVisitTime().equals(visit.getVisitTime())) {
            validateScheduling(dto.getDoctorId(), dto.getVisitDate(), dto.getVisitTime());
        }

        modelMapper.map(dto, visit);
        visit.setPatient(modelMapper.map(patient, Patient.class));
        visit.setDoctor(modelMapper.map(doctor, Doctor.class));
        visit.setDiagnosis(modelMapper.map(diagnosis, nbu.cscb869.data.models.Diagnosis.class));

        // Handle sick leave
        if (dto.isSickLeaveIssued() && visit.getSickLeave() == null) {
            SickLeaveCreateDTO sickLeaveDTO = SickLeaveCreateDTO.builder()
                    .startDate(dto.getVisitDate())
                    .durationDays(5)
                    .visitId(visit.getId())
                    .build();
            SickLeaveViewDTO sickLeave = sickLeaveService.create(sickLeaveDTO);
            visit.setSickLeave(modelMapper.map(sickLeave, SickLeave.class));
        } else if (!dto.isSickLeaveIssued() && visit.getSickLeave() != null) {
            sickLeaveService.delete(visit.getSickLeave().getId());
            visit.setSickLeave(null);
        }

        visit = visitRepository.save(visit);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, visit.getId());
        return modelMapper.map(visit, VisitViewDTO.class);
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

        Visit visit = visitRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException("Visit not found with ID: " + id);
                });

        visitRepository.delete(visit);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public VisitViewDTO getById(Long id) {
        if (id == null) {
            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        Visit visit = visitRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("No {} found with ID: {}", ENTITY_NAME, id);
                    return new EntityNotFoundException("Visit not found with ID: " + id);
                });
        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(visit, VisitViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<VisitViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
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
        Page<Visit> visits = filter == null || filter.trim().isEmpty()
                ? visitRepository.findAllActive(pageable)
                : visitRepository.findByPatientOrDoctorFilter("%" + filter.trim().toLowerCase() + "%", pageable);
        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));

        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
        return CompletableFuture.completedFuture(result);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<VisitViewDTO>> findByPatient(Long patientId, int page, int size) {
        if (patientId == null) {
            logger.error("Cannot retrieve {}: patient ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("patientId"));
        }
        if (page < 0) {
            logger.error("Invalid page number: {}", page);
            throw new InvalidDTOException("Page number must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid page size: {}", size);
            throw new InvalidDTOException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        logger.debug("Retrieving {} for patient ID: {}, page={}, size={}", ENTITY_NAME, patientId, page, size);

        PatientViewDTO patient = patientService.getById(patientId); // Validates patient exists
        Patient p = modelMapper.map(patient, Patient.class);
        Page<Visit> visits = visitRepository.findByPatient(p, PageRequest.of(page, size));
        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));

        logger.info("Retrieved {} {} for patient ID: {}", result.getTotalElements(), ENTITY_NAME, patientId);
        return CompletableFuture.completedFuture(result);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<VisitViewDTO>> findByDiagnosis(Long diagnosisId, int page, int size) {
        if (diagnosisId == null) {
            logger.error("Cannot retrieve {}: diagnosis ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("diagnosisId"));
        }
        if (page < 0) {
            logger.error("Invalid page number: {}", page);
            throw new InvalidDTOException("Page number must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid page size: {}", size);
            throw new InvalidDTOException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        logger.debug("Retrieving {} for diagnosis ID: {}, page={}, size={}", ENTITY_NAME, diagnosisId, page, size);

        DiagnosisViewDTO diagnosis = diagnosisService.getById(diagnosisId); // Validates diagnosis exists
        nbu.cscb869.data.models.Diagnosis d = modelMapper.map(diagnosis, nbu.cscb869.data.models.Diagnosis.class);
        Page<Visit> visits = visitRepository.findByDiagnosis(d, PageRequest.of(page, size));
        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));

        logger.info("Retrieved {} {} for diagnosis ID: {}", result.getTotalElements(), ENTITY_NAME, diagnosisId);
        return CompletableFuture.completedFuture(result);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<VisitViewDTO>> findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        if (startDate == null || endDate == null) {
            logger.error("Cannot retrieve {}: date is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("date"));
        }
        if (startDate.isAfter(endDate)) {
            logger.error("Invalid date range for {}: start date {} is after end date {}", ENTITY_NAME, startDate, endDate);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDateRange(startDate, endDate));
        }
        if (page < 0) {
            logger.error("Invalid page number: {}", page);
            throw new InvalidDTOException("Page number must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid page size: {}", size);
            throw new InvalidDTOException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        logger.debug("Retrieving {} from {} to {}, page={}, size={}", ENTITY_NAME, startDate, endDate, page, size);

        Page<Visit> visits = visitRepository.findByDateRange(startDate, endDate, PageRequest.of(page, size));
        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));

        logger.info("Retrieved {} {} from {} to {}", result.getTotalElements(), ENTITY_NAME, startDate, endDate);
        return CompletableFuture.completedFuture(result);
    }

    /** {@inheritDoc} */
    @Override
    public List<DiagnosisVisitCountDTO> findMostFrequentDiagnoses() {
        logger.debug("Retrieving most frequent diagnoses");

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();
        logger.info("Retrieved {} diagnoses with visit counts", result.size());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public int findMonthWithMostSickLeaves() {
        logger.debug("Retrieving month with most sick leaves");

        List<Visit> sickLeaveVisits = visitRepository.findAllActive().stream()
                .filter(Visit::isSickLeaveIssued)
                .toList();
        if (sickLeaveVisits.isEmpty()) {
            logger.warn("No sick leaves found");
            throw new InvalidInputException("No sick leaves issued");
        }

        int maxMonth = sickLeaveVisits.stream()
                .map(v -> v.getVisitDate().getMonthValue())
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new InvalidInputException("Unable to determine month with most sick leaves"));

        logger.info("Month with most sick leaves: {}", maxMonth);
        return maxMonth;
    }

    private void validateScheduling(Long doctorId, LocalDate visitDate, LocalTime visitTime) {
        if (visitRepository.findByDoctorAndDateTime(
                modelMapper.map(doctorService.getById(doctorId), Doctor.class),
                visitDate, visitTime).isPresent()) {
            logger.error("Visit time {} on {} is already booked for doctor ID: {}", visitTime, visitDate, doctorId);
            throw new InvalidInputException("Visit time is already booked");
        }
    }
}