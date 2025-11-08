package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.*;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.dto.MonthSickLeaveCountDTO;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.contracts.VisitService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class VisitServiceImpl implements VisitService {
    private static final Logger logger = LoggerFactory.getLogger(VisitServiceImpl.class);
    private static final String ENTITY_NAME = "Visit";
    private static final int MAX_PAGE_SIZE = 100;

    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final ModelMapper modelMapper;

    public VisitServiceImpl(VisitRepository visitRepository, PatientRepository patientRepository, DoctorRepository doctorRepository, DiagnosisRepository diagnosisRepository, ModelMapper modelMapper) {
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public VisitViewDTO create(VisitCreateDTO dto) {
        logger.debug("Attempting to create a new {} for patient ID {} and doctor ID {}.", ENTITY_NAME, dto.getPatientId(), dto.getDoctorId());
        validateDtoNotNull(dto);
        Patient patient = findPatientById(dto.getPatientId());
        Doctor doctor = findDoctorById(dto.getDoctorId());
        Diagnosis diagnosis = null;
        if (dto.getDiagnosisId() != null) {
            diagnosis = findDiagnosisById(dto.getDiagnosisId());
        }
        validateBusinessRules(patient, doctor, dto.getVisitDate(), dto.getVisitTime(), null);

        Visit visit = new Visit();
        mapVisitData(visit, dto.getVisitDate(), dto.getVisitTime(), patient, doctor, diagnosis);
        visit.setStatus(VisitStatus.SCHEDULED);
        mapChildrenToVisit(visit, dto.getSickLeave(), dto.getTreatment());

        Visit savedVisit = visitRepository.save(visit);
        logger.info("Successfully created {} with ID: {}", ENTITY_NAME, savedVisit.getId());
        return modelMapper.map(savedVisit, VisitViewDTO.class);
    }

    @Override
    @Transactional
    public VisitViewDTO scheduleNewVisitByPatient(VisitCreateDTO dto) {
        logger.debug("Attempting to schedule a new {} for patient ID {} by a patient.", ENTITY_NAME, dto.getPatientId());
        validateDtoNotNull(dto);
        Patient patient = findPatientById(dto.getPatientId());
        Doctor doctor = findDoctorById(dto.getDoctorId());

        validateBusinessRules(patient, doctor, dto.getVisitDate(), dto.getVisitTime(), null);

        Visit visit = new Visit();
        mapVisitData(visit, dto.getVisitDate(), dto.getVisitTime(), patient, doctor, null);
        visit.setStatus(VisitStatus.SCHEDULED);

        Visit savedVisit = visitRepository.save(visit);
        logger.info("Patient successfully scheduled {} with ID: {}", ENTITY_NAME, savedVisit.getId());
        return modelMapper.map(savedVisit, VisitViewDTO.class);
    }


    @Override
    @Transactional
    public VisitViewDTO update(VisitUpdateDTO dto) {
        logger.debug("Attempting to update {} with ID: {}", ENTITY_NAME, dto.getId());
        validateDtoNotNull(dto);
        validateIdNotNull(dto.getId());
        Visit visit = findVisitById(dto.getId());
        Patient patient = findPatientById(dto.getPatientId());
        Doctor doctor = findDoctorById(dto.getDoctorId());
        Diagnosis diagnosis = dto.getDiagnosisId() != null ? findDiagnosisById(dto.getDiagnosisId()) : null;
        validateBusinessRules(patient, doctor, dto.getVisitDate(), dto.getVisitTime(), visit.getId());
        mapVisitData(visit, dto.getVisitDate(), dto.getVisitTime(), patient, doctor, diagnosis);
        visit.setNotes(dto.getNotes());
        visit.setStatus(dto.getStatus());
        mapChildrenToVisit(visit, dto.getSickLeave(), dto.getTreatment());
        Visit savedVisit = visitRepository.save(visit);
        logger.info("Successfully updated {} with ID: {}", ENTITY_NAME, savedVisit.getId());
        return modelMapper.map(savedVisit, VisitViewDTO.class);
    }

    @Override
    @Transactional
    public VisitViewDTO documentVisit(Long visitId, VisitDocumentationDTO dto) {
        validateIdNotNull(visitId);
        validateDtoNotNull(dto);
        logger.debug("Documenting {} with ID: {}", ENTITY_NAME, visitId);

        Visit visit = findVisitById(visitId);

        if (visit.getStatus() != VisitStatus.SCHEDULED) {
            logger.error("Attempted to document a visit that was not in SCHEDULED state. Visit ID: {}, Status: {}", visitId, visit.getStatus());
            throw new InvalidInputException(ExceptionMessages.VISIT_NOT_SCHEDULED);
        }

        if (dto.getDiagnosisId() != null) {
            Diagnosis diagnosis = findDiagnosisById(dto.getDiagnosisId());
            visit.setDiagnosis(diagnosis);
            logger.debug("Set diagnosis ID {} for visit ID {}", dto.getDiagnosisId(), visitId);
        }

        visit.setNotes(dto.getNotes());
        mapChildrenToVisit(visit, dto.getSickLeave(), dto.getTreatment());
        visit.setStatus(VisitStatus.COMPLETED);

        Visit savedVisit = visitRepository.save(visit);
        logger.info("Successfully added documentation to {} with ID: {}", ENTITY_NAME, savedVisit.getId());
        return modelMapper.map(savedVisit, VisitViewDTO.class);
    }

    @Override
    @Transactional
    public void cancelVisit(Long visitId) {
        validateIdNotNull(visitId);
        logger.debug("Attempting to cancel {} with ID: {} by patient", ENTITY_NAME, visitId);
        Visit visit = findVisitById(visitId);

        authorizePatientAccess(visit.getPatient().getKeycloakId());

        if (visit.getStatus() != VisitStatus.SCHEDULED) {
            logger.warn("Patient attempted to cancel a visit that was not in SCHEDULED state. Visit ID: {}, Status: {}", visitId, visit.getStatus());
            throw new InvalidInputException("Only scheduled visits can be cancelled.");
        }

        visit.setStatus(VisitStatus.CANCELLED_BY_PATIENT);
        visitRepository.save(visit);
        logger.info("Patient successfully cancelled {} with ID: {}", ENTITY_NAME, visitId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        validateIdNotNull(id);
        logger.debug("Attempting to delete {} with ID: {}", ENTITY_NAME, id);
        if (!visitRepository.existsById(id)) {
            logger.error("Attempted to delete a non-existent {}. ID: {}", ENTITY_NAME, id);
            throw new EntityNotFoundException(ExceptionMessages.formatVisitNotFoundById(id));
        }
        visitRepository.deleteById(id);
        logger.info("Successfully deleted {} with ID: {}", ENTITY_NAME, id);
    }

    @Override
    public VisitViewDTO getById(Long id) {
        validateIdNotNull(id);
        logger.debug("Attempting to retrieve {} with ID: {}", ENTITY_NAME, id);
        Visit visit = findVisitById(id);
        authorizePatientAccess(visit.getPatient().getKeycloakId());
        logger.info("Successfully retrieved {} with ID: {}", ENTITY_NAME, id);

        VisitViewDTO visitViewDTO = modelMapper.map(visit, VisitViewDTO.class);

        if (visit.getSickLeave() != null) {
            visitViewDTO.setSickLeave(modelMapper.map(visit.getSickLeave(), SickLeaveViewDTO.class));
        }

        if (visit.getTreatment() != null) {
            visitViewDTO.setTreatment(modelMapper.map(visit.getTreatment(), TreatmentViewDTO.class));
        }

        return visitViewDTO;
    }

    @Override
    @Async
    public CompletableFuture<Page<VisitViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
        validatePagination(page, size);
        logger.debug("Retrieving all {} entities, page {}, size {}, filter: '{}'", ENTITY_NAME, page, size, filter);
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Visit> visits = (filter == null || filter.trim().isEmpty())
                ? visitRepository.findAll(pageable)
                : visitRepository.findByPatientOrDoctorFilter(filter, pageable);
        logger.info("Retrieved {} {} entities.", visits.getTotalElements(), ENTITY_NAME);
        return CompletableFuture.completedFuture(visits.map(v -> modelMapper.map(v, VisitViewDTO.class)));
    }

    @Override
    public Page<VisitViewDTO> getVisitsByPatient(Long patientId, int page, int size) {
        validateIdNotNull(patientId);
        validatePagination(page, size);
        logger.debug("Retrieving visits for patient ID: {}", patientId);
        Patient patient = findPatientById(patientId);
        authorizePatientAccess(patient.getKeycloakId());
        Page<VisitViewDTO> result = visitRepository.findByPatient(patient, PageRequest.of(page, size)).map(v -> modelMapper.map(v, VisitViewDTO.class));
        logger.info("Found {} visits for patient ID: {}", result.getTotalElements(), patientId);
        return result;
    }

    @Override
    public Page<VisitViewDTO> getVisitsByDiagnosis(Long diagnosisId, int page, int size) {
        validateIdNotNull(diagnosisId);
        validatePagination(page, size);
        logger.debug("Retrieving visits for diagnosis ID: {}", diagnosisId);
        Diagnosis diagnosis = findDiagnosisById(diagnosisId);
        Page<VisitViewDTO> result = visitRepository.findByDiagnosis(diagnosis, PageRequest.of(page, size)).map(v -> modelMapper.map(v, VisitViewDTO.class));
        logger.info("Found {} visits for diagnosis ID: {}", result.getTotalElements(), diagnosisId);
        return result;
    }

    @Override
    public Page<VisitViewDTO> getVisitsByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        validatePagination(page, size);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new InvalidInputException(ExceptionMessages.formatInvalidDateRange(startDate, endDate));
        }
        logger.debug("Retrieving visits between {} and {}", startDate, endDate);
        Page<VisitViewDTO> result = visitRepository.findByDateRange(startDate, endDate, PageRequest.of(page, size)).map(v -> modelMapper.map(v, VisitViewDTO.class));
        logger.info("Found {} visits in date range.", result.getTotalElements());
        return result;
    }

    @Override
    public Page<VisitViewDTO> getVisitsByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate, int page, int size) {
        validateIdNotNull(doctorId);
        validatePagination(page, size);
        Doctor doctor = findDoctorById(doctorId);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new InvalidInputException(ExceptionMessages.formatInvalidDateRange(startDate, endDate));
        }
        logger.debug("Retrieving visits for doctor ID {} between {} and {}", doctorId, startDate, endDate);
        Page<VisitViewDTO> result = visitRepository.findByDoctorAndDateRange(doctor, startDate, endDate, PageRequest.of(page, size)).map(v -> modelMapper.map(v, VisitViewDTO.class));
        logger.info("Found {} visits for doctor ID {} in date range.", result.getTotalElements(), doctorId);
        return result;
    }

    @Override
    public Page<VisitViewDTO> getVisitsByDoctorAndStatusAndDateRange(Long doctorId, VisitStatus status, LocalDate startDate, LocalDate endDate, int page, int size) {
        validateIdNotNull(doctorId);
        validatePagination(page, size);
        Doctor doctor = findDoctorById(doctorId);
        if (status == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new InvalidInputException(ExceptionMessages.formatInvalidDateRange(startDate, endDate));
        }
        Pageable pageable = PageRequest.of(page, size);
        logger.debug("Retrieving visits for doctor ID {} with status {} between {} and {}", doctorId, status, startDate, endDate);
        Page<VisitViewDTO> result = visitRepository.findByDoctorAndStatusAndVisitDateBetweenOrderByVisitDateAscVisitTimeAsc(doctor, status, startDate, endDate, pageable)
                .map(v -> modelMapper.map(v, VisitViewDTO.class));
        logger.info("Found {} visits for doctor ID {} with status {} in date range.", result.getTotalElements(), doctorId, status);
        return result;
    }

    @Override
    public List<DoctorVisitCountDTO> getVisitCountByDoctor() {
        logger.debug("Retrieving visit count per doctor.");
        return visitRepository.countVisitsByDoctor();
    }

    @Override
    public List<DiagnosisVisitCountDTO> getMostFrequentDiagnoses() {
        logger.debug("Retrieving most frequent diagnoses.");
        return visitRepository.findMostFrequentDiagnoses();
    }

    @Override
    public List<MonthSickLeaveCountDTO> getMostFrequentSickLeaveMonth() {
        logger.debug("Retrieving most frequent sick leave month.");
        return visitRepository.findMostFrequentSickLeaveMonth();
    }

    private Visit findVisitById(Long id) {
        return visitRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatVisitNotFoundById(id)));
    }

    private Patient findPatientById(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundById(id)));
    }

    private Doctor findDoctorById(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(id)));
    }

    private Diagnosis findDiagnosisById(Long id) {
        return diagnosisRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDiagnosisNotFoundById(id)));
    }

    private void mapVisitData(Visit visit, LocalDate date, LocalTime time, Patient patient, Doctor doctor, Diagnosis diagnosis) {
        visit.setVisitDate(date);
        visit.setVisitTime(time);
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
    }

    private void mapChildrenToVisit(Visit visit, SickLeaveCreateDTO sickLeaveDto, TreatmentCreateDTO treatmentDto) {
        visit.setTreatment(null);
        if (treatmentDto != null) {
            Treatment treatment = new Treatment();
            treatment.setDescription(treatmentDto.getDescription());
            treatment.setVisit(visit);
            if (treatmentDto.getMedicines() != null) {
                treatmentDto.getMedicines().forEach(medDto -> {
                    Medicine medicine = new Medicine();
                    medicine.setName(medDto.getName());
                    medicine.setDosage(medDto.getDosage());
                    medicine.setFrequency(medDto.getFrequency());
                    medicine.setTreatment(treatment);
                    treatment.getMedicines().add(medicine);
                });
            }
            visit.setTreatment(treatment);
        }

        visit.setSickLeave(null);
        if (sickLeaveDto != null) {
            SickLeave sickLeave = new SickLeave();
            sickLeave.setStartDate(sickLeaveDto.getStartDate());
            sickLeave.setDurationDays(sickLeaveDto.getDurationDays());
            sickLeave.setVisit(visit);
            visit.setSickLeave(sickLeave);
        }
    }

    private void mapChildrenToVisit(Visit visit, SickLeaveUpdateDTO sickLeaveDto, TreatmentUpdateDTO treatmentDto) {
        visit.setTreatment(null);
        if (treatmentDto != null) {
            Treatment treatment = new Treatment();
            treatment.setDescription(treatmentDto.getDescription());
            treatment.setVisit(visit);
            if (treatmentDto.getMedicines() != null) {
                treatmentDto.getMedicines().forEach(medDto -> {
                    Medicine medicine = new Medicine();
                    medicine.setName(medDto.getName());
                    medicine.setDosage(medDto.getDosage());
                    medicine.setFrequency(medDto.getFrequency());
                    medicine.setTreatment(treatment);
                    treatment.getMedicines().add(medicine);
                });
            }
            visit.setTreatment(treatment);
        }

        visit.setSickLeave(null);
        if (sickLeaveDto != null) {
            SickLeave sickLeave = new SickLeave();
            sickLeave.setStartDate(sickLeaveDto.getStartDate());
            sickLeave.setDurationDays(sickLeaveDto.getDurationDays());
            sickLeave.setVisit(visit);
            visit.setSickLeave(sickLeave);
        }
    }

    private void validateBusinessRules(Patient patient, Doctor doctor, LocalDate visitDate, LocalTime visitTime, Long existingVisitId) {
        logger.debug("Validating business rules for visit scheduling. Patient ID: {}, Doctor ID: {}", patient.getId(), doctor.getId());

        // Time slot validation
        logger.debug("Checking for existing visit for doctor ID {} at {} on {}", doctor.getId(), visitTime, visitDate);
        visitRepository.findByDoctorAndDateTime(doctor, visitDate, visitTime).ifPresent(existingVisit -> {
            if (existingVisitId == null || !existingVisit.getId().equals(existingVisitId)) {
                logger.warn("Time slot conflict detected for doctor ID {} at {} on {}. Existing visit ID: {}", doctor.getId(), visitTime, visitDate, existingVisit.getId());
                throw new InvalidInputException(ExceptionMessages.formatVisitTimeBooked(visitTime, visitDate));
            }
        });
        logger.debug("Time slot is available for doctor ID {}.", doctor.getId());

        // Insurance validation
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        logger.debug("Checking insurance status for patient ID {}. Payment date: {}. Cutoff date: {}", patient.getId(), patient.getLastInsurancePaymentDate(), sixMonthsAgo);
        if (patient.getLastInsurancePaymentDate() == null || patient.getLastInsurancePaymentDate().isBefore(sixMonthsAgo)) {
            logger.warn("Insurance validation failed for patient ID {}. Payment date is invalid.", patient.getId());
            throw new PatientInsuranceException(ExceptionMessages.formatPatientInsuranceInvalid(patient.getId()));
        }
        logger.debug("Insurance status is valid for patient ID {}.", patient.getId());
    }

    private void authorizePatientAccess(String patientKeycloakId) {
        String currentUserKeycloakId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))) {
            if (!patientKeycloakId.equals(currentUserKeycloakId)) {
                logger.warn("Access denied for patient {} trying to access records of patient {}", currentUserKeycloakId, patientKeycloakId);
                throw new AccessDeniedException(ExceptionMessages.PATIENT_ACCESS_DENIED);
            }
        }
    }

    private void validateDtoNotNull(Object dto) {
        if (dto == null) {
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull(ENTITY_NAME + " DTO"));
        }
    }

    private void validateIdNotNull(Long id) {
        if (id == null) {
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull(ENTITY_NAME + " ID"));
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidDTOException(ExceptionMessages.INVALID_PAGINATION_FOR_OPERATION);
        }
    }
}