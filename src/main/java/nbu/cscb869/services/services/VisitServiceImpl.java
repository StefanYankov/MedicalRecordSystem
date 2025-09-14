//package nbu.cscb869.services.services;
//
//import jakarta.validation.Validator;
//import nbu.cscb869.common.exceptions.EntityNotFoundException;
//import nbu.cscb869.common.exceptions.InvalidDTOException;
//import nbu.cscb869.common.exceptions.InvalidInputException;
//import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
//import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
//import nbu.cscb869.data.dto.DoctorVisitCountDTO;
//import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
//import nbu.cscb869.data.models.Doctor;
//import nbu.cscb869.data.models.Patient;
//import nbu.cscb869.data.models.SickLeave;
//import nbu.cscb869.data.models.Visit;
//import nbu.cscb869.data.repositories.VisitRepository;
//import nbu.cscb869.services.common.exceptions.InvalidPatientException;
//import nbu.cscb869.services.data.dtos.*;
//import nbu.cscb869.services.data.dtos.identity.UserViewDTO;
//import nbu.cscb869.services.services.contracts.*;
//import org.modelmapper.ModelMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.*;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
///**
// * Implementation of {@link VisitService} for managing visit-related operations.
// */
//@Service
//public class VisitServiceImpl implements VisitService {
//    private static final Logger logger = LoggerFactory.getLogger(VisitServiceImpl.class);
//    private static final String ENTITY_NAME = "Visit";
//    private static final int MAX_PAGE_SIZE = 100;
//
//    private final VisitRepository visitRepository;
//    private final PatientService patientService;
//    private final DoctorService doctorService;
//    private final DiagnosisService diagnosisService;
//    private final SickLeaveService sickLeaveService;
//    private final TreatmentService treatmentService;
//    private final UserService userService;
//    private final ModelMapper modelMapper;
//    private final Validator validator;
//
//    public VisitServiceImpl(VisitRepository visitRepository, PatientService patientService,
//                            DoctorService doctorService, DiagnosisService diagnosisService,
//                            SickLeaveService sickLeaveService, TreatmentService treatmentService,
//                            UserService userService, ModelMapper modelMapper, Validator validator) {
//        this.visitRepository = visitRepository;
//        this.patientService = patientService;
//        this.doctorService = doctorService;
//        this.diagnosisService = diagnosisService;
//        this.sickLeaveService = sickLeaveService;
//        this.treatmentService = treatmentService;
//        this.userService = userService;
//        this.modelMapper = modelMapper;
//        this.validator = validator;
//    }
//
//    @Override
//    @Transactional
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public VisitViewDTO create(VisitCreateDTO dto) {
//        if (dto == null) {
//            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
//            throw new InvalidDTOException("VisitCreateDTO cannot be null");
//        }
//        logger.debug("Creating {} for patient ID: {}, doctor ID: {}", ENTITY_NAME, dto.getPatientId(), dto.getDoctorId());
//
//        var violations = validator.validate(dto);
//        if (!violations.isEmpty()) {
//            logger.error("Invalid {} DTO: {}", ENTITY_NAME, violations);
//            throw new InvalidDTOException("Invalid visit data: " + violations);
//        }
//
//        validateDoctorOwnership(dto.getDoctorId());
//
//        PatientViewDTO patient = patientService.getById(dto.getPatientId());
//        DoctorViewDTO doctor = doctorService.getById(dto.getDoctorId());
//        DiagnosisViewDTO diagnosis = diagnosisService.getById(dto.getDiagnosisId());
//
//        if (!patientService.hasValidInsurance(patient.getId())) {
//            logger.error("Invalid insurance for patient ID: {}", dto.getPatientId());
//            throw new InvalidPatientException("Patient insurance is not valid (must be paid within last 6 months)");
//        }
//
//        validateScheduling(dto.getDoctorId(), dto.getVisitDate(), dto.getVisitTime());
//
//        Visit visit = modelMapper.map(dto, Visit.class);
//        visit.setPatient(modelMapper.map(patient, Patient.class));
//        visit.setDoctor(modelMapper.map(doctor, Doctor.class));
//        visit.setDiagnosis(modelMapper.map(diagnosis, nbu.cscb869.data.models.Diagnosis.class));
//        visit = visitRepository.save(visit);
//
//        if (dto.getSickLeaveIssued()) {
//            SickLeaveCreateDTO sickLeaveDTO = SickLeaveCreateDTO.builder()
//                    .startDate(dto.getVisitDate())
//                    .durationDays(5)
//                    .visitId(visit.getId())
//                    .build();
//            SickLeaveViewDTO sickLeave = sickLeaveService.create(sickLeaveDTO);
//            visit.setSickLeave(modelMapper.map(sickLeave, SickLeave.class));
//            visit = visitRepository.save(visit);
//        }
//
//        logger.info("Created {} with ID: {}", ENTITY_NAME, visit.getId());
//        return modelMapper.map(visit, VisitViewDTO.class);
//    }
//
//    @Override
//    @Transactional
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public VisitViewDTO update(VisitUpdateDTO dto) {
//        if (dto == null || dto.getId() == null) {
//            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("VisitUpdateDTO or ID cannot be null");
//        }
//        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
//
//        var violations = validator.validate(dto);
//        if (!violations.isEmpty()) {
//            logger.error("Invalid {} DTO: {}", ENTITY_NAME, violations);
//            throw new InvalidDTOException("Invalid visit data: " + violations);
//        }
//
//        Visit visit = visitRepository.findById(dto.getId())
//                .orElseThrow(() -> new EntityNotFoundException("Visit not found with ID: " + dto.getId()));
//
//        validateDoctorOwnership(dto.getDoctorId());
//
//        PatientViewDTO patient = patientService.getById(dto.getPatientId());
//        DoctorViewDTO doctor = doctorService.getById(dto.getDoctorId());
//        DiagnosisViewDTO diagnosis = diagnosisService.getById(dto.getDiagnosisId());
//
//        if (!patientService.hasValidInsurance(patient.getId())) {
//            logger.error("Invalid insurance for patient ID: {}", dto.getPatientId());
//            throw new InvalidPatientException("Patient insurance is not valid (must be paid within last 6 months)");
//        }
//
//        if (!dto.getVisitDate().equals(visit.getVisitDate()) || !dto.getVisitTime().equals(visit.getVisitTime())) {
//            validateScheduling(dto.getDoctorId(), dto.getVisitDate(), dto.getVisitTime(), visit.getId());
//        }
//
//        modelMapper.map(dto, visit);
//        visit.setPatient(modelMapper.map(patient, Patient.class));
//        visit.setDoctor(modelMapper.map(doctor, Doctor.class));
//        visit.setDiagnosis(modelMapper.map(diagnosis, nbu.cscb869.data.models.Diagnosis.class));
//
//        if (dto.getSickLeaveIssued() && visit.getSickLeave() == null) {
//            SickLeaveCreateDTO sickLeaveDTO = SickLeaveCreateDTO.builder()
//                    .startDate(dto.getVisitDate())
//                    .durationDays(5)
//                    .visitId(visit.getId())
//                    .build();
//            SickLeaveViewDTO sickLeave = sickLeaveService.create(sickLeaveDTO);
//            visit.setSickLeave(modelMapper.map(sickLeave, SickLeave.class));
//        } else if (!dto.getSickLeaveIssued() && visit.getSickLeave() != null) {
//            sickLeaveService.delete(visit.getSickLeave().getId());
//            visit.setSickLeave(null);
//        }
//
//        visit = visitRepository.save(visit);
//        logger.info("Updated {} with ID: {}", ENTITY_NAME, visit.getId());
//        return modelMapper.map(visit, VisitViewDTO.class);
//    }
//
//    @Override
//    @Transactional
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public void delete(Long id) {
//        if (id == null) {
//            logger.error("Cannot delete {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);
//
//        Visit visit = visitRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Visit not found with ID: " + id));
//
//        visit.setIsDeleted(true);
//        visit.setDeletedOn(LocalDateTime.now());
//        visitRepository.save(visit);
//        logger.info("Soft deleted {} with ID: {}", ENTITY_NAME, id);
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
//    public VisitViewDTO getById(Long id) {
//        if (id == null) {
//            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
//
//        Visit visit = visitRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Visit not found with ID: " + id));
//        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
//        return modelMapper.map(visit, VisitViewDTO.class);
//    }
//
//    @Override
//    @Async
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public CompletableFuture<Page<VisitViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}, filter={}",
//                ENTITY_NAME, page, size, orderBy, ascending, filter);
//
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<Visit> visits = (filter == null || filter.trim().isEmpty())
//                ? visitRepository.findAllActive(pageable)
//                : visitRepository.findByPatientOrDoctorFilter("%" + filter.trim().toLowerCase() + "%", pageable);
//        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));
//        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    @Override
//    @Async
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
//    public CompletableFuture<Page<VisitViewDTO>> findByPatient(Long patientId, int page, int size) {
//        if (patientId == null) {
//            logger.error("Cannot retrieve {}: Patient ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("Patient ID cannot be null");
//        }
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving {} for patient ID: {}, page={}, size={}", ENTITY_NAME, patientId, page, size);
//
//        PatientViewDTO patient = patientService.getById(patientId);
//        Patient p = modelMapper.map(patient, Patient.class);
//        Page<Visit> visits = visitRepository.findByPatient(p, PageRequest.of(page, size));
//        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));
//        logger.info("Retrieved {} {} for patient ID: {}", result.getTotalElements(), ENTITY_NAME, patientId);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    @Override
//    @Async
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public CompletableFuture<Page<VisitViewDTO>> findByDiagnosis(Long diagnosisId, int page, int size) {
//        if (diagnosisId == null) {
//            logger.error("Cannot retrieve {}: Diagnosis ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("Diagnosis ID cannot be null");
//        }
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving {} for diagnosis ID: {}, page={}, size={}", ENTITY_NAME, diagnosisId, page, size);
//
//        DiagnosisViewDTO diagnosis = diagnosisService.getById(diagnosisId);
//        nbu.cscb869.data.models.Diagnosis d = modelMapper.map(diagnosis, nbu.cscb869.data.models.Diagnosis.class);
//        Page<Visit> visits = visitRepository.findByDiagnosis(d, PageRequest.of(page, size));
//        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));
//        logger.info("Retrieved {} {} for diagnosis ID: {}", result.getTotalElements(), ENTITY_NAME, diagnosisId);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    @Override
//    @Async
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public CompletableFuture<Page<VisitViewDTO>> findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
//        if (startDate == null || endDate == null) {
//            logger.error("Cannot retrieve {}: Date is null", ENTITY_NAME);
//            throw new InvalidDTOException("Start date and end date cannot be null");
//        }
//        if (startDate.isAfter(endDate)) {
//            logger.error("Invalid date range: startDate={} is after endDate={}", startDate, endDate);
//            throw new InvalidDTOException("Start date must not be after end date");
//        }
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving {} from {} to {}, page={}, size={}", ENTITY_NAME, startDate, endDate, page, size);
//
//        Page<Visit> visits = visitRepository.findByDateRange(startDate, endDate, PageRequest.of(page, size));
//        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));
//        logger.info("Retrieved {} {} from {} to {}", result.getTotalElements(), ENTITY_NAME, startDate, endDate);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    @Override
//    @Async
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public CompletableFuture<Page<VisitViewDTO>> findByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate, int page, int size) {
//        if (doctorId == null) {
//            logger.error("Cannot retrieve {}: Doctor ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("Doctor ID cannot be null");
//        }
//        if (startDate == null || endDate == null) {
//            logger.error("Cannot retrieve {}: Date is null", ENTITY_NAME);
//            throw new InvalidDTOException("Start date and end date cannot be null");
//        }
//        if (startDate.isAfter(endDate)) {
//            logger.error("Invalid date range: startDate={} is after endDate={}", startDate, endDate);
//            throw new InvalidDTOException("Start date must not be after end date");
//        }
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving {} for doctor ID: {}, from {} to {}, page={}, size={}",
//                ENTITY_NAME, doctorId, startDate, endDate, page, size);
//
//        DoctorViewDTO doctor = doctorService.getById(doctorId);
//        Doctor d = modelMapper.map(doctor, Doctor.class);
//        Page<Visit> visits = visitRepository.findByDoctorAndDateRange(d, startDate, endDate, PageRequest.of(page, size));
//        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));
//        logger.info("Retrieved {} {} for doctor ID: {}, from {} to {}",
//                result.getTotalElements(), ENTITY_NAME, doctorId, startDate, endDate);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    @Override
//    @PreAuthorize("hasRole('ADMIN')")
//    public List<DoctorVisitCountDTO> countVisitsByDoctor() {
//        logger.debug("Retrieving visit counts by doctor");
//        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();
//        logger.info("Retrieved {} doctors with visit counts", result.size());
//        return result;
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public List<DiagnosisVisitCountDTO> findMostFrequentDiagnoses() {
//        logger.debug("Retrieving most frequent diagnoses");
//        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();
//        logger.info("Retrieved {} diagnoses with visit counts", result.size());
//        return result;
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public int findMonthWithMostSickLeaves() {
//        logger.debug("Retrieving month with most sick leaves");
//        List<YearMonthSickLeaveCountDTO> results = sickLeaveRepository.findYearMonthWithMostSickLeaves();
//        if (results.isEmpty()) {
//            logger.warn("No sick leaves found");
//            throw new InvalidInputException("No sick leaves issued");
//        }
//        logger.info("Month with most sick leaves: {}", results.get(0).getMonth());
//        return results.get(0).getMonth();
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public List<DoctorSickLeaveCountDTO> findDoctorsWithMostSickLeaves() {
//        logger.debug("Retrieving doctors with most sick leaves");
//        List<DoctorSickLeaveCountDTO> result = sickLeaveRepository.findDoctorsWithMostSickLeaves();
//        logger.info("Retrieved {} doctors with sick leave counts", result.size());
//        return result;
//    }
//
//    private void validateScheduling(Long doctorId, LocalDate visitDate, LocalTime visitTime, Long... excludeVisitId) {
//        Doctor d = modelMapper.map(doctorService.getById(doctorId), Doctor.class);
//        if (visitRepository.findByDoctorAndDateTime(d, visitDate, visitTime, excludeVisitId.length > 0 ? excludeVisitId[0] : null).isPresent()) {
//            logger.error("Visit time {} on {} is already booked for doctor ID: {}", visitTime, visitDate, doctorId);
//            throw new InvalidInputException("Visit time is already booked");
//        }
//    }
//
//    private void validateDoctorOwnership(Long doctorId) {
//        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
//                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
//            String currentUserId = userService.getCurrentUserId((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
//            DoctorViewDTO doctor = doctorService.getById(doctorId);
//            UserViewDTO user = userService.getByKeycloakId(currentUserId);
//            if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("DOCTOR")) || !doctor.getUserId().equals(user.getId())) {
//                logger.error("Doctor ID: {} does not match authenticated user: {}", doctorId, currentUserId);
//                throw new InvalidInputException("Cannot create/update visit for another doctor");
//            }
//        }
//    }
//}