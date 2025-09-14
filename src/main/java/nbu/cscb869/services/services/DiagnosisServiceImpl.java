//package nbu.cscb869.services.services;
//
//import nbu.cscb869.common.exceptions.EntityNotFoundException;
//import nbu.cscb869.common.exceptions.InvalidDTOException;
//import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
//import nbu.cscb869.data.dto.PatientDiagnosisDTO;
//import nbu.cscb869.data.models.Diagnosis;
//import nbu.cscb869.data.models.Visit;
//import nbu.cscb869.data.repositories.DiagnosisRepository;
//import nbu.cscb869.data.repositories.VisitRepository;
//import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
//import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
//import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
//import nbu.cscb869.services.services.contracts.DiagnosisService;
//import org.modelmapper.ModelMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.*;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
///**
// * Implementation of {@link DiagnosisService} for managing diagnosis operations.
// */
//@Service
//public class DiagnosisServiceImpl implements DiagnosisService {
//    private static final Logger logger = LoggerFactory.getLogger(DiagnosisServiceImpl.class);
//    private static final String ENTITY_NAME = "Diagnosis";
//    private static final int MAX_PAGE_SIZE = 100;
//
//    private final DiagnosisRepository diagnosisRepository;
//    private final VisitRepository visitRepository;
//    private final ModelMapper modelMapper;
//
//    /**
//     * Constructs a new DiagnosisServiceImpl with the specified dependencies.
//     * @param diagnosisRepository the repository for diagnosis entities
//     * @param visitRepository the repository for visit entities
//     * @param modelMapper the ModelMapper for DTO conversions
//     */
//    public DiagnosisServiceImpl(DiagnosisRepository diagnosisRepository, VisitRepository visitRepository, ModelMapper modelMapper) {
//        this.diagnosisRepository = diagnosisRepository;
//        this.visitRepository = visitRepository;
//        this.modelMapper = modelMapper;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    public DiagnosisViewDTO create(DiagnosisCreateDTO dto) {
//        if (dto == null) {
//            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
//            throw new InvalidDTOException("DiagnosisCreateDTO cannot be null");
//        }
//        logger.debug("Creating {} with name: {}", ENTITY_NAME, dto.getName());
//
//        if (diagnosisRepository.findByName(dto.getName()).isPresent()) {
//            throw new InvalidDTOException("Diagnosis with name " + dto.getName() + " already exists");
//        }
//
//        Diagnosis diagnosis = modelMapper.map(dto, Diagnosis.class);
//        diagnosisRepository.save(diagnosis);
//        logger.info("Created {} with ID: {}", ENTITY_NAME, diagnosis.getId());
//        return modelMapper.map(diagnosis, DiagnosisViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    public DiagnosisViewDTO update(DiagnosisUpdateDTO dto) {
//        if (dto == null || dto.getId() == null) {
//            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("DiagnosisUpdateDTO or ID cannot be null");
//        }
//        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
//
//        Diagnosis diagnosis = diagnosisRepository.findById(dto.getId())
//                .orElseThrow(() -> new EntityNotFoundException("Diagnosis not found with ID: " + dto.getId()));
//
//        if (!diagnosis.getName().equals(dto.getName()) && diagnosisRepository.findByName(dto.getName()).isPresent()) {
//            throw new InvalidDTOException("Diagnosis with name " + dto.getName() + " already exists");
//        }
//
//        modelMapper.map(dto, diagnosis);
//        diagnosisRepository.save(diagnosis);
//        logger.info("Updated {} with ID: {}", ENTITY_NAME, diagnosis.getId());
//        return modelMapper.map(diagnosis, DiagnosisViewDTO.class);
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
//        Diagnosis diagnosis = diagnosisRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Diagnosis not found with ID: " + id));
//
//        Page<Visit> visits = visitRepository.findByDiagnosis(diagnosis, PageRequest.of(0, 1));
//        if (!visits.isEmpty()) {
//            logger.error("Cannot delete {} with ID {}: Referenced in active visits", ENTITY_NAME, id);
//            throw new InvalidDTOException("Cannot delete diagnosis referenced in active visits");
//        }
//
//        diagnosis.setIsDeleted(true);
//        diagnosis.setDeletedOn(LocalDateTime.now());
//        diagnosisRepository.save(diagnosis);
//        logger.info("Soft deleted {} with ID: {}", ENTITY_NAME, id);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public DiagnosisViewDTO getById(Long id) {
//        if (id == null) {
//            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
//
//        Diagnosis diagnosis = diagnosisRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Diagnosis not found with ID: " + id));
//        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
//        return modelMapper.map(diagnosis, DiagnosisViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public DiagnosisViewDTO getByName(String name) {
//        if (name == null || name.trim().isEmpty()) {
//            logger.error("Cannot retrieve {}: Name is null or empty", ENTITY_NAME);
//            throw new InvalidDTOException("Name cannot be null or empty");
//        }
//        logger.debug("Retrieving {} with name: {}", ENTITY_NAME, name);
//
//        Diagnosis diagnosis = diagnosisRepository.findByName(name)
//                .orElseThrow(() -> new EntityNotFoundException("Diagnosis not found with name: " + name));
//        logger.info("Retrieved {} with name: {}", ENTITY_NAME, name);
//        return modelMapper.map(diagnosis, DiagnosisViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Async
//    public CompletableFuture<Page<DiagnosisViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}, filter={}",
//                ENTITY_NAME, page, size, orderBy, ascending, filter);
//
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<Diagnosis> diagnoses = (filter == null || filter.trim().isEmpty())
//                ? diagnosisRepository.findAllActive(pageable)
//                : diagnosisRepository.findByNameContainingIgnoreCase(filter.trim(), pageable);
//        Page<DiagnosisViewDTO> result = diagnoses.map(d -> modelMapper.map(d, DiagnosisViewDTO.class));
//        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public Page<PatientDiagnosisDTO> getPatientsByDiagnosis(Long diagnosisId, int page, int size) {
//        if (diagnosisId == null) {
//            logger.error("Cannot retrieve patients: Diagnosis ID is null");
//            throw new InvalidDTOException("Diagnosis ID cannot be null");
//        }
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving patients for Diagnosis ID: {}, page={}, size={}", diagnosisId, page, size);
//
//        Diagnosis diagnosis = diagnosisRepository.findById(diagnosisId)
//                .orElseThrow(() -> new EntityNotFoundException("Diagnosis not found with ID: " + diagnosisId));
//        Pageable pageable = PageRequest.of(page, size);
//        Page<PatientDiagnosisDTO> result = diagnosisRepository.findPatientsByDiagnosis(diagnosis, pageable);
//        logger.info("Retrieved {} patients for Diagnosis ID: {}", result.getTotalElements(), diagnosisId);
//        return result;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public List<DiagnosisVisitCountDTO> getMostFrequentDiagnoses() {
//        logger.debug("Retrieving most frequent diagnoses");
//        List<DiagnosisVisitCountDTO> result = diagnosisRepository.findMostFrequentDiagnoses();
//        logger.info("Retrieved {} frequent diagnoses", result.size());
//        return result;
//    }
//}