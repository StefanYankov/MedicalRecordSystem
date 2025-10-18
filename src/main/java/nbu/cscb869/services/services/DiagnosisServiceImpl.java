package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.PatientDiagnosisDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DiagnosisServiceImpl implements DiagnosisService {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosisServiceImpl.class);
    private static final String ENTITY_NAME = "Diagnosis";
    private static final int MAX_PAGE_SIZE = 100;

    private final DiagnosisRepository diagnosisRepository;
    private final VisitRepository visitRepository;
    private final ModelMapper modelMapper;

    public DiagnosisServiceImpl(final DiagnosisRepository diagnosisRepository, final VisitRepository visitRepository, final ModelMapper modelMapper) {
        this.diagnosisRepository = diagnosisRepository;
        this.visitRepository = visitRepository;
        this.modelMapper = modelMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DiagnosisViewDTO create(final DiagnosisCreateDTO dto) {
        if (dto == null) {
            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("DiagnosisCreateDTO"));
        }

        if (diagnosisRepository.findByName(dto.getName()).isPresent()) {
            throw new InvalidDTOException(ExceptionMessages.formatDiagnosisNameExists(dto.getName()));
        }

        logger.debug("Creating {} with name: {}", ENTITY_NAME, dto.getName());
        Diagnosis diagnosis = modelMapper.map(dto, Diagnosis.class);
        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        logger.info("Created {} with ID: {}", ENTITY_NAME, savedDiagnosis.getId());

        return modelMapper.map(savedDiagnosis, DiagnosisViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DiagnosisViewDTO update(final DiagnosisUpdateDTO dto) {
        if (dto == null || dto.getId() == null) {
            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("DiagnosisUpdateDTO or ID"));
        }

        Diagnosis diagnosis = diagnosisRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDiagnosisNotFoundById(dto.getId())));

        diagnosisRepository.findByName(dto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(diagnosis.getId())) {
                throw new InvalidDTOException(ExceptionMessages.formatDiagnosisNameExists(dto.getName()));
            }
        });

        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
        modelMapper.map(dto, diagnosis);
        Diagnosis updatedDiagnosis = diagnosisRepository.save(diagnosis);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, updatedDiagnosis.getId());

        return modelMapper.map(updatedDiagnosis, DiagnosisViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(final Long id) {
        if (id == null) {
            logger.error("Cannot delete {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }

        Diagnosis diagnosis = diagnosisRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDiagnosisNotFoundById(id)));

        if (!visitRepository.findByDiagnosis(diagnosis, PageRequest.of(0, 1)).isEmpty()) {
            throw new InvalidDTOException(ExceptionMessages.formatDiagnosisInUse(id));
        }

        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);
        diagnosisRepository.delete(diagnosis);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public DiagnosisViewDTO getById(final Long id) {
        if (id == null) {
            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }

        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
        Diagnosis diagnosis = diagnosisRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDiagnosisNotFoundById(id)));
        return modelMapper.map(diagnosis, DiagnosisViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    public DiagnosisViewDTO getByName(final String name) {
        if (name == null || name.isBlank()) {
            logger.error("Cannot retrieve {}: name is null or empty", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldEmpty("Name"));
        }

        logger.debug("Retrieving {} with name: {}", ENTITY_NAME, name);
        Diagnosis diagnosis = diagnosisRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDiagnosisNotFoundByName(name)));
        return modelMapper.map(diagnosis, DiagnosisViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<DiagnosisViewDTO>> getAll(final int page, final int size, final String orderBy, final boolean ascending, final String filter) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Invalid pagination: page={}, size={}", page, size);
            throw new InvalidDTOException("Invalid pagination parameters.");
        }

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Diagnosis> diagnoses = (filter == null || filter.trim().isEmpty())
                ? diagnosisRepository.findAll(pageable)
                : diagnosisRepository.findByNameContainingIgnoreCase(filter, pageable);

        Page<DiagnosisViewDTO> result = diagnoses.map(d -> modelMapper.map(d, DiagnosisViewDTO.class));
        return CompletableFuture.completedFuture(result);
    }

    /** {@inheritDoc} */
    @Override
    public Page<PatientDiagnosisDTO> getPatientsByDiagnosis(final Long diagnosisId, final int page, final int size) {
        if (diagnosisId == null) {
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("Diagnosis ID"));
        }
        Diagnosis diagnosis = diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDiagnosisNotFoundById(diagnosisId)));

        Pageable pageable = PageRequest.of(page, size);
        return diagnosisRepository.findPatientsByDiagnosis(diagnosis, pageable);
    }

    /** {@inheritDoc} */
    @Override
    public List<DiagnosisVisitCountDTO> getMostFrequentDiagnoses() {
        return diagnosisRepository.findMostFrequentDiagnoses();
    }
}
