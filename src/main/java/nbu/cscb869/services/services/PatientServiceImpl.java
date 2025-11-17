package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.*;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.PatientService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static nbu.cscb869.config.WebConstants.MAX_PAGE_SIZE;

@Service
public class PatientServiceImpl implements PatientService {
    private static final Logger logger = LoggerFactory.getLogger(PatientServiceImpl.class);
    private static final String ENTITY_NAME = "Patient";

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final VisitRepository visitRepository;
    private final ModelMapper modelMapper;

    public PatientServiceImpl(PatientRepository patientRepository, DoctorRepository doctorRepository, VisitRepository visitRepository, ModelMapper modelMapper) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.visitRepository = visitRepository;
        this.modelMapper = modelMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PatientViewDTO create(PatientCreateDTO dto) {
        return createPatientUnchecked(dto);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PatientViewDTO registerPatient(PatientCreateDTO dto) {
        validateDtoNotNull(dto, "register");
        logger.debug("Registering new patient with EGN: {}", dto.getEgn());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var oidcUser = (OidcUser) authentication.getPrincipal();
        String keycloakId = oidcUser.getSubject();

        dto.setKeycloakId(keycloakId);
        dto.setName(oidcUser.getFullName());

        return createPatientUnchecked(dto);
    }

    private PatientViewDTO createPatientUnchecked(PatientCreateDTO dto) {
        validateDtoNotNull(dto, "create");
        logger.debug("Creating {} with EGN: {}", ENTITY_NAME, dto.getEgn());

        if (patientRepository.findByEgn(dto.getEgn()).isPresent()) {
            logger.error("Patient with EGN {} already exists.", dto.getEgn());
            throw new InvalidDTOException(ExceptionMessages.formatPatientEgnExists(dto.getEgn()));
        }

        if (dto.getKeycloakId() == null || dto.getKeycloakId().isBlank()) {
            logger.error("Keycloak ID is missing for patient creation.");
            throw new InvalidDTOException(ExceptionMessages.KEYCLOAK_ID_MISSING_FOR_ADMIN_CREATION);
        }

        if (patientRepository.findByKeycloakId(dto.getKeycloakId()).isPresent()) {
            logger.error("Patient with Keycloak ID {} already exists.", dto.getKeycloakId());
            throw new InvalidDTOException(ExceptionMessages.formatPatientKeycloakIdExists(dto.getKeycloakId()));
        }

        Patient patient = new Patient();
        if (dto.getName() != null) patient.setName(dto.getName());
        if (dto.getEgn() != null) patient.setEgn(dto.getEgn());
        if (dto.getKeycloakId() != null) patient.setKeycloakId(dto.getKeycloakId());
        if (dto.getLastInsurancePaymentDate() != null) patient.setLastInsurancePaymentDate(dto.getLastInsurancePaymentDate());

        Doctor gp = findDoctorById(dto.getGeneralPractitionerId());
        if (!gp.isGeneralPractitioner()) {
            logger.error("Doctor with ID {} is not a general practitioner.", gp.getId());
            throw new InvalidDTOException(ExceptionMessages.formatInvalidGeneralPractitioner(gp.getId()));
        }
        patient.setGeneralPractitioner(gp);

        patient = patientRepository.save(patient);
        logger.info("Created {} with ID: {}", ENTITY_NAME, patient.getId());
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PatientViewDTO update(PatientUpdateDTO dto) {
        validateDtoNotNull(dto, "update");
        validateIdNotNull(dto.getId(), "update");
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        Patient patient = findPatientById(dto.getId());

        if (!patient.getEgn().equals(dto.getEgn())) {
            Optional<Patient> existingPatientWithEgn = patientRepository.findByEgn(dto.getEgn());
            if (existingPatientWithEgn.isPresent() && !existingPatientWithEgn.get().getId().equals(patient.getId())) {
                logger.error("Patient with EGN {} already exists for another patient.", dto.getEgn());
                throw new InvalidDTOException(ExceptionMessages.formatPatientEgnExists(dto.getEgn()));
            }
        }

        if (dto.getKeycloakId() != null && !dto.getKeycloakId().equals(patient.getKeycloakId())) {
            Optional<Patient> existingPatientWithKeycloakId = patientRepository.findByKeycloakId(dto.getKeycloakId());
            if (existingPatientWithKeycloakId.isPresent() && !existingPatientWithKeycloakId.get().getId().equals(patient.getId())) {
                logger.error("Patient with Keycloak ID {} already exists for another patient.", dto.getKeycloakId());
                throw new InvalidDTOException(ExceptionMessages.formatPatientKeycloakIdExists(dto.getKeycloakId()));
            }
        }

        if (dto.getName() != null) patient.setName(dto.getName());
        if (dto.getEgn() != null) patient.setEgn(dto.getEgn());
        if (dto.getKeycloakId() != null) patient.setKeycloakId(dto.getKeycloakId());
        if (dto.getLastInsurancePaymentDate() != null) patient.setLastInsurancePaymentDate(dto.getLastInsurancePaymentDate());

        if (dto.getGeneralPractitionerId() != null) {
            Doctor gp = findDoctorById(dto.getGeneralPractitionerId());
            if (!gp.isGeneralPractitioner()) {
                logger.error("Doctor with ID {} is not a general practitioner.", gp.getId());
                throw new InvalidDTOException(ExceptionMessages.formatInvalidGeneralPractitioner(gp.getId()));
            }
            patient.setGeneralPractitioner(gp);
        }

        patientRepository.save(patient);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, patient.getId());
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PatientViewDTO updateInsuranceStatus(Long patientId) {
        validateIdNotNull(patientId, "updateInsuranceStatus");
        logger.debug("Updating insurance status for patient ID: {}", patientId);
        Patient patient = findPatientById(patientId);
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patientRepository.save(patient);
        logger.info("Successfully updated insurance status for patient ID: {}", patientId);
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        validateIdNotNull(id, "delete");
        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);
        if (!patientRepository.existsById(id)) {
            logger.error("Patient with ID {} not found for deletion.", id);
            throw new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundById(id));
        }
        patientRepository.deleteById(id);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public PatientViewDTO getById(Long id) {
        validateIdNotNull(id, "getById");
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
        Patient patient = findPatientById(id);
        authorizePatientAccess(patient.getKeycloakId());
        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    public PatientViewDTO getByEgn(String egn) {
        if (egn == null || egn.trim().isEmpty()) {
            logger.error("EGN cannot be null or empty for getByEgn.");
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldEmpty("EGN"));
        }
        logger.debug("Retrieving {} with EGN: {}", ENTITY_NAME, egn);
        Patient patient = patientRepository.findByEgn(egn)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundByEgn(egn)));
        authorizePatientAccess(patient.getKeycloakId());
        logger.info("Retrieved {} with EGN: {}", ENTITY_NAME, egn);
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    public PatientViewDTO getByKeycloakId(String keycloakId) {
        if (keycloakId == null || keycloakId.trim().isEmpty()) {
            logger.error("Cannot retrieve {}: Keycloak ID is null or empty", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldEmpty("Keycloak ID"));
        }
        logger.debug("Retrieving {} with Keycloak ID: {}", ENTITY_NAME, keycloakId);

        Patient patient = patientRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> {
                    logger.error("Patient not found with Keycloak ID: {}", keycloakId);
                    return new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundByKeycloakId(keycloakId));
                });

        logger.info("Retrieved {} with Keycloak ID: {}", ENTITY_NAME, keycloakId);
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<PatientViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
        validatePagination(page, size, "getAll");
        Pageable pageable = PageRequest.of(page, size, Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy));
        return CompletableFuture.completedFuture(findPatients(pageable, filter));
    }

    /** {@inheritDoc} */
    @Override
    public Page<PatientViewDTO> findAll(Pageable pageable, String keyword) {
        return findPatients(pageable, keyword);
    }

    private Page<PatientViewDTO> findPatients(Pageable pageable, String filter) {
        Page<Patient> patients = (filter == null || filter.trim().isEmpty())
                ? patientRepository.findAll(pageable)
                : patientRepository.findByEgnContaining(filter.trim(), pageable);
        return patients.map(p -> modelMapper.map(p, PatientViewDTO.class));
    }

    /** {@inheritDoc} */
    @Override
    public Page<PatientViewDTO> getByGeneralPractitioner(Long generalPractitionerId, int page, int size) {
        validateIdNotNull(generalPractitionerId, "getByGeneralPractitioner");
        validatePagination(page, size, "getByGeneralPractitioner");
        Doctor gp = findDoctorById(generalPractitionerId);
        if (!gp.isGeneralPractitioner()) {
            logger.error("Doctor with ID {} is not a general practitioner.", gp.getId());
            throw new InvalidPatientException(ExceptionMessages.formatInvalidGeneralPractitioner(gp.getId()));
        }
        Pageable pageable = PageRequest.of(page, size);
        return patientRepository.findByGeneralPractitioner(gp, pageable)
                .map(p -> modelMapper.map(p, PatientViewDTO.class));
    }

    /** {@inheritDoc} */
    @Override
    public List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner() {
        logger.debug("Retrieving patient count by general practitioner.");
        return doctorRepository.findPatientCountByGeneralPractitioner();
    }

    /** {@inheritDoc} */
    @Override
    public Page<PatientViewDTO> findPatientsForDoctor(Long doctorId, Pageable pageable) {
        validateIdNotNull(doctorId, "findPatientsForDoctor");
        logger.debug("Retrieving patients for doctor ID: {}", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(doctorId)));

        // Patients for whom the doctor is the general practitioner
        List<Patient> gpPatients = patientRepository.findByGeneralPractitioner(doctor);

        // Patients who have had visits with this doctor
        List<Patient> visitedPatients = visitRepository.findByDoctorId(doctorId).stream()
                .map(visit -> visit.getPatient())
                .toList();

        // Combine and get unique patients
        Set<Patient> uniquePatients = new java.util.HashSet<>(gpPatients);
        uniquePatients.addAll(visitedPatients);

        List<PatientViewDTO> patientViewDTOS = uniquePatients.stream()
                .map(patient -> modelMapper.map(patient, PatientViewDTO.class))
                .collect(Collectors.toList());

        // Manually paginate the list
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), patientViewDTOS.size());
        List<PatientViewDTO> pagedContent = patientViewDTOS.subList(start, end);

        return new PageImpl<>(pagedContent, pageable, patientViewDTOS.size());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPatientAssociatedWithDoctor(Long patientId, Long doctorId) {
        validateIdNotNull(patientId, "isPatientAssociatedWithDoctor");
        validateIdNotNull(doctorId, "isPatientAssociatedWithDoctor");
        logger.debug("Checking association between patient ID {} and doctor ID {}", patientId, doctorId);

        Patient patient = findPatientById(patientId);

        // Check if the doctor is the patient's general practitioner
        if (patient.getGeneralPractitioner() != null && patient.getGeneralPractitioner().getId().equals(doctorId)) {
            return true;
        }

        // Check if the patient has had any visits with this doctor
        return visitRepository.existsByPatientIdAndDoctorId(patientId, doctorId);
    }

    /** {@inheritDoc} */
    @Override
    public Page<PatientViewDTO> findByDiagnosis(Long diagnosisId, Pageable pageable) {
        validateIdNotNull(diagnosisId, "findByDiagnosis");
        logger.debug("Retrieving patients for diagnosis ID: {}", diagnosisId);
        return patientRepository.findByDiagnosis(diagnosisId, pageable)
                .map(patient -> modelMapper.map(patient, PatientViewDTO.class));
    }

    private Patient findPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundById(id)));
    }

    private Doctor findDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(id)));
    }

    private void authorizePatientAccess(String patientKeycloakId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            logger.warn("Authorization attempt with null authentication object.");
            throw new AccessDeniedException(ExceptionMessages.AUTHENTICATION_REQUIRED);
        }

        // Check if the user is a DOCTOR or ADMIN - they have broader access
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR") || a.getAuthority().equals("ROLE_ADMIN"))) {
            logger.debug("Access granted for DOCTOR or ADMIN role. Skipping patient-specific authorization.");
            return;
        }

        // For PATIENT role, ensure they are accessing their own records
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))) {
            String currentUserId = authentication.getName();
            if (!currentUserId.equals(patientKeycloakId)) {
                logger.warn("Access denied for patient {} trying to access records of patient {}", currentUserId, patientKeycloakId);
                throw new AccessDeniedException(ExceptionMessages.PATIENT_ACCESS_DENIED);
            }
        } else {
            logger.warn("Unauthorized access attempt by user with roles: {}", authentication.getAuthorities());
            throw new AccessDeniedException(ExceptionMessages.UNAUTHORIZED_ACCESS);
        }
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
