package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.services.common.exceptions.InvalidPatientException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PatientServiceIntegrationTests {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private EntityManager entityManager;

    private Doctor gp;
    private String validEgn;

    @BeforeEach
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void setUp() {
        // Hard delete to clear all data
        patientRepository.deleteAllInBatch();
        doctorRepository.deleteAllInBatch();

        validEgn = TestDataUtils.generateValidEgn();

        // Setup Doctor
        gp = new Doctor();
        gp.setName("Dr. Smith");
        gp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        gp.setGeneralPractitioner(true);
        gp = doctorRepository.save(gp);
        entityManager.flush();
        entityManager.clear();

        // Verify Doctor persistence
        Doctor persistedDoctor = doctorRepository.findByIdIncludingDeleted(gp.getId())
                .orElseThrow(() -> new AssertionError("Doctor should exist after save"));
        assertFalse(persistedDoctor.getIsDeleted(), "Doctor should not be deleted");
    }

    // Happy Path
    @Test
    void create_ValidDTO_ReturnsPatientViewDTO() {
        PatientCreateDTO createDTO = PatientCreateDTO.builder()
                .name("John Doe")
                .egn(validEgn)
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(gp.getId())
                .build();

        PatientViewDTO result = patientService.create(createDTO);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals(validEgn, result.getEgn());
        assertEquals(gp.getId(), result.getGeneralPractitioner().getId());
    }

    @Test
    void update_ValidDTO_ReturnsUpdatedPatientViewDTO() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        patient.setLastInsurancePaymentDate(LocalDate.now());
        Patient savedPatient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        PatientUpdateDTO updateDTO = PatientUpdateDTO.builder()
                .id(savedPatient.getId())
                .name("Jane Doe")
                .lastInsurancePaymentDate(LocalDate.now().minusDays(1))
                .generalPractitionerId(gp.getId())
                .build();

        PatientViewDTO result = patientService.update(updateDTO);

        assertNotNull(result);
        assertEquals("Jane Doe", result.getName());
        assertEquals(validEgn, result.getEgn());
    }

    @Test
    void delete_ValidId_DeletesPatient() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        Patient savedPatient = patientRepository.save(patient);
        Long patientId = savedPatient.getId();
        entityManager.flush();
        entityManager.clear();

        patientService.delete(patientId);

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.getById(patientId));
        assertEquals(ExceptionMessages.formatPatientNotFoundById(patientId), ex.getMessage());
    }

    @Test
    void getById_ValidId_ReturnsPatientViewDTO() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        Patient savedPatient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        PatientViewDTO result = patientService.getById(savedPatient.getId());

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals(validEgn, result.getEgn());
    }

    @Test
    void getByEgn_ValidEgn_ReturnsPatientViewDTO() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        Patient savedPatient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        PatientViewDTO result = patientService.getByEgn(validEgn);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals(validEgn, result.getEgn());
    }

    @Test
    void getAll_ValidParameters_ReturnsPaginatedPatients() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        Page<PatientViewDTO> result = patientService.getAll(0, 10, "name", true, null).join();

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("John Doe", result.getContent().get(0).getName());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAll_WithFilter_ReturnsFilteredPatients() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        Page<PatientViewDTO> result = patientService.getAll(0, 10, "name", true, "Doe").join();

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("John Doe", result.getContent().get(0).getName());
    }

    @Test
    void getByGeneralPractitioner_ValidId_ReturnsPatients() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        Page<PatientViewDTO> result = patientService.getByGeneralPractitioner(gp.getId(), PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("John Doe", result.getContent().get(0).getName());
    }

    @Test
    void getPatientCountByGeneralPractitioner_ReturnsCounts() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        List<DoctorPatientCountDTO> result = patientService.getPatientCountByGeneralPractitioner();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getPatientCount());
    }

    // Error Cases
    @Test
    void create_NullDTO_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.create(null));
        assertEquals(ExceptionMessages.formatInvalidDTONull("PatientCreateDTO"), ex.getMessage());
    }

    @Test
    void create_DuplicateEgn_ThrowsInvalidPatientException() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        PatientCreateDTO createDTO = PatientCreateDTO.builder()
                .name("Jane Doe")
                .egn(validEgn)
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(gp.getId())
                .build();

        InvalidPatientException ex = assertThrows(InvalidPatientException.class, () -> patientService.create(createDTO));
        assertEquals(ExceptionMessages.formatPatientEgnExists(validEgn), ex.getMessage());
    }

    @Test
    void create_NonExistentGeneralPractitioner_ThrowsEntityNotFoundException() {
        PatientCreateDTO createDTO = PatientCreateDTO.builder()
                .name("John Doe")
                .egn(validEgn)
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(999L)
                .build();

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.create(createDTO));
        assertEquals(ExceptionMessages.formatDoctorNotFoundById(999L), ex.getMessage());
    }

    @Test
    void create_NonGeneralPractitioner_ThrowsInvalidPatientException() {
        Doctor nonGp = new Doctor();
        nonGp.setName("Dr. Jones");
        nonGp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        nonGp.setGeneralPractitioner(false);
        Doctor savedNonGp = doctorRepository.save(nonGp);
        entityManager.flush();
        entityManager.clear();

        PatientCreateDTO createDTO = PatientCreateDTO.builder()
                .name("John Doe")
                .egn(validEgn)
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(savedNonGp.getId())
                .build();

        InvalidPatientException ex = assertThrows(InvalidPatientException.class, () -> patientService.create(createDTO));
        assertEquals(ExceptionMessages.formatInvalidGeneralPractitioner(savedNonGp.getId()), ex.getMessage());
    }

    @Test
    void update_NullDTO_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.update(null));
        assertEquals(ExceptionMessages.formatInvalidDTONull("PatientUpdateDTO"), ex.getMessage());
    }

    @Test
    void update_NullId_ThrowsInvalidDTOException() {
        PatientUpdateDTO updateDTO = PatientUpdateDTO.builder()
                .name("Jane Doe")
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(gp.getId())
                .build();

        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.update(updateDTO));
        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), ex.getMessage());
    }

    @Test
    void update_NonExistentPatient_ThrowsEntityNotFoundException() {
        PatientUpdateDTO updateDTO = PatientUpdateDTO.builder()
                .id(999L)
                .name("Jane Doe")
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(gp.getId())
                .build();

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.update(updateDTO));
        assertEquals(ExceptionMessages.formatPatientNotFoundById(999L), ex.getMessage());
    }

    @Test
    void update_NonExistentGeneralPractitioner_ThrowsEntityNotFoundException() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        Patient savedPatient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        PatientUpdateDTO updateDTO = PatientUpdateDTO.builder()
                .id(savedPatient.getId())
                .name("Jane Doe")
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(999L)
                .build();

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.update(updateDTO));
        assertEquals(ExceptionMessages.formatDoctorNotFoundById(999L), ex.getMessage());
    }

    @Test
    void update_NonGeneralPractitioner_ThrowsInvalidPatientException() {
        Doctor nonGp = new Doctor();
        nonGp.setName("Dr. Jones");
        nonGp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        nonGp.setGeneralPractitioner(false);
        Doctor savedNonGp = doctorRepository.save(nonGp);
        entityManager.flush();
        entityManager.clear();

        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setGeneralPractitioner(gp);
        Patient savedPatient = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        PatientUpdateDTO updateDTO = PatientUpdateDTO.builder()
                .id(savedPatient.getId())
                .name("Jane Doe")
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(savedNonGp.getId())
                .build();

        InvalidPatientException ex = assertThrows(InvalidPatientException.class, () -> patientService.update(updateDTO));
        assertEquals(ExceptionMessages.formatInvalidGeneralPractitioner(savedNonGp.getId()), ex.getMessage());
    }

    @Test
    void delete_NullId_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.delete(null));
        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), ex.getMessage());
    }

    @Test
    void delete_NonExistentPatient_ThrowsEntityNotFoundException() {
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.delete(999L));
        assertEquals(ExceptionMessages.formatPatientNotFoundById(999L), ex.getMessage());
    }

    @Test
    void getById_NullId_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.getById(null));
        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), ex.getMessage());
    }

    @Test
    void getById_NonExistentId_ThrowsEntityNotFoundException() {
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.getById(999L));
        assertEquals(ExceptionMessages.formatPatientNotFoundById(999L), ex.getMessage());
    }

    @Test
    void getByEgn_NullEgn_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.getByEgn(null));
        assertEquals(ExceptionMessages.formatInvalidFieldEmpty("EGN"), ex.getMessage());
    }

    @Test
    void getByEgn_EmptyEgn_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.getByEgn(""));
        assertEquals(ExceptionMessages.formatInvalidFieldEmpty("EGN"), ex.getMessage());
    }

    @Test
    void getByEgn_NonExistentEgn_ThrowsEntityNotFoundException() {
        String nonExistentEgn = TestDataUtils.generateValidEgn();
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.getByEgn(nonExistentEgn));
        assertEquals(ExceptionMessages.formatPatientNotFoundByEgn(nonExistentEgn), ex.getMessage());
    }

    @Test
    void getAll_NegativePage_ThrowsInvalidDTOException() {
        CompletionException ex = assertThrows(CompletionException.class, () -> patientService.getAll(-1, 10, "name", true, null).join());
        assertTrue(ex.getCause() instanceof InvalidDTOException);
        assertEquals("Page number must not be negative", ex.getCause().getMessage());
    }

    @Test
    void getAll_InvalidPageSize_ThrowsInvalidDTOException() {
        CompletionException ex = assertThrows(CompletionException.class, () -> patientService.getAll(0, 101, "name", true, null).join());
        assertTrue(ex.getCause() instanceof InvalidDTOException);
        assertEquals("Page size must be between 1 and 100", ex.getCause().getMessage());
    }

    @Test
    void getByGeneralPractitioner_NonExistentDoctor_ThrowsEntityNotFoundException() {
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.getByGeneralPractitioner(999L, PageRequest.of(0, 10)));
        assertEquals(ExceptionMessages.formatDoctorNotFoundById(999L), ex.getMessage());
    }

    @Test
    void getByGeneralPractitioner_NonGeneralPractitioner_ThrowsInvalidPatientException() {
        Doctor nonGp = new Doctor();
        nonGp.setName("Dr. Jones");
        nonGp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        nonGp.setGeneralPractitioner(false);
        Doctor savedNonGp = doctorRepository.save(nonGp);
        entityManager.flush();
        entityManager.clear();

        InvalidPatientException ex = assertThrows(InvalidPatientException.class, () -> patientService.getByGeneralPractitioner(savedNonGp.getId(), PageRequest.of(0, 10)));
        assertEquals(ExceptionMessages.formatInvalidGeneralPractitioner(savedNonGp.getId()), ex.getMessage());
    }

    // Edge Cases
    @Test
    void getAll_EmptyResults_ReturnsEmptyPage() {
        Page<PatientViewDTO> result = patientService.getAll(0, 10, "name", true, null).join();

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getAll_FilterNoMatches_ReturnsEmptyPage() {
        Page<PatientViewDTO> result = patientService.getAll(0, 10, "name", true, "NonExistent").join();

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getByGeneralPractitioner_NoPatients_ReturnsEmptyPage() {
        Page<PatientViewDTO> result = patientService.getByGeneralPractitioner(gp.getId(), PageRequest.of(0, 10));

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getPatientCountByGeneralPractitioner_EmptyResults_ReturnsEmptyList() {
        List<DoctorPatientCountDTO> result = patientService.getPatientCountByGeneralPractitioner();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}