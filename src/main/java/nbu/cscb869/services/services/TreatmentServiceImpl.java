//package nbu.cscb869.services.services;
//
//import nbu.cscb869.common.exceptions.EntityNotFoundException;
//import nbu.cscb869.common.exceptions.InvalidDTOException;
//import nbu.cscb869.common.exceptions.InvalidInputException;
//import nbu.cscb869.data.models.Medicine;
//import nbu.cscb869.data.models.Treatment;
//import nbu.cscb869.data.models.Visit;
//import nbu.cscb869.data.repositories.MedicineRepository;
//import nbu.cscb869.data.repositories.TreatmentRepository;
//import nbu.cscb869.data.repositories.VisitRepository;
//import nbu.cscb869.services.data.dtos.DoctorViewDTO;
//import nbu.cscb869.services.data.dtos.TreatmentCreateDTO;
//import nbu.cscb869.services.data.dtos.TreatmentUpdateDTO;
//import nbu.cscb869.services.data.dtos.TreatmentViewDTO;
//import nbu.cscb869.services.data.dtos.identity.UserViewDTO;
//import nbu.cscb869.services.services.contracts.TreatmentService;
//import nbu.cscb869.services.services.contracts.UserService;
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
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
///**
// * Implementation of {@link TreatmentService} for managing treatment-related operations.
// */
//@Service
//public class TreatmentServiceImpl implements TreatmentService {
//    private static final Logger logger = LoggerFactory.getLogger(TreatmentServiceImpl.class);
//    private static final String ENTITY_NAME = "Treatment";
//    private static final int MAX_PAGE_SIZE = 100;
//
//    private final TreatmentRepository treatmentRepository;
//    private final VisitRepository visitRepository;
//    private final MedicineRepository medicineRepository;
//    private final UserService userService;
//    private final ModelMapper modelMapper;
//
//    public TreatmentServiceImpl(TreatmentRepository treatmentRepository, VisitRepository visitRepository,
//                                MedicineRepository medicineRepository, UserService userService, ModelMapper modelMapper) {
//        this.treatmentRepository = treatmentRepository;
//        this.visitRepository = visitRepository;
//        this.medicineRepository = medicineRepository;
//        this.userService = userService;
//        this.modelMapper = modelMapper;
//    }
//
//    @Override
//    @Transactional
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public TreatmentViewDTO create(TreatmentCreateDTO dto) {
//        if (dto == null) {
//            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
//            throw new InvalidDTOException("TreatmentCreateDTO cannot be null");
//        }
//        logger.debug("Creating {} for visit ID: {}", ENTITY_NAME, dto.getVisitId());
//
//        Visit visit = visitRepository.findById(dto.getVisitId())
//                .orElseThrow(() -> new EntityNotFoundException("Visit not found with ID: " + dto.getVisitId()));
//
//        validateDoctorOwnership(visit.getDoctor().getId());
//
//        List<Medicine> medicines = validateAndGetMedicines(dto.getMedicineIds());
//
//        Treatment treatment = modelMapper.map(dto, Treatment.class);
//        treatment.setVisit(visit);
//        treatment.setMedicines(medicines);
//        treatment = treatmentRepository.save(treatment);
//        logger.info("Created {} with ID: {}", ENTITY_NAME, treatment.getId());
//        return modelMapper.map(treatment, TreatmentViewDTO.class);
//    }
//
//    @Override
//    @Transactional
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public TreatmentViewDTO update(TreatmentUpdateDTO dto) {
//        if (dto == null || dto.getId() == null) {
//            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("TreatmentUpdateDTO or ID cannot be null");
//        }
//        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
//
//        Treatment treatment = treatmentRepository.findById(dto.getId())
//                .orElseThrow(() -> new EntityNotFoundException("Treatment not found with ID: " + dto.getId()));
//
//        Visit visit = visitRepository.findById(dto.getVisitId())
//                .orElseThrow(() -> new EntityNotFoundException("Visit not found with ID: " + dto.getVisitId()));
//
//        validateDoctorOwnership(visit.getDoctor().getId());
//
//        List<Medicine> medicines = validateAndGetMedicines(dto.getMedicineIds());
//
//        modelMapper.map(dto, treatment);
//        treatment.setVisit(visit);
//        treatment.setMedicines(medicines);
//        treatment = treatmentRepository.save(treatment);
//        logger.info("Updated {} with ID: {}", ENTITY_NAME, treatment.getId());
//        return modelMapper.map(treatment, TreatmentViewDTO.class);
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
//        Treatment treatment = treatmentRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Treatment not found with ID: " + id));
//
//        treatment.setIsDeleted(true);
//        treatment.setDeletedOn(LocalDateTime.now());
//        treatmentRepository.save(treatment);
//        logger.info("Soft deleted {} with ID: {}", ENTITY_NAME, id);
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
//    public TreatmentViewDTO getById(Long id) {
//        if (id == null) {
//            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
//
//        Treatment treatment = treatmentRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Treatment not found with ID: " + id));
//        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
//        return modelMapper.map(treatment, TreatmentViewDTO.class);
//    }
//
//    @Override
//    @Async
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
//    public CompletableFuture<Page<TreatmentViewDTO>> getAll(int page, int size, String orderBy, boolean ascending) {
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}",
//                ENTITY_NAME, page, size, orderBy, ascending);
//
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<Treatment> treatments = treatmentRepository.findAllActive(pageable);
//        Page<TreatmentViewDTO> result = treatments.map(t -> modelMapper.map(t, TreatmentViewDTO.class));
//        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    private List<Medicine> validateAndGetMedicines(List<Long> medicineIds) {
//        if (medicineIds == null || medicineIds.isEmpty()) {
//            return List.of();
//        }
//        List<Medicine> medicines = medicineRepository.findAllById(medicineIds);
//        if (medicines.size() != medicineIds.size()) {
//            logger.error("Invalid medicines provided: some IDs do not exist");
//            throw new InvalidDTOException("One or more medicine IDs are invalid");
//        }
//        return medicines;
//    }
//
//    private void validateDoctorOwnership(Long doctorId) {
//        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
//                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
//            String currentUserId = userService.getCurrentUserId((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
//            UserViewDTO user = userService.getByKeycloakId(currentUserId);
//            DoctorViewDTO doctor = doctorService.getById(doctorId);
//            if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("DOCTOR")) || !doctor.getUserId().equals(user.getId())) {
//                logger.error("Doctor ID: {} does not match authenticated user: {}", doctorId, currentUserId);
//                throw new InvalidInputException("Cannot create/update treatment for another doctor");
//            }
//        }
//    }
//}