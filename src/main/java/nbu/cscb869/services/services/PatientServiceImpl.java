//package nbu.cscb869.services.services;
//
//import nbu.cscb869.common.exceptions.EntityNotFoundException;
//import nbu.cscb869.common.exceptions.InvalidDTOException;
//import nbu.cscb869.common.exceptions.InvalidPatientException;
//import nbu.cscb869.data.dto.DoctorPatientCountDTO;
//import nbu.cscb869.data.models.Doctor;
//import nbu.cscb869.data.models.Patient;
//import nbu.cscb869.data.repositories.DoctorRepository;
//import nbu.cscb869.data.repositories.PatientRepository;
//import nbu.cscb869.services.data.dtos.PatientCreateDTO;
//import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
//import nbu.cscb869.services.data.dtos.PatientViewDTO;
//import nbu.cscb869.services.services.contracts.PatientService;
//import org.modelmapper.ModelMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.*;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
///**
// * Implementation of {@link PatientService} for managing patient operations.
// */
//@Service
//public class PatientServiceImpl implements PatientService {
//    private static final Logger logger = LoggerFactory.getLogger(PatientServiceImpl.class);
//    private static final String ENTITY_NAME = "Patient";
//    private static final int MAX_PAGE_SIZE = 100;
//
//    private final PatientRepository patientRepository;
//    private final DoctorRepository doctorRepository;
//    private final ModelMapper modelMapper;
//
//    public PatientServiceImpl(PatientRepository patientRepository, DoctorRepository doctorRepository, ModelMapper modelMapper) {
//        this.patientRepository = patientRepository;
//        this.doctorRepository = doctorRepository;
//        this.modelMapper = modelMapper;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    public PatientViewDTO create(PatientCreateDTO dto) {
//        if (dto == null) {
//            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
//            throw new InvalidDTOException("PatientCreateDTO cannot be null");
//        }
//        logger.debug("Creating {} with EGN: {}", ENTITY_NAME, dto.getEgn());
//
//        if (patientRepository.findByEgn(dto.getEgn()).isPresent()) {
//            throw new InvalidPatientException("Patient with EGN " + dto.getEgn() + " already exists");
//        }
//
//        Doctor gp = doctorRepository.findById(dto.getGeneralPractitionerId())
//                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + dto.getGeneralPractitionerId()));
//        if (!gp.isGeneralPractitioner()) {
//            throw new InvalidPatientException("Doctor with ID " + dto.getGeneralPractitionerId() + " is not a general practitioner");
//        }
//
//        Patient patient = modelMapper.map(dto, Patient.class);
//        patient.setGeneralPractitioner(gp);
//        patientRepository.save(patient);
//        logger.info("Created {} with ID: {}", ENTITY_NAME, patient.getId());
//        return modelMapper.map(patient, PatientViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    public PatientViewDTO update(PatientUpdateDTO dto) {
//        if (dto == null || dto.getId() == null) {
//            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("PatientUpdateDTO or ID cannot be null");
//        }
//        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
//
//        Patient patient = patientRepository.findById(dto.getId())
//                .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + dto.getId()));
//
//        if (!patient.getEgn().equals(dto.getEgn()) && patientRepository.findByEgn(dto.getEgn()).isPresent()) {
//            throw new InvalidPatientException("EGN " + dto.getEgn() + " is already in use");
//        }
//
//        Doctor gp = doctorRepository.findById(dto.getGeneralPractitionerId())
//                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + dto.getGeneralPractitionerId()));
//        if (!gp.isGeneralPractitioner()) {
//            throw new InvalidPatientException("Doctor with ID " + dto.getGeneralPractitionerId() + " is not a general practitioner");
//        }
//
//        modelMapper.map(dto, patient);
//        patient.setGeneralPractitioner(gp);
//        patientRepository.save(patient);
//        logger.info("Updated {} with ID: {}", ENTITY_NAME, patient.getId());
//        return modelMapper.map(patient, PatientViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    public void delete(Long id) {
//        if (id == null) {
//            logger.error("Cannot delete {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);
//
//        Patient patient = patientRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + id));
//
//        patient.setIsDeleted(true);
//        patientRepository.save(patient);
//        logger.info("Soft deleted {} with ID: {}", ENTITY_NAME, id);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public PatientViewDTO getById(Long id) {
//        if (id == null) {
//            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
//
//        Patient patient = patientRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Patient not found with ID: " + id));
//        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
//        return modelMapper.map(patient, PatientViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public PatientViewDTO getByEgn(String egn) {
//        if (egn == null || egn.trim().isEmpty()) {
//            logger.error("Cannot retrieve {}: EGN is null or empty", ENTITY_NAME);
//            throw new InvalidDTOException("EGN cannot be null or empty");
//        }
//        logger.debug("Retrieving {} with EGN: {}", ENTITY_NAME, egn);
//
//        Patient patient = patientRepository.findByEgn(egn)
//                .orElseThrow(() -> new EntityNotFoundException("Patient not found with EGN: " + egn));
//        logger.info("Retrieved {} with EGN: {}", ENTITY_NAME, egn);
//        return modelMapper.map(patient, PatientViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Async
//    public CompletableFuture<Page<PatientViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}, filter={}", ENTITY_NAME, page, size, orderBy, ascending, filter);
//
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<Patient> patients = (filter == null || filter.trim().isEmpty())
//                ? patientRepository.findAllActive(pageable)
//                : patientRepository.findByEgnContaining("%" + filter.trim().toLowerCase() + "%", pageable);
//        Page<PatientViewDTO> result = patients.map(p -> modelMapper.map(p, PatientViewDTO.class));
//        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public Page<PatientViewDTO> getByGeneralPractitioner(Long generalPractitionerId, int page, int size) {
//        if (generalPractitionerId == null) {
//            logger.error("Cannot retrieve {}: General Practitioner ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("General Practitioner ID cannot be null");
//        }
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving {} for General Practitioner ID: {}, page={}, size={}", ENTITY_NAME, generalPractitionerId, page, size);
//
//        Doctor gp = doctorRepository.findById(generalPractitionerId)
//                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + generalPractitionerId));
//        if (!gp.isGeneralPractitioner()) {
//            throw new InvalidPatientException("Doctor with ID " + generalPractitionerId + " is not a general practitioner");
//        }
//
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Patient> patients = patientRepository.findByGeneralPractitioner(gp, pageable);
//        Page<PatientViewDTO> result = patients.map(p -> modelMapper.map(p, PatientViewDTO.class));
//        logger.info("Retrieved {} {} for General Practitioner ID: {}", result.getTotalElements(), ENTITY_NAME, generalPractitionerId);
//        return result;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner() {
//        logger.debug("Retrieving patient count by General Practitioner");
//        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();
//        logger.info("Retrieved {} General Practitioners with patient counts", result.size());
//        return result;
//    }
//}