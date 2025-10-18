package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityInUseException;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.services.data.dtos.SpecialtyCreateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyUpdateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.contracts.SpecialtyService;
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

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link SpecialtyService} for managing specialty operations.
 */
@Service
public class SpecialtyServiceImpl implements SpecialtyService {
    private static final Logger logger = LoggerFactory.getLogger(SpecialtyServiceImpl.class);
    private static final String ENTITY_NAME = "Specialty";
    private static final int MAX_PAGE_SIZE = 100;

    private final SpecialtyRepository specialtyRepository;
    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new SpecialtyServiceImpl with the specified dependencies.
     *
     * @param specialtyRepository the repository for specialty entities
     * @param doctorRepository    the repository for doctor entities
     * @param modelMapper         the ModelMapper for DTO conversions
     */
    public SpecialtyServiceImpl(SpecialtyRepository specialtyRepository, DoctorRepository doctorRepository, ModelMapper modelMapper) {
        this.specialtyRepository = specialtyRepository;
        this.doctorRepository = doctorRepository;
        this.modelMapper = modelMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SpecialtyViewDTO create(SpecialtyCreateDTO dto) {
        validateDtoNotNull(dto, "create");
        logger.debug("Creating {} with name: {}", ENTITY_NAME, dto.getName());

        if (specialtyRepository.findByName(dto.getName()).isPresent()) {
            throw new InvalidDTOException(ExceptionMessages.formatSpecialtyNameExists(dto.getName()));
        }

        Specialty specialty = modelMapper.map(dto, Specialty.class);
        Specialty savedSpecialty = specialtyRepository.save(specialty);

        logger.info("Created {} with ID: {}", ENTITY_NAME, savedSpecialty.getId());
        return modelMapper.map(savedSpecialty, SpecialtyViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SpecialtyViewDTO update(SpecialtyUpdateDTO dto) {
        validateDtoNotNull(dto, "update");
        validateIdNotNull(dto.getId(), "update");
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        Specialty specialty = specialtyRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatSpecialtyNotFoundByName("with ID: " + dto.getId())));

        specialtyRepository.findByName(dto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(dto.getId())) {
                throw new InvalidDTOException(ExceptionMessages.formatSpecialtyNameExists(dto.getName()));
            }
        });

        specialty.setName(dto.getName());
        specialty.setDescription(dto.getDescription());

        Specialty updatedSpecialty = specialtyRepository.save(specialty);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, updatedSpecialty.getId());
        return modelMapper.map(updatedSpecialty, SpecialtyViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        validateIdNotNull(id, "delete");
        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);

        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatSpecialtyNotFoundByName("with ID: " + id)));

        if (doctorRepository.existsBySpecialtiesContains(specialty)) {
            throw new EntityInUseException(ExceptionMessages.formatSpecialtyInUse(id));
        }

        specialtyRepository.delete(specialty);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    public SpecialtyViewDTO getById(Long id) {
        validateIdNotNull(id, "getById");
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatSpecialtyNotFoundByName("with ID: " + id)));

        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
        return modelMapper.map(specialty, SpecialtyViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<SpecialtyViewDTO>> getAll(int page, int size, String orderBy, boolean ascending) {
        validatePagination(page, size, "getAll");
        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}", ENTITY_NAME, page, size, orderBy, ascending);

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Specialty> specialties = specialtyRepository.findAll(pageable);

        Page<SpecialtyViewDTO> result = specialties.map(s -> modelMapper.map(s, SpecialtyViewDTO.class));
        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
        return CompletableFuture.completedFuture(result);
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
