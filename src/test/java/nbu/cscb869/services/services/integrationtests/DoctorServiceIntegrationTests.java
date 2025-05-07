package nbu.cscb869.services.services.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidDoctorException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DoctorServiceIntegrationTests {
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
    private EntityManager entityManager;

    private Specialty specialty;

    @BeforeEach
    void setUp() {
        // Delete in correct order to avoid foreign key constraints
        entityManager.createNativeQuery("DELETE FROM sick_leaves").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM visits").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM patients").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM doctor_specialties").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM doctors").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM specialties").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM diagnoses").executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // Create a specialty for use in tests
        specialty = new Specialty();
        specialty.setName("Cardiology");
        specialty.setDescription("Heart-related conditions");
        specialty = specialtyRepository.saveAndFlush(specialty);
    }

    private DoctorCreateDTO createDoctorCreateDTO(String name, String uniqueIdNumber, boolean isGeneralPractitioner, Set<Long> specialtyIds) {
        return DoctorCreateDTO.builder()
                .name(name)
                .uniqueIdNumber(uniqueIdNumber)
                .isGeneralPractitioner(isGeneralPractitioner)
                .imageUrl("http://example.com/image.jpg")
                .specialtyIds(specialtyIds)
                .build();
    }

    private DoctorUpdateDTO createDoctorUpdateDTO(Long id, String name, boolean isGeneralPractitioner, Set<Long> specialtyIds) {
        return DoctorUpdateDTO.builder()
                .id(id)
                .name(name)
                .isGeneralPractitioner(isGeneralPractitioner)
                .imageUrl("http://example.com/updated-image.jpg")
                .specialtyIds(specialtyIds)
                .build();
    }

    private Patient createPatient(String name, String egn, Doctor generalPractitioner) {
        Patient patient = new Patient();
        patient.setName(name);
        patient.setEgn(egn);
        patient.setGeneralPractitioner(generalPractitioner);
        return patientRepository.saveAndFlush(patient);
    }

    private Diagnosis createDiagnosis() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        diagnosis.setDescription("Viral infection");
        entityManager.persist(diagnosis);
        entityManager.flush();
        return diagnosis;
    }

    // Happy Path
    @Test
    void Create_ValidDTO_SavesSuccessfully() {
        DoctorCreateDTO dto = createDoctorCreateDTO("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true, Set.of(specialty.getId()));
        DoctorViewDTO result = doctorService.create(dto);
        entityManager.flush();

        assertNotNull(result.getId());
        assertEquals("Dr. Smith", result.getName());
        assertTrue(result.isGeneralPractitioner());
        assertEquals("http://example.com/image.jpg", result.getImageUrl());
        assertEquals(1, result.getSpecialties().size());
        assertTrue(result.getSpecialties().stream().anyMatch(s -> s.getName().equals("Cardiology")));

        Doctor saved = doctorRepository.findById(result.getId()).orElseThrow();
        assertFalse(saved.getIsDeleted());
        assertNotNull(saved.getCreatedOn());
        assertNotNull(saved.getVersion());
    }

    @Test
    void Update_ValidDTO_UpdatesSuccessfully() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Jones");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);
        doctor.setSpecialties(Set.of(specialty));
        doctor = doctorRepository.saveAndFlush(doctor);
        entityManager.clear();

        DoctorUpdateDTO dto = createDoctorUpdateDTO(doctor.getId(), "Dr. Updated Jones", false, Set.of());
        DoctorViewDTO result = doctorService.update(dto);
        entityManager.flush();

        assertEquals(doctor.getId(), result.getId());
        assertEquals("Dr. Updated Jones", result.getName());
        assertFalse(result.isGeneralPractitioner());
        assertEquals("http://example.com/updated-image.jpg", result.getImageUrl());
        assertTrue(result.getSpecialties().isEmpty());

        Doctor updated = doctorRepository.findById(doctor.getId()).orElseThrow();
        assertEquals(1L, updated.getVersion());
        assertNotNull(updated.getModifiedOn());
    }

    @Test
    void Delete_ExistingId_SoftDeletesSuccessfully() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Brown");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor = doctorRepository.saveAndFlush(doctor);

        doctorService.delete(doctor.getId());
        entityManager.flush();

        Doctor deleted = doctorRepository.findByIdIncludingDeleted(doctor.getId()).orElseThrow();
        assertTrue(deleted.getIsDeleted());
        assertNotNull(deleted.getDeletedOn());
        assertEquals(0, doctorRepository.findAllActive().size());
    }

    @Test
    void GetById_ExistingId_ReturnsDoctor() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Wilson");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setSpecialties(Set.of(specialty));
        doctor = doctorRepository.saveAndFlush(doctor);

        DoctorViewDTO result = doctorService.getById(doctor.getId());

        assertEquals(doctor.getId(), result.getId());
        assertEquals("Dr. Wilson", result.getName());
        assertEquals(1, result.getSpecialties().size());
        assertTrue(result.getSpecialties().stream().anyMatch(s -> s.getName().equals("Cardiology")));
    }

    @Test
    void GetByUniqueIdNumber_ExistingId_ReturnsDoctor() {
        String uniqueId = TestDataUtils.generateUniqueIdNumber();
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Taylor");
        doctor.setUniqueIdNumber(uniqueId);
        doctor = doctorRepository.saveAndFlush(doctor);

        DoctorViewDTO result = doctorService.getByUniqueIdNumber(uniqueId);

        assertEquals(doctor.getId(), result.getId());
        assertEquals("Dr. Taylor", result.getName());
        assertEquals(uniqueId, result.getUniqueIdNumber());
    }

    @Test
    void GetAll_EmptyFilter_ReturnsAll() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Clark");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctorRepository.saveAndFlush(doctor);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        entityManager.clear();

        Page<DoctorViewDTO> result = doctorService.getAll(0, 10, "name", true, "").join();

        assertEquals(1, result.getTotalElements());
        assertEquals("Dr. Clark", result.getContent().get(0).getName());
    }

    @Test
    void GetAll_ValidFilter_ReturnsFiltered() {
        Doctor doctor1 = new Doctor();
        doctor1.setName("Dr. Adams");
        doctor1.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        Doctor doctor2 = new Doctor();
        doctor2.setName("Dr. Anderson");
        doctor2.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctorRepository.saveAndFlush(doctor1);
        doctorRepository.saveAndFlush(doctor2);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        entityManager.clear();

        Page<DoctorViewDTO> result = doctorService.getAll(0, 10, "name", true, "Ad").join();

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(d -> d.getName().equals("Dr. Adams")));
        assertTrue(result.getContent().stream().anyMatch(d -> d.getName().equals("Dr. Anderson")));
    }

    @Test
    void FindByCriteria_NameAndSpecialty_ReturnsFiltered() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Lee");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setSpecialties(Set.of(specialty));
        doctorRepository.saveAndFlush(doctor);

        Map<String, Object> conditions = Map.of(
                "name", "Lee",
                "specialtyId", specialty.getId()
        );
        Page<DoctorViewDTO> result = doctorService.findByCriteria(conditions, 0, 10, "name", true);

        assertEquals(1, result.getTotalElements());
        assertEquals("Dr. Lee", result.getContent().get(0).getName());
    }

    @Test
    void GetPatientsByGeneralPractitioner_ValidDoctorId_ReturnsPatients() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Moore");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);
        doctor = doctorRepository.saveAndFlush(doctor);
        Patient patient = createPatient("John Doe", TestDataUtils.generateValidEgn(), doctor);

        Page<PatientViewDTO> result = doctorService.getPatientsByGeneralPractitioner(doctor.getId());

        assertEquals(1, result.getTotalElements());
        assertEquals("John Doe", result.getContent().get(0).getName());
    }

    @Test
    void GetPatientCountByGeneralPractitioner_ValidDoctorId_ReturnsCount() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Harris");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);
        doctor = doctorRepository.saveAndFlush(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor);

        DoctorPatientCountDTO result = doctorService.getPatientCountByGeneralPractitioner(doctor.getId());

        assertEquals(1L, result.getPatientCount());
        assertEquals("Dr. Harris", result.getDoctor().getName());
    }

    @Test
    void GetVisitCount_ValidDoctorId_ReturnsCount() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Lewis");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor = doctorRepository.saveAndFlush(doctor);
        Patient patient = createPatient("Bob Smith", TestDataUtils.generateValidEgn(), doctor);
        Diagnosis diagnosis = createDiagnosis();
        Visit visit = new Visit();
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(java.time.LocalTime.now());
        visit.setSickLeaveIssued(false);
        visitRepository.saveAndFlush(visit);

        DoctorVisitCountDTO result = doctorService.getVisitCount(doctor.getId());

        assertEquals(1L, result.getVisitCount());
        assertEquals("Dr. Lewis", result.getDoctor().getName());
    }

    @Test
    void GetVisitsByPeriod_ValidParameters_ReturnsVisits() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Walker");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor = doctorRepository.saveAndFlush(doctor);
        Patient patient = createPatient("Alice Brown", TestDataUtils.generateValidEgn(), doctor);
        Diagnosis diagnosis = createDiagnosis();
        Visit visit = new Visit();
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(java.time.LocalTime.now());
        visit.setSickLeaveIssued(false);
        visitRepository.saveAndFlush(visit);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Page<VisitViewDTO> result = doctorService.getVisitsByPeriod(doctor.getId(), startDate, endDate).join();

        assertEquals(1, result.getTotalElements());
        assertEquals(doctor.getId(), result.getContent().get(0).getDoctor().getId());
    }

    @Test
    void GetDoctorsWithMostSickLeaves_ValidData_ReturnsCounts() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Young");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor = doctorRepository.saveAndFlush(doctor);
        Patient patient = createPatient("Carol White", TestDataUtils.generateValidEgn(), doctor);
        Diagnosis diagnosis = createDiagnosis();
        Visit visit = new Visit();
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(java.time.LocalTime.now());
        visit.setSickLeaveIssued(true);
        visit = visitRepository.saveAndFlush(visit);
        entityManager.createNativeQuery("INSERT INTO sick_leaves (id, start_date, duration_days, visit_id, is_deleted, version) " +
                        "VALUES (1, ?, 5, ?, false, 0)")
                .setParameter(1, LocalDate.now())
                .setParameter(2, visit.getId())
                .executeUpdate();
        entityManager.flush();

        List<DoctorSickLeaveCountDTO> result = doctorService.getDoctorsWithMostSickLeaves();

        assertEquals(1, result.size());
        assertEquals("Dr. Young", result.get(0).getDoctor().getName());
        assertEquals(1L, result.get(0).getSickLeaveCount());
    }

    // Error Cases
    @Test
    void Create_NullDTO_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.create(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
        assertEquals("DoctorCreateDTO must not be null", exception.getMessage());
    }

    @Test
    void Create_BlankName_ThrowsInvalidInputException() {
        DoctorCreateDTO dto = createDoctorCreateDTO("", TestDataUtils.generateUniqueIdNumber(), true, Set.of(specialty.getId()));
        Exception exception = null;
        try {
            doctorService.create(dto);
            entityManager.flush();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidInputException.class, exception);
    }

    @Test
    void Create_DuplicateUniqueIdNumber_ThrowsInvalidDoctorException() {
        String uniqueId = TestDataUtils.generateUniqueIdNumber();
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Green");
        doctor.setUniqueIdNumber(uniqueId);
        doctorRepository.saveAndFlush(doctor);

        DoctorCreateDTO dto = createDoctorCreateDTO("Dr. Blue", uniqueId, true, Set.of());
        Exception exception = null;
        try {
            doctorService.create(dto);
            entityManager.flush();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDoctorException.class, exception);
        assertEquals(MessageFormat.format("A doctor with unique ID {0} already exists",uniqueId ), exception.getMessage());
    }

    @Test
    void Create_InvalidSpecialtyId_ThrowsInvalidDoctorException() {
        DoctorCreateDTO dto = createDoctorCreateDTO("Dr. Black", TestDataUtils.generateUniqueIdNumber(), true, Set.of(999L));
        Exception exception = null;
        try {
            doctorService.create(dto);
            entityManager.flush();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDoctorException.class, exception);
        assertEquals("One or more specialty IDs are invalid", exception.getMessage());
    }

    @Test
    void Update_NullDTO_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.update(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
        assertEquals("DoctorUpdateDTO must not be null", exception.getMessage());
    }

    @Test
    void Update_NullId_ThrowsInvalidDTOException() {
        DoctorUpdateDTO dto = createDoctorUpdateDTO(null, "Dr. White", true, Set.of());
        Exception exception = null;
        try {
            doctorService.update(dto);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
        assertEquals("ID must not be null", exception.getMessage());
    }

    @Test
    void Update_NonExistentId_ThrowsEntityNotFoundException() {
        DoctorUpdateDTO dto = createDoctorUpdateDTO(999L, "Dr. Gray", true, Set.of());
        Exception exception = null;
        try {
            doctorService.update(dto);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(EntityNotFoundException.class, exception);
        assertEquals("Doctor not found with ID: 999", exception.getMessage());
    }

    @Test
    void Delete_NullId_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.delete(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
        assertEquals("ID must not be null", exception.getMessage());
    }

    @Test
    void Delete_NonExistentId_ThrowsEntityNotFoundException() {
        Exception exception = null;
        try {
            doctorService.delete(999L);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(EntityNotFoundException.class, exception);
        assertEquals("Doctor not found with ID: 999", exception.getMessage());
    }

    @Test
    void Delete_GeneralPractitionerWithPatients_ThrowsInvalidDoctorException() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. King");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);
        doctor = doctorRepository.saveAndFlush(doctor);
        Patient patient = createPatient("Alice Smith", TestDataUtils.generateValidEgn(), doctor);

        Exception exception = null;
        try {
            doctorService.delete(doctor.getId());
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDoctorException.class, exception);
        assertEquals("Cannot delete doctor who is a general practitioner for active patients", exception.getMessage());
    }

    @Test
    void GetById_NullId_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.getById(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
        assertEquals("ID must not be null", exception.getMessage());
    }

    @Test
    void GetById_NonExistentId_ThrowsEntityNotFoundException() {
        Exception exception = null;
        try {
            doctorService.getById(999L);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(EntityNotFoundException.class, exception);
        assertEquals("Doctor not found with ID: 999", exception.getMessage());
    }

    @Test
    void GetByUniqueIdNumber_NullId_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.getByUniqueIdNumber(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
        assertEquals("uniqueIdNumber must not be empty", exception.getMessage());
    }

    @Test
    void GetByUniqueIdNumber_NonExistentId_ThrowsEntityNotFoundException() {
        Exception exception = null;
        try {
            doctorService.getByUniqueIdNumber("NONEXISTENT");
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(EntityNotFoundException.class, exception);
        assertEquals("Doctor not found with unique ID: NONEXISTENT", exception.getMessage());
    }

    @Test
    void GetAll_NegativePage_ThrowsInvalidInputException() {
        Exception exception = null;
        try {
            doctorService.getAll(-1, 10, "name", true, null).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidInputException.class, exception.getCause());
        assertEquals("Page number must not be negative", exception.getCause().getMessage());
    }

    @Test
    void GetAll_ZeroPageSize_ThrowsInvalidInputException() {
        Exception exception = null;
        try {
            doctorService.getAll(0, 0, "name", true, null).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidInputException.class, exception.getCause());
        assertEquals("Page size must be between 1 and 100", exception.getCause().getMessage());
    }

    @Test
    void GetAll_ExcessivePageSize_ThrowsInvalidInputException() {
        Exception exception = null;
        try {
            doctorService.getAll(0, 101, "name", true, null).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidInputException.class, exception.getCause());
        assertEquals("Page size must be between 1 and 100", exception.getCause().getMessage());
    }

    @Test
    void GetPatientsByGeneralPractitioner_NullDoctorId_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.getPatientsByGeneralPractitioner(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
        assertEquals("doctorId must not be null", exception.getMessage());
    }

    @Test
    void GetPatientsByGeneralPractitioner_NonExistentDoctorId_ThrowsEntityNotFoundException() {
        Exception exception = null;
        try {
            doctorService.getPatientsByGeneralPractitioner(999L);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(EntityNotFoundException.class, exception);
        assertEquals("Doctor not found with ID: 999", exception.getMessage());
    }

    @Test
    void GetPatientCountByGeneralPractitioner_NullDoctorId_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.getPatientCountByGeneralPractitioner(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
        assertEquals("doctorId must not be null", exception.getMessage());
    }

    @Test
    void GetVisitCount_NullDoctorId_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.getVisitCount(null);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(InvalidDTOException.class, exception);
        assertEquals("doctorId must not be null", exception.getMessage());
    }

    @Test
    void GetVisitsByPeriod_NullDoctorId_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.getVisitsByPeriod(null, LocalDate.now(), LocalDate.now()).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidDTOException.class, exception.getCause());
        assertEquals("doctorId must not be null", exception.getCause().getMessage());
    }

    @Test
    void GetVisitsByPeriod_NullStartDate_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.getVisitsByPeriod(1L, null, LocalDate.now()).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidDTOException.class, exception.getCause());
        assertEquals("startDate must not be null", exception.getCause().getMessage());
    }

    @Test
    void GetVisitsByPeriod_NullEndDate_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.getVisitsByPeriod(1L, LocalDate.now(), null).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidDTOException.class, exception.getCause());
        assertEquals("endDate must not be null", exception.getCause().getMessage());
    }

    @Test
    void GetVisitsByPeriod_InvalidDateRange_ThrowsInvalidDTOException() {
        Exception exception = null;
        try {
            doctorService.getVisitsByPeriod(1L, LocalDate.now(), LocalDate.now().minusDays(1)).join();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertInstanceOf(CompletionException.class, exception);
        assertNotNull(exception.getCause());
        assertInstanceOf(InvalidDTOException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Start date 2025-05-07 must not be after end date 2025-05-06"));
    }

    // Edge Cases
    @Test
    void Create_MaximumNameLength_SavesSuccessfully() {
        String maxName = "A".repeat(100);
        DoctorCreateDTO dto = createDoctorCreateDTO(maxName, TestDataUtils.generateUniqueIdNumber(), true, Set.of());
        DoctorViewDTO result = doctorService.create(dto);
        entityManager.flush();

        assertEquals(maxName, result.getName());
    }

    @Test
    void Create_EmptySpecialties_SavesSuccessfully() {
        DoctorCreateDTO dto = createDoctorCreateDTO("Dr. Evans", TestDataUtils.generateUniqueIdNumber(), true, Set.of());
        DoctorViewDTO result = doctorService.create(dto);
        entityManager.flush();

        assertEquals("Dr. Evans", result.getName());
        assertTrue(result.getSpecialties().isEmpty());
    }

    @Test
    void Update_SameName_UpdatesSuccessfully() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Carter");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor = doctorRepository.saveAndFlush(doctor);
        entityManager.clear();

        DoctorUpdateDTO dto = createDoctorUpdateDTO(doctor.getId(), "Dr. Carter", false, Set.of(specialty.getId()));
        DoctorViewDTO result = doctorService.update(dto);
        entityManager.flush();

        assertEquals("Dr. Carter", result.getName());
        assertEquals(1, result.getSpecialties().size());
    }

    @Test
    void FindByCriteria_EmptyConditions_ReturnsAll() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Turner");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctorRepository.saveAndFlush(doctor);

        Page<DoctorViewDTO> result = doctorService.findByCriteria(Map.of(), 0, 10, "name", true);

        assertEquals(1, result.getTotalElements());
        assertEquals("Dr. Turner", result.getContent().get(0).getName());
    }

    @Test
    void GetDoctorsWithMostSickLeaves_NoSickLeaves_ReturnsEmpty() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Parker");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctorRepository.saveAndFlush(doctor);

        List<DoctorSickLeaveCountDTO> result = doctorService.getDoctorsWithMostSickLeaves();

        assertTrue(result.isEmpty());
    }
}