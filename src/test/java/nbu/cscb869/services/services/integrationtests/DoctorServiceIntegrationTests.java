package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.common.exceptions.*;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.services.contracts.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DoctorServiceIntegrationTests {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private EntityManager entityManager;

    private Doctor doctor;
    private Specialty specialty;
    private Patient patient;
    private Visit visit;
    private Diagnosis diagnosis;

    @BeforeEach
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void setUp() {
        // Hard delete to clear all data
        visitRepository.deleteAllInBatch();
        patientRepository.deleteAllInBatch();
        doctorRepository.deleteAllInBatch();
        specialtyRepository.deleteAllInBatch();
        diagnosisRepository.deleteAllInBatch();

        // Setup Specialty
        specialty = new Specialty();
        specialty.setName("Cardiology");
        specialty.setDescription("Heart-related specialties");
        specialty = specialtyRepository.save(specialty);
        entityManager.flush();
        entityManager.clear();
        assertNotNull(specialty.getId(), "Specialty ID should not be null");

        // Setup Diagnosis
        diagnosis = new Diagnosis();
        diagnosis.setName("Common Cold");
        diagnosis.setDescription("A mild viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        entityManager.clear();
        assertNotNull(diagnosis.getId(), "Diagnosis ID should not be null");

        // Setup Doctor
        doctor = new Doctor();
        doctor.setName("Dr. Smith");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);
        doctor.setSpecialties(Set.of(specialty));
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        entityManager.clear();

        // Verify Doctor persistence
        Doctor persistedDoctor = doctorRepository.findByIdIncludingDeleted(doctor.getId())
                .orElseThrow(() -> new AssertionError("Doctor should exist after save"));
        assertFalse(persistedDoctor.getIsDeleted(), "Doctor should not be deleted");
        System.out.println("Saved Doctor ID: " + doctor.getId() + ", isDeleted: " + doctor.getIsDeleted());
    }

    // Happy Path
    @Test
    void Create_ValidDTO_ReturnsDoctorViewDTO() {
        DoctorCreateDTO createDTO = DoctorCreateDTO.builder()
                .name("Dr. Jones")
                .uniqueIdNumber(TestDataUtils.generateUniqueIdNumber())
                .isGeneralPractitioner(false)
                .specialtyIds(Set.of(specialty.getId()))
                .build();

        DoctorViewDTO result = doctorService.create(createDTO);

        assertNotNull(result);
        assertEquals("Dr. Jones", result.getName());
        assertEquals(createDTO.getUniqueIdNumber(), result.getUniqueIdNumber());
        assertFalse(result.isGeneralPractitioner());
        assertEquals(1, result.getSpecialties().size());
        assertEquals("Cardiology", result.getSpecialties().iterator().next().getName());
    }

    @Test
    void Update_ValidDTO_ReturnsUpdatedDoctorViewDTO() {
        DoctorUpdateDTO updateDTO = DoctorUpdateDTO.builder()
                .id(doctor.getId())
                .name("Dr. Smith Updated")
                .isGeneralPractitioner(false)
                .specialtyIds(Set.of(specialty.getId()))
                .build();

        DoctorViewDTO result = doctorService.update(updateDTO);

        assertNotNull(result);
        assertEquals("Dr. Smith Updated", result.getName());
        assertFalse(result.isGeneralPractitioner());
        assertEquals(1, result.getSpecialties().size());
    }

    @Test
    void Delete_ValidId_DeletesDoctor() {
        DoctorCreateDTO createDTO = DoctorCreateDTO.builder()
                .name("Dr. Jones")
                .uniqueIdNumber(TestDataUtils.generateUniqueIdNumber())
                .isGeneralPractitioner(false)
                .build();
        DoctorViewDTO created = doctorService.create(createDTO);

        doctorService.delete(created.getId());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> doctorService.getById(created.getId()));
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, created.getId()),
                exception.getMessage());
    }

    @Test
    void GetById_ValidId_ReturnsDoctorViewDTO() {
        DoctorViewDTO result = doctorService.getById(doctor.getId());

        assertNotNull(result);
        assertEquals("Dr. Smith", result.getName());
        assertEquals(doctor.getUniqueIdNumber(), result.getUniqueIdNumber());
    }

    @Test
    void GetByUniqueIdNumber_ValidUniqueId_ReturnsDoctorViewDTO() {
        DoctorViewDTO result = doctorService.getByUniqueIdNumber(doctor.getUniqueIdNumber());

        assertNotNull(result);
        assertEquals("Dr. Smith", result.getName());
        assertEquals(doctor.getUniqueIdNumber(), result.getUniqueIdNumber());
    }

    @Test
    void GetAll_ValidParameters_ReturnsPaginatedDoctors() throws InterruptedException {
        Page<DoctorViewDTO> result = doctorService.getAll(0, 10, "name", true, null).join();
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Dr. Smith", result.getContent().get(0).getName());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void GetAll_WithFilter_ReturnsFilteredDoctors() {
        Page<DoctorViewDTO> result = doctorService.getAll(0, 10, "name", true, "Dr. Smith").join();

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Dr. Smith", result.getContent().get(0).getName());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void FindByCriteria_ValidConditions_ReturnsFilteredDoctors() {
        Map<String, Object> conditions = Map.of("name", "Smith", "isGeneralPractitioner", "true", "specialtyId", specialty.getId());

        Page<DoctorViewDTO> result = doctorService.findByCriteria(conditions, 0, 10, "name", true);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Dr. Smith", result.getContent().get(0).getName());
    }

    @Test
    void GetPatientsByGeneralPractitioner_ValidId_ReturnsPatients() {
        patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(doctor);
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        Page<PatientViewDTO> result = doctorService.getPatientsByGeneralPractitioner(doctor.getId());

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("John Doe", result.getContent().get(0).getName());
    }

    @Test
    void GetPatientCountByGeneralPractitioner_ValidId_ReturnsCount() {
        patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(doctor);
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        DoctorPatientCountDTO result = doctorService.getPatientCountByGeneralPractitioner(doctor.getId());

        assertNotNull(result);
        assertEquals(1L, result.getPatientCount());
        assertEquals("Dr. Smith", result.getDoctor().getName());
    }

    @Test
    void GetVisitCount_ValidId_ReturnsCount() {
        patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(doctor);
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        visit = new Visit();
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setVisitDate(LocalDate.now());
        visit.setSickLeaveIssued(false);
        visit.setDiagnosis(diagnosis);
        visit = visitRepository.save(visit);
        entityManager.flush();
        entityManager.clear();

        DoctorVisitCountDTO result = doctorService.getVisitCount(doctor.getId());

        assertNotNull(result);
        assertEquals(1L, result.getVisitCount());
        assertEquals("Dr. Smith", result.getDoctor().getName());
    }

    @Test
    void GetVisitsByPeriod_ValidParameters_ReturnsVisits() {
        patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(doctor);
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        visit = new Visit();
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setVisitDate(LocalDate.now());
        visit.setSickLeaveIssued(false);
        visit.setDiagnosis(diagnosis);
        visit = visitRepository.save(visit);
        entityManager.flush();
        entityManager.clear();

        System.out.println("Doctor ID for getVisitsByPeriod: " + doctor.getId());
        Page<VisitViewDTO> result = doctorService.getVisitsByPeriod(doctor.getId(), LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)).join();

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(visit.getVisitDate(), result.getContent().get(0).getVisitDate());
    }

    @Test
    void GetDoctorsWithMostSickLeaves_ValidCall_ReturnsSickLeaveCounts() {
        List<DoctorSickLeaveCountDTO> result = doctorService.getDoctorsWithMostSickLeaves();

        assertNotNull(result);
        assertTrue(result.isEmpty()); // No sick leaves in setup
    }

    // Error Cases
    @Test
    void Create_NullDTO_ThrowsInvalidDTOException() {
        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.create(null));
        assertEquals(ExceptionMessages.formatInvalidDTONull("DoctorCreateDTO"), exception.getMessage());
    }

    @Test
    void Create_DuplicateUniqueId_ThrowsInvalidDoctorException() {
        DoctorCreateDTO createDTO = DoctorCreateDTO.builder()
                .name("Dr. Jones")
                .uniqueIdNumber(doctor.getUniqueIdNumber())
                .isGeneralPractitioner(false)
                .build();

        InvalidDoctorException exception = assertThrows(InvalidDoctorException.class, () -> doctorService.create(createDTO));
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_UNIQUE_ID_EXISTS, doctor.getUniqueIdNumber()), exception.getMessage());
    }

    @Test
    void Create_InvalidSpecialtyIds_ThrowsInvalidDoctorException() {
        DoctorCreateDTO createDTO = DoctorCreateDTO.builder()
                .name("Dr. Jones")
                .uniqueIdNumber(TestDataUtils.generateUniqueIdNumber())
                .isGeneralPractitioner(false)
                .specialtyIds(Set.of(999L))
                .build();

        InvalidDoctorException exception = assertThrows(InvalidDoctorException.class, () -> doctorService.create(createDTO));
        assertEquals("One or more specialty IDs are invalid", exception.getMessage());
    }

    @Test
    void Update_NullDTO_ThrowsInvalidDTOException() {
        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.update(null));
        assertEquals(ExceptionMessages.formatInvalidDTONull("DoctorUpdateDTO"), exception.getMessage());
    }

    @Test
    void Update_NullId_ThrowsInvalidDTOException() {
        DoctorUpdateDTO updateDTO = DoctorUpdateDTO.builder().build();
        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.update(updateDTO));
        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), exception.getMessage());
    }

    @Test
    void Update_NonExistentDoctor_ThrowsEntityNotFoundException() {
        DoctorUpdateDTO updateDTO = DoctorUpdateDTO.builder().id(999L).name("Dr. Jones").build();

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.update(updateDTO));
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 999L), exception.getMessage());
    }

    @Test
    void Delete_NullId_ThrowsInvalidDTOException() {
        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.delete(null));
        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), exception.getMessage());
    }

    @Test
    void Delete_NonExistentDoctor_ThrowsEntityNotFoundException() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.delete(999L));
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 999L), exception.getMessage());
    }

    @Test
    void Delete_GpWithActivePatients_ThrowsInvalidDoctorException() {
        patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(doctor);
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        InvalidDoctorException exception = assertThrows(InvalidDoctorException.class, () -> doctorService.delete(doctor.getId()));
        assertEquals(ExceptionMessages.DOCTOR_HAS_ACTIVE_PATIENTS, exception.getMessage());
    }

    @Test
    void GetById_NullId_ThrowsInvalidDTOException() {
        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.getById(null));
        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), exception.getMessage());
    }

    @Test
    void GetById_NonExistentId_ThrowsEntityNotFoundException() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getById(999L));
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 999L), exception.getMessage());
    }

    @Test
    void GetByUniqueIdNumber_NullUniqueId_ThrowsInvalidDTOException() {
        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.getByUniqueIdNumber(null));
        assertEquals(ExceptionMessages.formatInvalidFieldEmpty("uniqueIdNumber"), exception.getMessage());
    }

    @Test
    void GetByUniqueIdNumber_NonExistentUniqueId_ThrowsEntityNotFoundException() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getByUniqueIdNumber("DOC99999"));
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_UNIQUE_ID, "DOC99999"), exception.getMessage());
    }

    @Test
    void GetAll_NegativePage_ThrowsInvalidInputException() {
        CompletionException ex = assertThrows(CompletionException.class, () -> doctorService.getAll(-1, 10, "name", true, null).join());
        assertTrue(ex.getCause() instanceof InvalidInputException);
        assertEquals("Page number must not be negative", ex.getCause().getMessage());
    }

    @Test
    void GetAll_InvalidPageSize_ThrowsInvalidInputException() {
        CompletionException ex = assertThrows(CompletionException.class, () -> doctorService.getAll(0, 101, "name", true, null).join());
        assertTrue(ex.getCause() instanceof InvalidInputException);
        assertEquals("Page size must be between 1 and 100", ex.getCause().getMessage());
    }

    @Test
    void FindByCriteria_NegativePage_ThrowsInvalidInputException() {
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> doctorService.findByCriteria(Map.of(), -1, 10, "name", true));
        assertEquals("Page number must not be negative", exception.getMessage());
    }

    @Test
    void FindByCriteria_InvalidPageSize_ThrowsInvalidInputException() {
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> doctorService.findByCriteria(Map.of(), 0, 101, "name", true));
        assertEquals("Page size must be between 1 and 100", exception.getMessage());
    }

    @Test
    void GetPatientsByGeneralPractitioner_NonExistentDoctor_ThrowsEntityNotFoundException() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getPatientsByGeneralPractitioner(999L));
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 999L), exception.getMessage());
    }

    @Test
    void GetPatientCountByGeneralPractitioner_NonExistentDoctor_ThrowsEntityNotFoundException() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getPatientCountByGeneralPractitioner(999L));
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 999L), exception.getMessage());
    }

    @Test
    void GetVisitCount_NonExistentDoctor_ThrowsEntityNotFoundException() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getVisitCount(999L));
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 999L), exception.getMessage());
    }

    @Test
    void GetVisitsByPeriod_NonExistentDoctor_ThrowsEntityNotFoundException() {
        CompletionException ex = assertThrows(CompletionException.class, () -> doctorService.getVisitsByPeriod(999L, LocalDate.now(), LocalDate.now()).join());
        assertTrue(ex.getCause() instanceof EntityNotFoundException);
        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 999L), ex.getCause().getMessage());
    }

    @Test
    void GetVisitsByPeriod_NullInputs_ThrowsInvalidDTOException() {
        CompletionException idEx = assertThrows(CompletionException.class, () -> doctorService.getVisitsByPeriod(null, LocalDate.now(), LocalDate.now()).join());
        assertTrue(idEx.getCause() instanceof InvalidDTOException);
        assertEquals(ExceptionMessages.formatInvalidFieldNull("doctorId"), idEx.getCause().getMessage());

        CompletionException startDateEx = assertThrows(CompletionException.class, () -> doctorService.getVisitsByPeriod(doctor.getId(), null, LocalDate.now()).join());
        assertTrue(startDateEx.getCause() instanceof InvalidDTOException);
        assertEquals(ExceptionMessages.formatInvalidFieldNull("startDate"), startDateEx.getCause().getMessage());

        CompletionException endDateEx = assertThrows(CompletionException.class, () -> doctorService.getVisitsByPeriod(doctor.getId(), LocalDate.now(), null).join());
        assertTrue(endDateEx.getCause() instanceof InvalidDTOException);
        assertEquals(ExceptionMessages.formatInvalidFieldNull("endDate"), endDateEx.getCause().getMessage());
    }

    @Test
    void GetVisitsByPeriod_InvalidDateRange_ThrowsInvalidDTOException() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.minusDays(1);

        CompletionException ex = assertThrows(CompletionException.class, () -> doctorService.getVisitsByPeriod(doctor.getId(), startDate, endDate).join());
        assertTrue(ex.getCause() instanceof InvalidDTOException);
        assertEquals(MessageFormat.format(ExceptionMessages.INVALID_DATE_RANGE, startDate, endDate), ex.getCause().getMessage());
    }

    // Edge Cases
    @Test
    void Create_EmptySpecialtyIds_ReturnsDoctorViewDTO() {
        DoctorCreateDTO createDTO = DoctorCreateDTO.builder()
                .name("Dr. Jones")
                .uniqueIdNumber(TestDataUtils.generateUniqueIdNumber())
                .isGeneralPractitioner(false)
                .specialtyIds(Set.of())
                .build();

        DoctorViewDTO result = doctorService.create(createDTO);

        assertNotNull(result);
        assertEquals("Dr. Jones", result.getName());
        assertTrue(result.getSpecialties().isEmpty());
    }

    @Test
    void GetAll_EmptyResults_ReturnsEmptyPage() {
        doctorRepository.deleteAllInBatch();

        Page<DoctorViewDTO> result = doctorService.getAll(0, 10, "name", true, null).join();

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void FindByCriteria_EmptyConditions_ReturnsAllDoctors() {
        Page<DoctorViewDTO> result = doctorService.findByCriteria(Map.of(), 0, 10, "name", true);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Dr. Smith", result.getContent().get(0).getName());
    }

    @Test
    void GetPatientsByGeneralPractitioner_NoPatients_ReturnsEmptyPage() {
        Page<PatientViewDTO> result = doctorService.getPatientsByGeneralPractitioner(doctor.getId());

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void GetPatientCountByGeneralPractitioner_NoPatients_ReturnsZeroCount() {
        DoctorPatientCountDTO result = doctorService.getPatientCountByGeneralPractitioner(doctor.getId());

        assertNotNull(result);
        assertEquals(0L, result.getPatientCount());
    }

    @Test
    void GetVisitCount_NoVisits_ReturnsZeroCount() {
        DoctorVisitCountDTO result = doctorService.getVisitCount(doctor.getId());

        assertNotNull(result);
        assertEquals(0L, result.getVisitCount());
    }

    @Test
    void GetVisitsByPeriod_NoVisits_ReturnsEmptyPage() {
        Page<VisitViewDTO> result = doctorService.getVisitsByPeriod(doctor.getId(), LocalDate.now(), LocalDate.now()).join();

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void GetDoctorsWithMostSickLeaves_NoSickLeaves_ReturnsEmptyList() {
        List<DoctorSickLeaveCountDTO> result = doctorService.getDoctorsWithMostSickLeaves();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}