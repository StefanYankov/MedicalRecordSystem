//package nbu.cscb869.services.services;
//
//import com.cloudinary.Cloudinary;
//import com.cloudinary.utils.ObjectUtils;
//import nbu.cscb869.common.exceptions.EntityNotFoundException;
//import nbu.cscb869.common.exceptions.InvalidDTOException;
//import nbu.cscb869.common.exceptions.InvalidDoctorException;
//import nbu.cscb869.common.exceptions.InvalidInputException;
//import nbu.cscb869.data.dto.DoctorPatientCountDTO;
//import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
//import nbu.cscb869.data.dto.DoctorVisitCountDTO;
//import nbu.cscb869.data.models.Doctor;
//import nbu.cscb869.data.models.Patient;
//import nbu.cscb869.data.models.Specialty;
//import nbu.cscb869.data.models.Visit;
//import nbu.cscb869.data.repositories.DoctorRepository;
//import nbu.cscb869.data.repositories.SpecialtyRepository;
//import nbu.cscb869.data.repositories.VisitRepository;
//import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
//import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
//import nbu.cscb869.services.data.dtos.DoctorViewDTO;
//import nbu.cscb869.services.data.dtos.PatientViewDTO;
//import nbu.cscb869.services.data.dtos.VisitViewDTO;
//import nbu.cscb869.services.services.contracts.DoctorService;
//import org.modelmapper.ModelMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Collectors;
//
///**
// * Implementation of {@link DoctorService} for managing doctor operations.
// */
//@Service
//public class DoctorServiceImpl implements DoctorService {
//    private static final Logger logger = LoggerFactory.getLogger(DoctorServiceImpl.class);
//    private static final String ENTITY_NAME = "Doctor";
//    private static final int MAX_PAGE_SIZE = 100;
//
//    private final DoctorRepository doctorRepository;
//    private final SpecialtyRepository specialtyRepository;
//    private final VisitRepository visitRepository;
//    private final ModelMapper modelMapper;
//    private final Cloudinary cloudinary;
//
//    /**
//     * Constructs a new DoctorServiceImpl with the specified dependencies.
//     * @param doctorRepository the repository for doctor entities
//     * @param specialtyRepository the repository for specialty entities
//     * @param visitRepository the repository for visit entities
//     * @param modelMapper the ModelMapper for DTO conversions
//     * @param cloudName the Cloudinary cloud name
//     * @param apiKey the Cloudinary API key
//     * @param apiSecret the Cloudinary API secret
//     */
//    public DoctorServiceImpl(DoctorRepository doctorRepository, SpecialtyRepository specialtyRepository,
//                             VisitRepository visitRepository, ModelMapper modelMapper,
//                             @Value("${cloudinary.cloud-name}") String cloudName,
//                             @Value("${cloudinary.api-key}") String apiKey,
//                             @Value("${cloudinary.api-secret}") String apiSecret) {
//        this.doctorRepository = doctorRepository;
//        this.specialtyRepository = specialtyRepository;
//        this.visitRepository = visitRepository;
//        this.modelMapper = modelMapper;
//        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
//                "cloud_name", cloudName,
//                "api_key", apiKey,
//                "api_secret", apiSecret));
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    public DoctorViewDTO create(DoctorCreateDTO dto, MultipartFile image) {
//        if (dto == null) {
//            logger.error("Cannot create {}: DTO is null", ENTITY_NAME);
//            throw new InvalidDTOException("DoctorCreateDTO cannot be null");
//        }
//        logger.debug("Creating {} with unique ID: {}", ENTITY_NAME, dto.getUniqueIdNumber());
//
//        if (doctorRepository.findByUniqueIdNumber(dto.getUniqueIdNumber()).isPresent()) {
//            throw new InvalidDoctorException("Doctor with unique ID " + dto.getUniqueIdNumber() + " already exists");
//        }
//
//        Doctor doctor = modelMapper.map(dto, Doctor.class);
//        if (dto.getSpecialtyIds() != null && !dto.getSpecialtyIds().isEmpty()) {
//            Set<Specialty> specialties = dto.getSpecialtyIds().stream()
//                    .map(id -> specialtyRepository.findById(id)
//                            .orElseThrow(() -> new EntityNotFoundException("Specialty not found with ID: " + id)))
//                    .collect(Collectors.toSet());
//            doctor.setSpecialties(specialties);
//        }
//
//        if (image != null && !image.isEmpty()) {
//            try {
//                var uploadResult = cloudinary.uploader().upload(image.getBytes(), ObjectUtils.asMap(
//                        "folder", "doctors",
//                        "public_id", "doctor_" + dto.getUniqueIdNumber()));
//                doctor.setImageUrl((String) uploadResult.get("secure_url"));
//            } catch (IOException e) {
//                logger.error("Failed to upload image for {}: {}", ENTITY_NAME, e.getMessage());
//                throw new InvalidInputException("Failed to upload image: " + e.getMessage());
//            }
//        }
//
//        doctorRepository.save(doctor);
//        logger.info("Created {} with ID: {}", ENTITY_NAME, doctor.getId());
//        return modelMapper.map(doctor, DoctorViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Transactional
//    public DoctorViewDTO update(DoctorUpdateDTO dto, MultipartFile image) {
//        if (dto == null || dto.getId() == null) {
//            logger.error("Cannot update {}: DTO or ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("DoctorUpdateDTO or ID cannot be null");
//        }
//        logger.debug("Updating {} with ID: {}", ENTITY_NAME, dto.getId());
//
//        Doctor doctor = doctorRepository.findById(dto.getId())
//                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + dto.getId()));
//
//        if (!doctor.getUniqueIdNumber().equals(dto.getUniqueIdNumber()) &&
//                doctorRepository.findByUniqueIdNumber(dto.getUniqueIdNumber()).isPresent()) {
//            throw new InvalidDoctorException("Unique ID " + dto.getUniqueIdNumber() + " is already in use");
//        }
//
//        modelMapper.map(dto, doctor);
//        if (dto.getSpecialtyIds() != null) {
//            Set<Specialty> specialties = dto.getSpecialtyIds().stream()
//                    .map(id -> specialtyRepository.findById(id)
//                            .orElseThrow(() -> new EntityNotFoundException("Specialty not found with ID: " + id)))
//                    .collect(Collectors.toSet());
//            doctor.setSpecialties(specialties);
//        } else {
//            doctor.setSpecialties(Set.of());
//        }
//
//        if (image != null && !image.isEmpty()) {
//            try {
//                var uploadResult = cloudinary.uploader().upload(image.getBytes(), ObjectUtils.asMap(
//                        "folder", "doctors",
//                        "public_id", "doctor_" + dto.getUniqueIdNumber()));
//                doctor.setImageUrl((String) uploadResult.get("secure_url"));
//            } catch (IOException e) {
//                logger.error("Failed to upload image for {}: {}", ENTITY_NAME, e.getMessage());
//                throw new InvalidInputException("Failed to upload image: " + e.getMessage());
//            }
//        }
//
//        doctorRepository.save(doctor);
//        logger.info("Updated {} with ID: {}", ENTITY_NAME, doctor.getId());
//        return modelMapper.map(doctor, DoctorViewDTO.class);
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
//        Doctor doctor = doctorRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + id));
//
//        if (doctor.isGeneralPractitioner()) {
//            long patientCount = doctorRepository.findPatientCountByGeneralPractitioner().stream()
//                    .filter(dto -> dto.getDoctor().getId().equals(id))
//                    .mapToLong(DoctorPatientCountDTO::getPatientCount)
//                    .findFirst()
//                    .orElse(0L);
//            if (patientCount > 0) {
//                throw new InvalidDoctorException("Cannot delete General Practitioner with active patients");
//            }
//        }
//
//        doctor.setIsDeleted(true);
//        doctor.setDeletedOn(LocalDateTime.now());
//        doctorRepository.save(doctor);
//        logger.info("Soft deleted {} with ID: {}", ENTITY_NAME, id);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public DoctorViewDTO getById(Long id) {
//        if (id == null) {
//            logger.error("Cannot retrieve {}: ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("ID cannot be null");
//        }
//        logger.debug("Retrieving {} with ID: {}", ENTITY_NAME, id);
//
//        Doctor doctor = doctorRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + id));
//        logger.info("Retrieved {} with ID: {}", ENTITY_NAME, id);
//        return modelMapper.map(doctor, DoctorViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public DoctorViewDTO getByUniqueIdNumber(String uniqueIdNumber) {
//        if (uniqueIdNumber == null || uniqueIdNumber.trim().isEmpty()) {
//            logger.error("Cannot retrieve {}: Unique ID is null or empty", ENTITY_NAME);
//            throw new InvalidDTOException("Unique ID cannot be null or empty");
//        }
//        logger.debug("Retrieving {} with unique ID: {}", ENTITY_NAME, uniqueIdNumber);
//
//        Doctor doctor = doctorRepository.findByUniqueIdNumber(uniqueIdNumber)
//                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with unique ID: " + uniqueIdNumber));
//        logger.info("Retrieved {} with unique ID: {}", ENTITY_NAME, uniqueIdNumber);
//        return modelMapper.map(doctor, DoctorViewDTO.class);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Async
//    public CompletableFuture<Page<DoctorViewDTO>> getAll(int page, int size, String orderBy, boolean ascending, String filter) {
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving all {}: page={}, size={}, orderBy={}, ascending={}, filter={}",
//                ENTITY_NAME, page, size, orderBy, ascending, filter);
//
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<Doctor> doctors = (filter == null || filter.trim().isEmpty())
//                ? doctorRepository.findAllActive(pageable)
//                : doctorRepository.findByUniqueIdNumberContaining("%" + filter.trim().toLowerCase() + "%", pageable);
//        Page<DoctorViewDTO> result = doctors.map(d -> modelMapper.map(d, DoctorViewDTO.class));
//        logger.info("Retrieved {} {} for page {}, size {}", result.getTotalElements(), ENTITY_NAME, page, size);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public Page<DoctorViewDTO> findByCriteria(Specification<Doctor> spec, int page, int size, String orderBy, boolean ascending) {
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidInputException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving {} by criteria: page={}, size={}, orderBy={}, ascending={}",
//                ENTITY_NAME, page, size, orderBy, ascending);
//
//        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, orderBy);
//        Pageable pageable = PageRequest.of(page, size, sort);
//        Page<Doctor> doctors = doctorRepository.findAll(spec, pageable);
//        Page<DoctorViewDTO> result = doctors.map(d -> modelMapper.map(d, DoctorViewDTO.class));
//        logger.info("Retrieved {} {} by criteria", result.getTotalElements(), ENTITY_NAME);
//        return result;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public Page<PatientViewDTO> getByGeneralPractitioner(Long generalPractitionerId, int page, int size) {
//        if (generalPractitionerId == null) {
//            logger.error("Cannot retrieve {} patients: General Practitioner ID is null", ENTITY_NAME);
//            throw new InvalidDTOException("General Practitioner ID cannot be null");
//        }
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        logger.debug("Retrieving patients for General Practitioner ID: {}, page={}, size={}",
//                generalPractitionerId, page, size);
//
//        Doctor gp = doctorRepository.findById(generalPractitionerId)
//                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + generalPractitionerId));
//        if (!gp.isGeneralPractitioner()) {
//            throw new InvalidDoctorException("Doctor with ID " + generalPractitionerId + " is not a general practitioner");
//        }
//
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Patient> patients = doctorRepository.findPatientsByGeneralPractitioner(gp, pageable);
//        Page<PatientViewDTO> result = patients.map(p -> modelMapper.map(p, PatientViewDTO.class));
//        logger.info("Retrieved {} patients for General Practitioner ID: {}", result.getTotalElements(), generalPractitionerId);
//        return result;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public List<DoctorPatientCountDTO> getPatientCountByGeneralPractitioner() {
//        logger.debug("Retrieving patient count by General Practitioner");
//        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();
//        logger.info("Retrieved {} General Practitioners with patient counts", result.size());
//        return result;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public List<DoctorVisitCountDTO> getVisitCount() {
//        logger.debug("Retrieving visit count by Doctor");
//        List<DoctorVisitCountDTO> result = doctorRepository.findVisitCountByDoctor();
//        logger.info("Retrieved {} Doctors with visit counts", result.size());
//        return result;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    @Async
//    public CompletableFuture<Page<VisitViewDTO>> getVisitsByPeriod(Long doctorId, LocalDate startDate, LocalDate endDate,
//                                                                   int page, int size) {
//        if (doctorId == null || startDate == null || endDate == null) {
//            logger.error("Cannot retrieve visits: Doctor ID or dates are null");
//            throw new InvalidDTOException("Doctor ID and dates cannot be null");
//        }
//        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
//            logger.error("Invalid pagination: page={}, size={}", page, size);
//            throw new InvalidDTOException("Invalid pagination parameters");
//        }
//        if (startDate.isAfter(endDate)) {
//            logger.error("Invalid date range: startDate={} is after endDate={}", startDate, endDate);
//            throw new InvalidInputException("Start date must be before or equal to end date");
//        }
//        logger.debug("Retrieving visits for Doctor ID: {}, from {} to {}, page={}, size={}",
//                doctorId, startDate, endDate, page, size);
//
//        Doctor doctor = doctorRepository.findById(doctorId)
//                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with ID: " + doctorId));
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Visit> visits = visitRepository.findByDoctorAndDateRange(doctor, startDate, endDate, pageable);
//        Page<VisitViewDTO> result = visits.map(v -> modelMapper.map(v, VisitViewDTO.class));
//        logger.info("Retrieved {} visits for Doctor ID: {}", result.getTotalElements(), doctorId);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public List<DoctorSickLeaveCountDTO> getDoctorsWithMostSickLeaves() {
//        logger.debug("Retrieving doctors with most sick leaves");
//        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();
//        logger.info("Retrieved {} doctors with sick leave counts", result.size());
//        return result;
//    }
//}