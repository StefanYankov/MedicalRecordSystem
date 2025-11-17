package nbu.cscb869.services.services;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidDoctorException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.specifications.DoctorSpecification;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.utility.CloudinaryService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of {@link DoctorService} for managing doctor operations.
 */
@Service
public class DoctorServiceImpl implements DoctorService {
    private static final Logger logger = LoggerFactory.getLogger(DoctorServiceImpl.class);
    private static final String ENTITY_NAME = "Doctor";
    private static final int MAX_PAGE_SIZE = 100;

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final VisitRepository visitRepository;
    private final ModelMapper modelMapper;
    private final CloudinaryService cloudinaryService;

    public DoctorServiceImpl(DoctorRepository doctorRepository, SpecialtyRepository specialtyRepository,
                             VisitRepository visitRepository, ModelMapper modelMapper,
                             CloudinaryService cloudinaryService) {
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
        this.visitRepository = visitRepository;
        this.modelMapper = modelMapper;
        this.cloudinaryService = cloudinaryService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DoctorViewDTO createDoctor(DoctorCreateDTO dto) {
        return create(dto, null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DoctorViewDTO updateDoctor(DoctorUpdateDTO dto) {
        return update(dto, null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DoctorViewDTO create(DoctorCreateDTO dto, MultipartFile image) {
        validateDtoNotNull(dto, "create");
        logger.debug("Creating {} with unique ID: {}", ENTITY_NAME, dto.getUniqueIdNumber());

        if (doctorRepository.findByUniqueIdNumber(dto.getUniqueIdNumber()).isPresent()) {
            throw new InvalidDoctorException(ExceptionMessages.formatDoctorUniqueIdExists(dto.getUniqueIdNumber()));
        }

        Doctor doctor = modelMapper.map(dto, Doctor.class);
        doctor.setKeycloakId(dto.getKeycloakId());
        doctor.setApproved(false); // Explicitly set to false for all new applications
        setSpecialtiesByNames(dto.getSpecialties(), doctor);
        handleImageUpload(doctor, image, dto.getUniqueIdNumber());

        doctorRepository.save(doctor);
        logger.info("Created {} with ID: {}. Awaiting admin approval.", ENTITY_NAME, doctor.getId());
        return modelMapper.map(doctor, DoctorViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DoctorViewDTO update(DoctorUpdateDTO dto, MultipartFile image) {
        if (dto == null) {
            logger.error("Cannot update {}: DTO is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull(ENTITY_NAME));
        }
        if (dto.getId() == null) {
            logger.error("Cannot update {}: ID is null", ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldNull("ID"));
        }
        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());

        Doctor doctor = doctorRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(dto.getId())));

        if (dto.isDeleteImage() && doctor.getImageUrl() != null) {
            cloudinaryService.deleteImage(cloudinaryService.getPublicIdFromUrl(doctor.getImageUrl()));
            doctor.setImageUrl(null);
        }

        modelMapper.map(dto, doctor);

        handleImageUpload(doctor, image, doctor.getUniqueIdNumber());

        doctorRepository.save(doctor);
        logger.info("Updated {} with ID: {}", ENTITY_NAME, doctor.getId());
        return modelMapper.map(doctor, DoctorViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        validateIdNotNull(id, "delete");
        logger.debug("Deleting {} with ID: {}", ENTITY_NAME, id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(id)));

        if (doctor.isGeneralPractitioner()) {
            long patientCount = doctorRepository.findPatientCountByGeneralPractitioner().stream()
                    .filter(d -> d.getDoctor().getId().equals(id))
                    .mapToLong(DoctorPatientCountDTO::getPatientCount)
                    .findFirst().orElse(0L);
            if (patientCount > 0) {
                throw new InvalidDoctorException(ExceptionMessages.DOCTOR_HAS_ACTIVE_PATIENTS);
            }
        }

        doctorRepository.delete(doctor);
        logger.info("Deleted {} with ID: {}", ENTITY_NAME, id);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public DoctorViewDTO getById(Long id) {
        validateIdNotNull(id, "getById");
        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);

        Doctor doctor = doctorRepository.findByIdWithSpecialties(id)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(id)));

        logger.info("Retrieved {} with ID: {}. Specialties: {}", ENTITY_NAME, id, doctor.getSpecialties().stream().map(Specialty::getName).collect(Collectors.toSet()));
        return modelMapper.map(doctor, DoctorViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    public DoctorViewDTO getByUniqueIdNumber(String uniqueIdNumber) {
        validateUniqueIdNotNullOrEmpty(uniqueIdNumber, "getByUniqueIdNumber");
        logger.debug("Retrieving {} with unique ID: {}", ENTITY_NAME, uniqueIdNumber);

        Doctor doctor = doctorRepository.findByUniqueIdNumber(uniqueIdNumber)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundByUniqueId(uniqueIdNumber)));
        logger.info("Retrieved {} with unique ID: {}", ENTITY_NAME, uniqueIdNumber);
        return modelMapper.map(doctor, DoctorViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Async
    public CompletableFuture<Page<DoctorViewDTO>> getAllAsync(int page, int size, String orderBy, boolean ascending, String filter) {
        validatePagination(page, size, "getAllAsync");
        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}, filter={}",
                ENTITY_NAME, page, size, orderBy, ascending, filter);

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Doctor> doctors = (filter == null || filter.trim().isEmpty())
                ? doctorRepository.findAll(pageable)
                : doctorRepository.findByUniqueIdNumberContaining("%" + filter.trim().toLowerCase() + "%", pageable);
        Page<DoctorViewDTO> result = doctors.map(d -> modelMapper.map(d, DoctorViewDTO.class));
        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
        return CompletableFuture.completedFuture(result);
    }

    /** {@inheritDoc} */
    @Override
    public Page<DoctorViewDTO> findByCriteria(Specification<Doctor> spec, int page, int size, String orderBy, boolean ascending) {
        validatePagination(page, size, "findByCriteria");
        logger.debug("Retrieving {} by criteria: page={}, size={}, orderBy={}, ascending={}",
                ENTITY_NAME, page, size, orderBy, ascending);

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Doctor> doctors = doctorRepository.findAll(spec, pageable);
        Page<DoctorViewDTO> result = doctors.map(d -> modelMapper.map(d, DoctorViewDTO.class));
        logger.info("Retrieved {} {} by criteria", result.getTotalElements(), ENTITY_NAME);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Page<DoctorViewDTO> findAllBySpecialty(Long specialtyId, int page, int size, String sortBy, boolean asc) {
        validateIdNotNull(specialtyId, "findAllBySpecialty");
        logger.debug("Finding all doctors by specialty ID: {}", specialtyId);

        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatSpecialtyNotFoundById(specialtyId)));

        Specification<Doctor> spec = DoctorSpecification.hasSpecialty(specialty);

        return findByCriteria(spec, page, size, sortBy, asc);
    }

    /** {@inheritDoc} */
    @Override
    public Page<PatientViewDTO> getPatientsByGeneralPractitioner(Long generalPractitionerId, int page, int size) {
        validateIdNotNull(generalPractitionerId, "getPatientsByGeneralPractitioner");
        validatePagination(page, size, "getPatientsByGeneralPractitioner");
        logger.debug("Retrieving patients for General Practitioner ID: {}, page={}, size={}",
                generalPractitionerId, page, size);

        Doctor gp = doctorRepository.findById(generalPractitionerId)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(generalPractitionerId)));
        if (!gp.isGeneralPractitioner()) {
            throw new InvalidDoctorException(ExceptionMessages.formatInvalidGeneralPractitioner(generalPractitionerId));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<PatientViewDTO> result = doctorRepository.findPatientsByGeneralPractitioner(gp, pageable)
                .map(p -> modelMapper.map(p, PatientViewDTO.class));
        logger.info("Retrieved {} patients for General Practitioner ID: {}", result.getTotalElements(), generalPractitionerId);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner() {
        logger.debug("Retrieving patient count by General Practitioner for internal use.");
        return doctorRepository.findPatientCountByGeneralPractitioner();
    }

    /** {@inheritDoc} */
    @Override
    public List<DoctorVisitCountDTO> getVisitCount() {
        logger.debug("Retrieving visit count by Doctor");
        return doctorRepository.findVisitCountByDoctor();
    }

    @Override
    public Page<VisitViewDTO> getVisitsByPeriod(Long doctorId, LocalDate startDate, LocalDate endDate, int page, int size) {
        validateParamsNotNull(doctorId, startDate, endDate, "getVisitsByPeriod");
        validatePagination(page, size, "getVisitsByPeriod");
        validateDateRange(startDate, endDate);
        logger.debug("Retrieving visits for Doctor ID: {}, from {} to {}, page={}, size={}",
                doctorId, startDate, endDate, page, size);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(doctorId)));
        Pageable pageable = PageRequest.of(page, size);
        Page<VisitViewDTO> result = visitRepository.findByDoctorAndDateRange(doctor, startDate, endDate, pageable)
                .map(v -> modelMapper.map(v, VisitViewDTO.class));
        logger.info("Retrieved {} visits for Doctor ID: {}", result.getTotalElements(), doctorId);
        return result;
    }

    @Override
    public List<DoctorSickLeaveCountDTO> getDoctorsWithMostSickLeaves() {
        logger.debug("Retrieving doctors with most sick leaves");
        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();
        logger.info("Retrieved {} doctors with sick leave counts", result.size());
        return result;
    }

    @Override
    public DoctorViewDTO getByKeycloakId(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new InvalidInputException("Keycloak ID cannot be null or blank.");
        }
        logger.debug("Retrieving {} by Keycloak ID: {}", ENTITY_NAME, keycloakId);
        Doctor doctor = doctorRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundByKeycloakId(keycloakId)));
        logger.info("Retrieved {} with Keycloak ID: {}", ENTITY_NAME, keycloakId);
        return modelMapper.map(doctor, DoctorViewDTO.class);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteDoctorImage(Long doctorId) {
        validateIdNotNull(doctorId, "deleteDoctorImage");
        logger.debug("Deleting image for doctor ID: {}", doctorId);
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(doctorId)));

        if (doctor.getImageUrl() != null) {
            cloudinaryService.deleteImage(cloudinaryService.getPublicIdFromUrl(doctor.getImageUrl()));
            doctor.setImageUrl(null);
            doctorRepository.save(doctor);
            logger.info("Successfully deleted image for doctor ID: {}", doctorId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Page<DoctorViewDTO> getUnapprovedDoctors(int page, int size) {
        validatePagination(page, size, "getUnapprovedDoctors");
        logger.debug("Retrieving unapproved doctors: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Doctor> unapprovedDoctors = doctorRepository.findByIsApproved(false, pageable);

        Page<DoctorViewDTO> result = unapprovedDoctors.map(doctor -> modelMapper.map(doctor, DoctorViewDTO.class));
        logger.info("Retrieved {} unapproved doctors.", result.getTotalElements());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void approveDoctor(Long doctorId) {
        validateIdNotNull(doctorId, "approveDoctor");
        logger.debug("Attempting to approve doctor with ID: {}", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatDoctorNotFoundById(doctorId)));

        if (doctor.isApproved()) {
            logger.warn("Doctor with ID {} is already approved.", doctorId);
            throw new InvalidDoctorException(ExceptionMessages.formatDoctorAlreadyApproved(doctorId));
        }

        doctor.setApproved(true);
        doctorRepository.save(doctor);

        logger.info("Successfully approved doctor with ID: {}.", doctorId);
    }

    /** {@inheritDoc} */
    @Override
    public List<DoctorPatientCountReportDTO> getPatientCountReport() {
        logger.debug("Retrieving patient count report for API.");
        List<DoctorPatientCountDTO> resultsFromRepo = doctorRepository.findPatientCountByGeneralPractitioner();
        return resultsFromRepo.stream()
                .map(result -> {
                    DoctorViewDTO doctorViewDTO = modelMapper.map(result.getDoctor(), DoctorViewDTO.class);
                    return new DoctorPatientCountReportDTO(doctorViewDTO, result.getPatientCount());
                })
                .collect(Collectors.toList());
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

    private void validateUniqueIdNotNullOrEmpty(String uniqueId, String operation) {
        if (uniqueId == null || uniqueId.trim().isEmpty()) {
            logger.error("Cannot {} {}: Unique ID is null or empty", operation, ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidFieldEmpty("Unique ID"));
        }
    }

    private void validatePagination(int page, int size, String operation) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            logger.error("Cannot {} {}: Invalid pagination, page={}, size={}", operation, ENTITY_NAME, page, size);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("Pagination parameters"));
        }
    }

    private void validateParamsNotNull(Object param1, Object param2, Object param3, String operation) {
        if (param1 == null || param2 == null || param3 == null) {
            logger.error("Cannot {} {}: Parameters are null", operation, ENTITY_NAME);
            throw new InvalidDTOException(ExceptionMessages.formatInvalidDTONull("Parameters"));
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            logger.error("Cannot process {} visits: Invalid date range, startDate={} is after endDate={}", ENTITY_NAME, startDate, endDate);
            throw new InvalidInputException(ExceptionMessages.formatInvalidDateRange(startDate, endDate));
        }
    }

    private void setSpecialtiesByNames(Set<String> specialtyNames, Doctor doctor) {
        if (specialtyNames == null || specialtyNames.isEmpty()) {
            if (doctor.getSpecialties() != null) {
                doctor.getSpecialties().clear();
            }
        } else {
            Set<Specialty> specialties = specialtyNames.stream()
                    .map(name -> specialtyRepository.findByName(name)
                            .orElseThrow(() -> new EntityNotFoundException(ExceptionMessages.formatSpecialtyNotFoundByName(name))))
                    .collect(Collectors.toSet());
            doctor.setSpecialties(new HashSet<>(specialties));
        }
    }

    private void handleImageUpload(Doctor doctor, MultipartFile image, String uniqueIdNumber) {
        if (image != null && !image.isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadImage(image).get(); // .get() blocks and waits for the async result
                doctor.setImageUrl(imageUrl);
                logger.info("Uploaded image for {} with unique ID: {}", ENTITY_NAME, uniqueIdNumber);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Failed to upload image for {} with unique ID {}: {}", ENTITY_NAME, uniqueIdNumber, e.getMessage());
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new InvalidInputException(ExceptionMessages.formatFailedToCreateDoctor(e.getMessage()));
            }
        }
    }
}
