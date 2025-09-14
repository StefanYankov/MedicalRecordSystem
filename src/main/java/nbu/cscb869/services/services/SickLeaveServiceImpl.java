//package nbu.cscb869.services.services;
//
//import nbu.cscb869.common.exceptions.EntityNotFoundException;
//import nbu.cscb869.common.exceptions.InvalidDTOException;
//import nbu.cscb869.common.exceptions.InvalidInputException;
//import nbu.cscb869.data.models.SickLeave;
//import nbu.cscb869.data.models.Visit;
//import nbu.cscb869.data.repositories.SickLeaveRepository;
//import nbu.cscb869.data.repositories.VisitRepository;
//import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
//import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
//import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
//import nbu.cscb869.services.services.contracts.SickLeaveService;
//import org.modelmapper.ModelMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.*;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.concurrent.CompletableFuture;
//
///**
// * Implementation of {@link SickLeaveService} for managing sick leave-related operations.
// */
//@Service
//public class SickLeaveServiceImpl implements SickLeaveService {
//    private static final Logger logger = LoggerFactory.getLogger(SickLeaveServiceImpl.class);
//    private static final String ENTITY_NAME = "SickLeave";
//    private static final int MAX_PAGE_SIZE = 100;
//
//    private final SickLeaveRepository sickLeaveRepository;
//    private final VisitRepository visitRepository;
//    private final ModelMapper modelMapper;
//
//    public SickLeaveServiceImpl(SickLeaveRepository sickLeaveRepository, VisitRepository visitRepository, ModelMapper modelMapper) {
//        this.sickLeaveRepository = sickLeaveRepository;
//        this.visitRepository = visitRepository;
//        this.modelMapper = modelMapper;
//    }
//
//    @Override
//    @Transactional
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public SickLeaveViewDTO create(SickLeaveCreateDTO dto) {
//        if (dto == null) {
//            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
//            throw new InvalidDTOException("SickLeaveCreateDTO cannot be null");
//        }
//        logger.debug("Creating {} for visit ID: {}", ENTITY_NAME, dto.getVisitId());
//
//        Visit visit = visitRepository.findById(dto.getVisitId())
//                .orElseThrow(() -> new EntityNotFoundException("Visit not found with ID: " + dto.getVisitId()));
//        if (!visit.isSickLeaveIssued()) {
//            logger.error("Visit ID: {} does not have sick leave issued", dto.getVisitId());
//            throw new InvalidInputException("Cannot create sick leave for visit without sick leave issued");
//        }
//
//        SickLeave sickLeave = modelMapper.map(dto, SickLeave.class);
//        sickLeave.setVisit(visit);
//        sickLeave = sickLeaveRepository.save(sickLeave);
//        logger.info("Created {} with ID: {}", ENTITY_NAME, sickLeave.getId());
//        return modelMapper.map(sickLeave, SickLeaveViewDTO.class);
//    }
//
//    @Override
//    @Transactional
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public SickLeaveViewDTO update(SickLeaveUpdateDTO dto) {
//        if (dto == null || dto.getId() == null) {
//            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("SickLeaveUpdateDTO or ID cannot be null");
//        }
//        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
//
//        SickLeave sickLeave = sickLeaveRepository.findById(dto.getId())
//                .orElseThrow(() -> new EntityNotFoundException("SickLeave not found with ID: " + dto.getId()));
//
//        Visit visit = visitRepository.findById(dto.getVisitId())
//                .orElseThrow(() -> new EntityNotFoundException("Visit not found with ID: " + dto.getVisitId()));
//        if (!visit.isSickLeaveIssued()) {
//            logger.error("Visit ID: {} does not have sick leave issued", dto.getVisitId());
//            throw new InvalidInputException("Cannot update sick leave for visit without sick leave issued");
//        }
//
//        modelMapper.map(dto, sickLeave);
//        sickLeave.setVisit(visit);
//        sickLeave = sickLeaveRepository.save(sickLeave);
//        logger.info("Updated {} with ID: {}", ENTITY_NAME, sickLeave.getId());
//        return modelMapper.map(sickLeave, SickLeaveViewDTO.class);
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
//        SickLeave sickLeave = sickLeaveRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("SickLeave not found with ID: " + id));
//
//        sickLeave.setIsDeleted(true);
//        sickLeave.setDeletedOn(LocalDateTime.now());
//        sickLeaveRepository.save(sickLeave);
//        logger.info("Soft deleted {} with ID: {}", ENTITY_NAME, id);
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
//    public SickLeaveViewDTO getById(Long id) {
//        if (id == null) {
//            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
//
//        SickLeave sickLeave = sickLeaveRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("SickLeave not found with ID: " + id));
//        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
//        return modelMapper.map(sickLeave, SickLeaveViewDTO.class);
//    }
//
//    @Override
//    @Async
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public CompletableFuture<Page<SickLeaveViewDTO>> getAll(int page, int size, String orderBy, boolean ascending) {
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}",
//                ENTITY_NAME, page, size, orderBy, ascending);
//
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<SickLeave> sickLeaves = sickLeaveRepository.findAllActive(pageable);
//        Page<SickLeaveViewDTO> result = sickLeaves.map(sl -> modelMapper.map(sl, SickLeaveViewDTO.class));
//        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
//        return CompletableFuture.completedFuture(result);
//    }
//}