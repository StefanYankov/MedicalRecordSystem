package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.*;
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

/**
 * Implementation of {@link VisitService} for managing visit operations.
 */
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

    /**
     * Constructs a new VisitServiceImpl with the specified dependencies.
     */
    public VisitServiceImpl(VisitRepository visitRepository, PatientRepository patientRepository, DoctorRepository doctorRepository, DiagnosisRepository diagnosisRepository, ModelMapper modelMapper) {
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.modelMapper = modelMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public VisitViewDTO create(VisitCreateDTO dto) {
        validateDtoNotNull(dto);
        logger.debug("Creating {} on {} at {}", ENTITY_NAME, dto.getVisitDate(), dto.getVisitTime());

        Patient patient = findPatientById(dto.getPatientId());
        Doctor doctor = findDoctorById(dto.getDoctorId());
        Diagnosis diagnosis = findDiagnosisById(dto.getDiagnosisId());

        validateBusinessRules(patient, doctor, dto.getVisitDate(), dto.getVisitTime(), null);

        Visit visit = new Visit();
        mapVisitData(visit, dto.getVisitDate(), dto.getVisitTime(), patient, doctor, diagnosis);
        mapChildrenToVisit(visit, dto.getSickLeave(), dto.getTreatment());

        Visit savedVisit = visitRepository.save(visit);
        logger.info("Created {} with ID: {}", ENTITY_NAME, savedVisit.getId());

        // Removed: notificationService.sendVisitConfirmation(savedVisit);

        return modelMapper.map(savedVisit, VisitViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public VisitViewDTO update(VisitUpdateDTO dto) {
        validateDtoNotNull(dto);
        validateIdNotNull(dto.getId());
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        Visit visit = findVisitById(dto.getId());
        Patient patient = findPatientById(dto.getPatientId());
        Doctor doctor = findDoctorById(dto.getDoctorId());
        Diagnosis diagnosis = findDiagnosisById(dto.getDiagnosisId());

        validateBusinessRules(patient, doctor, dto.getVisitDate(), dto.getVisitTime(), visit.getId());

        mapVisitData(visit, dto.getVisitDate(), dto.getVisitTime(), patient, doctor, diagnosis);
        mapChildrenToVisit(visit, dto.getSickLeave(), dto.getTreatment());

        Visit savedVisit = visitRepository.save(visit);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, savedVisit.getId());
        return modelMapper.map(savedVisit, VisitViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        validateIdNotNull(id);
        if (!visitRepository.existsById(id)) {
            throw new EntityNotFoundException(ExceptionMessages.formatVisitNotFoundById(id));
        }
        visitRepository.deleteById(id);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public VisitViewDTO getById(Long id) {
        validateIdNotNull(id);
        Visit visit = findVisitById(id);
        authorizePatientAccess(visit.getPatient().getKeycloakId());
        return modelMapper.map(visit, VisitViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<VisitViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
        validatePagination(page, size);
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Visit> visits = (filter == null || filter.trim().isEmpty())
                ? visitRepository.findAll(pageable)
                : visitRepository.findByPatientOrDoctorFilter(filter, pageable);

        return CompletableFuture.completedFuture(visits.map(v -> modelMapper.map(v, VisitViewDTO.class)));
    }

    /** {@inheritDoc} */
    @Override
    public Page<VisitViewDTO> getVisitsByPatient(Long patientId, int page, int size) {
        validateIdNotNull(patientId);
        validatePagination(page, size);
        Patient patient = findPatientById(patientId);
        authorizePatientAccess(patient.getKeycloakId());
        return visitRepository.findByPatient(patient, PageRequest.of(page, size)).map(v -> modelMapper.map(v, VisitViewDTO.class));
    }

    /** {@inheritDoc} */
    @Override
    public Page<VisitViewDTO> getVisitsByDiagnosis(Long diagnosisId, int page, int size) {
        validateIdNotNull(diagnosisId);
        validatePagination(page, size);
        Diagnosis diagnosis = findDiagnosisById(diagnosisId);
        return visitRepository.findByDiagnosis(diagnosis, PageRequest.of(page, size)).map(v -> modelMapper.map(v, VisitViewDTO.class));
    }

    /** {@inheritDoc} */
    @Override
    public Page<VisitViewDTO> getVisitsByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        validatePagination(page, size);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new InvalidInputException(ExceptionMessages.formatInvalidDateRange(startDate, endDate));
        }
        return visitRepository.findByDateRange(startDate, endDate, PageRequest.of(page, size)).map(v -> modelMapper.map(v, VisitViewDTO.class));
    }

    /** {@inheritDoc} */
    @Override
    public Page<VisitViewDTO> getVisitsByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate, int page, int size) {
        validateIdNotNull(doctorId);
        validatePagination(page, size);
        Doctor doctor = findDoctorById(doctorId);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new InvalidInputException(ExceptionMessages.formatInvalidDateRange(startDate, endDate));
        }
        return visitRepository.findByDoctorAndDateRange(doctor, startDate, endDate, PageRequest.of(page, size)).map(v -> modelMapper.map(v, VisitViewDTO.class));
    }

    /** {@inheritDoc} */
    @Override
    public List<DoctorVisitCountDTO> getVisitCountByDoctor() {
        return visitRepository.countVisitsByDoctor();
    }

    /** {@inheritDoc} */
    @Override
    public List<DiagnosisVisitCountDTO> getMostFrequentDiagnoses() {
        return visitRepository.findMostFrequentDiagnoses();
    }

    // --- Private Helper Methods ---

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
        if (patient.getLastInsurancePaymentDate() == null || patient.getLastInsurancePaymentDate().isBefore(LocalDate.now().minusMonths(6))) {
            throw new InvalidInputException(ExceptionMessages.formatPatientInsuranceInvalid(patient.getId()));
        }
        visitRepository.findByDoctorAndDateTime(doctor, visitDate, visitTime).ifPresent(existingVisit -> {
            if (existingVisitId == null || !existingVisit.getId().equals(existingVisitId)) {
                throw new InvalidInputException(ExceptionMessages.formatVisitTimeBooked(visitTime, visitDate));
            }
        });
    }

    private void authorizePatientAccess(String patientKeycloakId) {
        String currentUserKeycloakId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))) {
            if (!patientKeycloakId.equals(currentUserKeycloakId)) {
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
