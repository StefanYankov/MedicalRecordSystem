package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.common.exceptions.InvalidPatientException;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.PatientService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(PatientServiceImplIntegrationTests.AsyncTestConfig.class)
class PatientServiceImplIntegrationTests {

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private VisitRepository visitRepository;

    private Doctor testDoctor;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setName("Dr. House");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "new-patient-id", authorities = "ROLE_PATIENT")
    void registerPatient_WithValidDataAsPatient_ShouldSucceed_HappyPath() {
        // ARRANGE
        PatientCreateDTO createDTO = new PatientCreateDTO();
        createDTO.setName("New Patient");
        createDTO.setEgn(TestDataUtils.generateValidEgn());
        createDTO.setGeneralPractitionerId(testDoctor.getId());

        // ACT
        PatientViewDTO result = patientService.registerPatient(createDTO);

        // ASSERT
        assertNotNull(result);
        assertEquals("New Patient", result.getName());

        Patient savedPatient = patientRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedPatient);
        assertEquals("new-patient-id", savedPatient.getKeycloakId());
        assertEquals(testDoctor.getId(), savedPatient.getGeneralPractitioner().getId());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "existing-patient-id", authorities = "ROLE_PATIENT")
    void registerPatient_WhenPatientProfileAlreadyExists_ShouldThrowException_ErrorCase() {
        // ARRANGE
        Patient existingPatient = new Patient();
        existingPatient.setKeycloakId("existing-patient-id");
        existingPatient.setName("Already Exists");
        existingPatient.setEgn(TestDataUtils.generateValidEgn());
        existingPatient.setGeneralPractitioner(testDoctor);
        patientRepository.save(existingPatient);

        PatientCreateDTO createDTO = new PatientCreateDTO();
        createDTO.setName("New Attempt");
        createDTO.setEgn(TestDataUtils.generateValidEgn());
        createDTO.setGeneralPractitionerId(testDoctor.getId());

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.registerPatient(createDTO));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void registerPatient_AsDoctor_ShouldBeDenied_ErrorCase() {
        // ARRANGE
        PatientCreateDTO createDTO = new PatientCreateDTO();

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> patientService.registerPatient(createDTO));
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
    void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
        // ARRANGE
        Patient patient = new Patient();
        patient.setKeycloakId("patient-owner-id");
        patient.setName("Patient Owner");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(testDoctor);
        patient = patientRepository.save(patient);

        // ACT
        PatientViewDTO result = patientService.getById(patient.getId());

        // ASSERT
        assertNotNull(result);
        assertEquals(patient.getId(), result.getId());
        assertEquals("Patient Owner", result.getName());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "other-patient-id", authorities = "ROLE_PATIENT")
    void getById_AsOtherPatient_ShouldBeDenied_ErrorCase() {
        // ARRANGE
        Patient patient = new Patient();
        patient.setKeycloakId(TestDataUtils.generateKeycloakId()); // Different ID
        patient.setName("Patient Owner");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(testDoctor);
        patient = patientRepository.save(patient);

        Long patientIdToFetch = patient.getId();

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> patientService.getById(patientIdToFetch));
    }

    @Test
    void getById_AsDoctor_ShouldSucceed_HappyPath() {
        // ARRANGE
        // Manual security context setup for this specific test to ensure stability
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass", Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR")))
        );

        Patient patient = new Patient();
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setName("Patient Owner");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(testDoctor);
        patient = patientRepository.save(patient);

        // ACT
        PatientViewDTO result = patientService.getById(patient.getId());

        // ASSERT
        assertNotNull(result);
        assertEquals(patient.getId(), result.getId());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void create_AsAdmin_ShouldSucceed_HappyPath() {
        // ARRANGE
        PatientCreateDTO createDTO = new PatientCreateDTO();
        createDTO.setName("Patient by Admin");
        createDTO.setEgn(TestDataUtils.generateValidEgn());
        createDTO.setGeneralPractitionerId(testDoctor.getId());
        createDTO.setKeycloakId("patient-created-by-admin-id");

        // ACT
        PatientViewDTO result = patientService.create(createDTO);

        // ASSERT
        assertNotNull(result);
        Patient saved = patientRepository.findByKeycloakId("patient-created-by-admin-id").orElse(null);
        assertNotNull(saved);
        assertEquals("Patient by Admin", saved.getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void create_AsPatient_ShouldBeDenied_ErrorCase() {
        // ARRANGE
        PatientCreateDTO createDTO = new PatientCreateDTO();

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> patientService.create(createDTO));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void delete_AsAdmin_ShouldSucceed_HappyPath() {
        // ARRANGE
        Patient patient = new Patient();
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setName("To Delete");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(testDoctor);
        patient = patientRepository.save(patient);
        long patientId = patient.getId();

        // ACT
        patientService.delete(patientId);

        // ASSERT
        Optional<Patient> deleted = patientRepository.findById(patientId);
        assertFalse(deleted.isPresent());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void delete_AsPatient_ShouldBeDenied_ErrorCase() {
        // ARRANGE
        Patient patient = new Patient();
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setName("Cannot Delete");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(testDoctor);
        patient = patientRepository.save(patient);
        long patientId = patient.getId();

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> patientService.delete(patientId));
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "patient-to-update", authorities = "ROLE_PATIENT")
    void update_AsPatientOwner_ShouldSucceed_HappyPath() {
        // ARRANGE
        Patient patient = new Patient();
        patient.setKeycloakId("patient-to-update");
        patient.setName("Original Name");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(testDoctor);
        patient = patientRepository.save(patient);

        PatientUpdateDTO updateDTO = new PatientUpdateDTO();
        updateDTO.setId(patient.getId());
        updateDTO.setName("Updated Name");
        updateDTO.setEgn(patient.getEgn());
        updateDTO.setGeneralPractitionerId(testDoctor.getId());

        // ACT
        PatientViewDTO result = patientService.update(updateDTO);

        // ASSERT
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        Patient updatedPatient = patientRepository.findById(patient.getId()).get();
        assertEquals("Updated Name", updatedPatient.getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void update_AsAdmin_ShouldSucceed_HappyPath() {
        // ARRANGE
        Patient patient = new Patient();
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setName("Original Name");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(testDoctor);
        patient = patientRepository.save(patient);

        PatientUpdateDTO updateDTO = new PatientUpdateDTO();
        updateDTO.setId(patient.getId());
        updateDTO.setName("Updated by Admin");
        updateDTO.setEgn(patient.getEgn());
        updateDTO.setGeneralPractitionerId(testDoctor.getId());

        // ACT
        PatientViewDTO result = patientService.update(updateDTO);

        // ASSERT
        assertNotNull(result);
        assertEquals("Updated by Admin", result.getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void getAll_AsPatient_ShouldBeDenied_ErrorCase() {
        // ARRANGE
        CompletableFuture<Page<PatientViewDTO>> future = patientService.getAll(0, 10, "name", true, null);

        // ACT & ASSERT
        ExecutionException exception = assertThrows(ExecutionException.class, future::get, "Expected ExecutionException to be thrown");
        assertInstanceOf(AccessDeniedException.class, exception.getCause(), "Expected cause to be AccessDeniedException");
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getPatientCountByGeneralPractitioner_AsAdmin_ShouldSucceed_HappyPath() {
        // ARRANGE
        Patient patient = new Patient();
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setName("Test Patient for Count");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(testDoctor);
        patientRepository.save(patient);

        // ACT
        var result = patientService.getPatientCountByGeneralPractitioner();

        // ASSERT
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).getPatientCount());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void getPatientCountByGeneralPractitioner_AsDoctor_ShouldBeDenied_ErrorCase() {
        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> patientService.getPatientCountByGeneralPractitioner());
    }
}
