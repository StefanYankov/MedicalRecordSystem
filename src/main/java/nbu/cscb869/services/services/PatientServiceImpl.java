package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidPatientException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.PatientService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link PatientService} for managing patient operations.
 */
@Service
public class PatientServiceImpl implements PatientService {
    private static final Logger logger = LoggerFactory.getLogger(PatientServiceImpl.class);
    private static final String ENTITY_NAME = "Patient";
    private static final int MAX_PAGE_SIZE = 100;

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;

    public PatientServiceImpl(PatientRepository patientRepository, DoctorRepository doctorRepository, ModelMapper modelMapper) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.modelMapper = modelMapper;
    }

    private String getCurrentUserKeycloakId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("User not authenticated or Keycloak ID not available.");
        }
        return jwt.getSubject(); // Keycloak user ID is typically in the 'sub' claim
    }

    private boolean isCurrentUserPatient() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_PATIENT"));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PatientViewDTO create(PatientCreateDTO dto) {
        if (dto == null) {
            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("PatientCreateDTO"));
        }
        if (dto.getKeycloakId() == null || dto.getKeycloakId().isBlank()) {
            logger.error("Cannot create {}: Keycloak ID is missing for admin creation", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatKeycloakIdMissingForAdminCreation());
        }
        logger.debug("Creating {} with EGN: {} and Keycloak ID: {}", ENTITY_NAME, dto.getEgn(), dto.getKeycloakId());

        if (patientRepository.findByEgn(dto.getEgn()).isPresent()) {
            throw new InvalidPatientException(ExceptionMessages.formatPatientEgnExists(dto.getEgn()));
        }
        if (patientRepository.findByKeycloakId(dto.getKeycloakId()).isPresent()) {
            throw new InvalidPatientException(ExceptionMessages.formatPatientKeycloakIdExists(dto.getKeycloakId()));
        }

        Doctor gp = doctorRepository.findById(dto.getGeneralPractitionerId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(dto.getGeneralPractitionerId())));
        if (!gp.isGeneralPractitioner()) {
            throw new InvalidPatientException(ExceptionMessages.formatInvalidGeneralPractitioner(dto.getGeneralPractitionerId()));
        }

        Patient patient = modelMapper.map(dto, Patient.class);
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);
        logger.info("Created {} with ID: {}", ENTITY_NAME, patient.getId());
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PatientViewDTO registerPatient(PatientCreateDTO dto) {
        if (dto == null) {
            logger.error("Cannot register {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("PatientCreateDTO"));
        }
        String keycloakId = getCurrentUserKeycloakId();
        logger.debug("Registering patient with EGN: {} for Keycloak ID: {}", dto.getEgn(), keycloakId);

        if (patientRepository.findByEgn(dto.getEgn()).isPresent()) {
            throw new InvalidPatientException(ExceptionMessages.formatPatientEgnExists(dto.getEgn()));
        }
        if (patientRepository.findByKeycloakId(keycloakId).isPresent()) {
            throw new InvalidPatientException(ExceptionMessages.formatPatientKeycloakIdExists(keycloakId));
        }

        Doctor gp = doctorRepository.findById(dto.getGeneralPractitionerId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(dto.getGeneralPractitionerId())));
        if (!gp.isGeneralPractitioner()) {
            throw new InvalidPatientException(ExceptionMessages.formatInvalidGeneralPractitioner(dto.getGeneralPractitionerId()));
        }

        Patient patient = modelMapper.map(dto, Patient.class);
        patient.setKeycloakId(keycloakId);
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);
        logger.info("Registered patient with ID: {} for Keycloak ID: {}", patient.getId(), keycloakId);
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PatientViewDTO update(PatientUpdateDTO dto) {
        if (dto == null) {
            logger.error("Cannot update {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("PatientUpdateDTO"));
        }
        if (dto.getId() == null) {
            logger.error("Cannot update {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        Patient patient = patientRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundById(dto.getId())));

        if (isCurrentUserPatient() && !patient.getKeycloakId().equals(getCurrentUserKeycloakId())) {
            throw new AccessDeniedException("Patients can only update their own records.");
        }

        if (!patient.getEgn().equals(dto.getEgn()) && patientRepository.findByEgn(dto.getEgn()).isPresent()) {
            throw new InvalidPatientException(ExceptionMessages.formatPatientEgnExists(dto.getEgn()));
        }

        if (dto.getKeycloakId() != null && !dto.getKeycloakId().isBlank() && !patient.getKeycloakId().equals(dto.getKeycloakId())) {
            if (patientRepository.findByKeycloakId(dto.getKeycloakId()).isPresent()) {
                throw new InvalidPatientException(ExceptionMessages.formatPatientKeycloakIdExists(dto.getKeycloakId()));
            }
        }

        Doctor gp = doctorRepository.findById(dto.getGeneralPractitionerId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(dto.getGeneralPractitionerId())));
        if (!gp.isGeneralPractitioner()) {
            throw new InvalidPatientException(ExceptionMessages.formatInvalidGeneralPractitioner(dto.getGeneralPractitionerId()));
        }

        modelMapper.map(dto, patient);
        patient.setGeneralPractitioner(gp);

        if (dto.getKeycloakId() != null && !dto.getKeycloakId().isBlank()) {
            patient.setKeycloakId(dto.getKeycloakId());
        }
        patientRepository.save(patient);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, patient.getId());
        return modelMapper.map(patient, PatientViewDTO.class);
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

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundById(id)));

        patientRepository.delete(patient);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public PatientViewDTO getById(Long id) {
        if (id == null) {
            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundById(id)));

        if (isCurrentUserPatient() && !patient.getKeycloakId().equals(getCurrentUserKeycloakId())) {
            throw new AccessDeniedException("Patients can only view their own records.");
        }

        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    public PatientViewDTO getByEgn(String egn) {
        if (egn == null || egn.trim().isEmpty()) {
            logger.error("Cannot retrieve {}: EGN is null or empty", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldEmpty("EGN"));
        }
        logger.debug("Retrieving {} with EGN: {}", ENTITY_NAME, egn);

        Patient patient = patientRepository.findByEgn(egn)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundByEgn(egn)));

        if (isCurrentUserPatient() && !patient.getKeycloakId().equals(getCurrentUserKeycloakId())) {
            throw new AccessDeniedException("Patients can only view their own records.");
        }

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
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatPatientNotFoundByKeycloakId(keycloakId)));

        if (isCurrentUserPatient() && !patient.getKeycloakId().equals(getCurrentUserKeycloakId())) {
            throw new AccessDeniedException("Patients can only view their own records.");
        }

        logger.info("Retrieved {} with Keycloak ID: {}", ENTITY_NAME, keycloakId);
        return modelMapper.map(patient, PatientViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<PatientViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid pagination: page={}, size={}", page, size);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("Pagination parameters"));
        }
        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}, filter={}", ENTITY_NAME, page, size, orderBy, ascending, filter);

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Patient> patients = (filter == null || filter.trim().isEmpty())
                ? patientRepository.findAll(pageable)
                : patientRepository.findByEgnContaining("%" + filter.trim().toLowerCase() + "%", pageable);
        Page<PatientViewDTO> result = patients.map(p -> modelMapper.map(p, PatientViewDTO.class));
        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
        return CompletableFuture.completedFuture(result);
    }

    /** {@inheritDoc} */
    @Override
    public Page<PatientViewDTO> getByGeneralPractitioner(Long generalPractitionerId, int page, int size) {
        if (generalPractitionerId == null) {
            logger.error("Cannot retrieve {}: General Practitioner ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("General Practitioner ID"));
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid pagination: page={}, size={}", page, size);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("Pagination parameters"));
        }
        logger.debug("Retrieving {} for General Practitioner ID: {}, page={}, size={}", ENTITY_NAME, generalPractitionerId, page, size);

        Doctor gp = doctorRepository.findById(generalPractitionerId)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(generalPractitionerId)));
        if (!gp.isGeneralPractitioner()) {
            throw new InvalidPatientException(ExceptionMessages.formatInvalidGeneralPractitioner(generalPractitionerId));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Patient> patients = patientRepository.findByGeneralPractitioner(gp, pageable);
        Page<PatientViewDTO> result = patients.map(p -> modelMapper.map(p, PatientViewDTO.class));
        logger.info("Retrieved {} {} for General Practitioner ID: {}", result.getTotalElements(), ENTITY_NAME, generalPractitionerId);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner() {
        logger.debug("Retrieving patient count by General Practitioner");
        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();
        logger.info("Retrieved {} General Practitioners with patient counts", result.size());
        return result;
    }
}