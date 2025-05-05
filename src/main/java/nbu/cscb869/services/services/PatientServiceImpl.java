package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.services.common.exceptions.InvalidPatientException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static nbu.cscb869.common.exceptions.ExceptionMessages.formatDoctorNotFoundById;

/**
 * Implementation of {@link PatientService} for managing patient-related operations.
 */
@Service
public class PatientServiceImpl implements PatientService {
    private static final Logger logger = LoggerFactory.getLogger(PatientServiceImpl.class);

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;

    public PatientServiceImpl(PatientRepository patientRepository, DoctorRepository doctorRepository, ModelMapper modelMapper) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public PatientViewDTO create(PatientCreateDTO createDTO) {
        if (createDTO == null) {
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("PatientCreateDTO"));
        }

        // Validate EGN uniqueness
        if (patientRepository.findByEgn(createDTO.getEgn()).isPresent()) {
            throw new InvalidPatientException(ExceptionMessages.formatPatientEgnExists(createDTO.getEgn()));
        }

        // Validate general practitioner
        Doctor gp = doctorRepository.findById(createDTO.getGeneralPractitionerId())
                .orElseThrow(() -> new EntityNotFoundException(formatDoctorNotFoundById(createDTO.getGeneralPractitionerId())));
        if (!gp.isGeneralPractitioner()) {
            throw new InvalidPatientException(ExceptionMessages.formatInvalidGeneralPractitioner(createDTO.getGeneralPractitionerId()));
        }

        Patient patient = modelMapper.map(createDTO, Patient.class);
        patient.setGeneralPractitioner(gp);
        patient = patientRepository.save(patient);
        logger.info("Created patient with ID: {}", patient.getId());

        return modelMapper.map(patient, PatientViewDTO.class);
    }

    @Override
    @Transactional
    public PatientViewDTO update(PatientUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("PatientUpdateDTO"));
        }
        if (updateDTO.getId() == null) {
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }

        Patient patient = patientRepository.findById(updateDTO.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        ExceptionMessages.formatPatientNotFoundById(updateDTO.getId())));

        // Validate general practitioner
        Doctor gp = doctorRepository.findById(updateDTO.getGeneralPractitionerId())
                .orElseThrow(() -> new EntityNotFoundException(formatDoctorNotFoundById(updateDTO.getGeneralPractitionerId())));
        if (!gp.isGeneralPractitioner()) {
            throw new InvalidPatientException(ExceptionMessages.formatInvalidGeneralPractitioner(updateDTO.getGeneralPractitionerId()));
        }

        modelMapper.map(updateDTO, patient);
        patient.setGeneralPractitioner(gp);
        patient = patientRepository.save(patient);
        logger.info("Updated patient with ID: {}", patient.getId());

        return modelMapper.map(patient, PatientViewDTO.class);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ExceptionMessages.formatPatientNotFoundById(id)));

        patientRepository.delete(patient); // Soft delete via @SQLDelete
        logger.info("Soft deleted patient with ID: {}", id);
    }

    @Override
    public PatientViewDTO getById(Long id) {
        if (id == null) {
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ExceptionMessages.formatPatientNotFoundById(id)));

        return modelMapper.map(patient, PatientViewDTO.class);
    }

    @Override
    public PatientViewDTO getByEgn(String egn) {
        if (egn == null || egn.trim().isEmpty()) {
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldEmpty("EGN"));
        }

        Patient patient = patientRepository.findByEgn(egn)
                .orElseThrow(() -> new EntityNotFoundException(
                        ExceptionMessages.formatPatientNotFoundByEgn(egn)));

        return modelMapper.map(patient, PatientViewDTO.class);
    }

    @Override
    @Async
    public CompletableFuture<Page<PatientViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
        if (page < 0) {
            throw new InvalidDTOException("Page number must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new InvalidDTOException("Page size must be between 1 and 100");
        }

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Patient> patients;
        if (filter == null || filter.trim().isEmpty()) {
            patients = patientRepository.findAllActive(pageable);
        } else {
            String filterPattern = "%" + filter.trim().toLowerCase() + "%";
            patients = patientRepository.findByNameOrEgnContaining(filterPattern, pageable);
        }

        Page<PatientViewDTO> result = patients.map(patient -> modelMapper.map(patient, PatientViewDTO.class));
        logger.info("Retrieved {} patients for page {}, size {}", result.getTotalElements(), page, size);

        return CompletableFuture.completedFuture(result);
    }

    @Override
    public Page<PatientViewDTO> getByGeneralPractitioner(Long generalPractitionerId, Pageable pageable) {
        Doctor gp = doctorRepository.findById(generalPractitionerId)
                .orElseThrow(() -> new EntityNotFoundException(formatDoctorNotFoundById(generalPractitionerId))
                        );
        if (!gp.isGeneralPractitioner()) {
            throw new InvalidPatientException(ExceptionMessages.formatInvalidGeneralPractitioner(generalPractitionerId));
        }

        Page<Patient> patients = patientRepository.findByGeneralPractitioner(gp, pageable);
        return patients.map(patient -> modelMapper.map(patient, PatientViewDTO.class));
    }

    @Override
    public List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner() {
        return patientRepository.countPatientsByGeneralPractitioner();
    }
}