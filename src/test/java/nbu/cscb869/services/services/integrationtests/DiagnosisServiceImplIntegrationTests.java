package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(DiagnosisServiceImplIntegrationTests.AsyncTestConfig.class)
class DiagnosisServiceImplIntegrationTests {

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private DiagnosisService diagnosisService;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private Doctor testDoctor;
    private Patient testPatient;
    private Diagnosis testDiagnosis;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();

        testDoctor = doctorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Doctor d = new Doctor();
            d.setName("Dr. House");
            d.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            d.setKeycloakId(TestDataUtils.generateKeycloakId());
            d.setGeneralPractitioner(true);
            return doctorRepository.save(d);
        });

        testPatient = patientRepository.findAll().stream().findFirst().orElseGet(() -> {
            Patient p = new Patient();
            p.setName("Test Patient");
            p.setEgn(TestDataUtils.generateValidEgn());
            p.setGeneralPractitioner(testDoctor);
            p.setKeycloakId(TestDataUtils.generateKeycloakId());
            return patientRepository.save(p);
        });

        testDiagnosis = diagnosisRepository.findByName("Flu").orElseGet(() -> {
            Diagnosis d = new Diagnosis();
            d.setName("Flu");
            d.setDescription("Viral infection");
            return diagnosisRepository.save(d);
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Create Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void create_AsAdminWithValidData_ShouldSucceed_HappyPath() {
        DiagnosisCreateDTO createDTO = new DiagnosisCreateDTO("Common Cold", "Viral infection of the nose and throat");
        DiagnosisViewDTO result = diagnosisService.create(createDTO);
        assertNotNull(result);
        assertEquals("Common Cold", result.getName());
        assertTrue(diagnosisRepository.findById(result.getId()).isPresent());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void create_WithExistingName_ShouldThrowException_ErrorCase() {
        DiagnosisCreateDTO createDTO = new DiagnosisCreateDTO("Flu", "Duplicate infection");
        assertThrows(InvalidDTOException.class, () -> diagnosisService.create(createDTO));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void create_AsDoctor_ShouldBeDenied_ErrorCase() {
        DiagnosisCreateDTO createDTO = new DiagnosisCreateDTO("Unauthorized Flu", "Viral infection");
        assertThrows(AccessDeniedException.class, () -> diagnosisService.create(createDTO));
    }

    // --- Update Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void update_AsAdminWithValidData_ShouldSucceed_HappyPath() {
        DiagnosisUpdateDTO updateDTO = new DiagnosisUpdateDTO(testDiagnosis.getId(), "Influenza", "A serious viral infection");
        DiagnosisViewDTO result = diagnosisService.update(updateDTO);
        assertEquals("Influenza", result.getName());
        assertEquals("A serious viral infection", result.getDescription());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void update_AsDoctor_ShouldBeDenied_ErrorCase() {
        DiagnosisUpdateDTO updateDTO = new DiagnosisUpdateDTO(testDiagnosis.getId(), "New Flu", "");
        assertThrows(AccessDeniedException.class, () -> diagnosisService.update(updateDTO));
    }

    // --- Delete Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void delete_AsAdmin_ShouldSucceed_HappyPath() {
        // Create a new diagnosis to delete to avoid affecting other tests
        Diagnosis toDelete = new Diagnosis();
        toDelete.setName("To Delete");
        toDelete = diagnosisRepository.save(toDelete);
        long diagnosisId = toDelete.getId();

        diagnosisService.delete(diagnosisId);
        assertFalse(diagnosisRepository.findById(diagnosisId).isPresent());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void delete_WithNonExistentId_ShouldThrowException_ErrorCase() {
        assertThrows(EntityNotFoundException.class, () -> diagnosisService.delete(999L));
    }

    // --- GetById/GetByName Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void getById_AsDoctor_ShouldSucceed_HappyPath() {
        DiagnosisViewDTO result = diagnosisService.getById(testDiagnosis.getId());
        assertNotNull(result);
        assertEquals(testDiagnosis.getName(), result.getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void getById_AsPatient_ShouldBeDenied_ErrorCase() {
        assertThrows(AccessDeniedException.class, () -> diagnosisService.getById(testDiagnosis.getId()));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getByName_AsAdmin_ShouldSucceed_HappyPath() {
        DiagnosisViewDTO result = diagnosisService.getByName("Flu");
        assertNotNull(result);
        assertEquals(testDiagnosis.getId(), result.getId());
    }

    // --- GetAll Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void getAll_AsDoctorWithFilter_ShouldReturnFilteredPage_HappyPath() throws ExecutionException, InterruptedException {
        // ARRANGE: This test commits, so it must create its own isolated data.
        Diagnosis allergy = new Diagnosis();
        allergy.setName("Allergy");
        allergy.setDescription("Immune system response");
        diagnosisRepository.save(allergy);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // ACT
        CompletableFuture<Page<DiagnosisViewDTO>> resultFuture = diagnosisService.getAll(0, 10, "name", true, "Allergy");
        Page<DiagnosisViewDTO> result = resultFuture.get();

        // ASSERT
        assertEquals(1, result.getTotalElements());
        assertEquals("Allergy", result.getContent().get(0).getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void getAll_AsPatient_ShouldBeDenied_ErrorCase() {
        CompletableFuture<Page<DiagnosisViewDTO>> future = diagnosisService.getAll(0, 10, "name", true, null);
        ExecutionException exception = assertThrows(ExecutionException.class, future::get, "Expected ExecutionException to be thrown");
        assertInstanceOf(AccessDeniedException.class, exception.getCause(), "Expected cause to be AccessDeniedException");
    }

    // --- Reporting Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getPatientsByDiagnosis_AsAdmin_ShouldReturnCorrectPatients_HappyPath() {
        Visit visit = Visit.builder().patient(testPatient).doctor(testDoctor).diagnosis(testDiagnosis).visitDate(LocalDate.now()).visitTime(LocalTime.now()).build();
        visitRepository.save(visit);
        Page<nbu.cscb869.data.dto.PatientDiagnosisDTO> result = diagnosisService.getPatientsByDiagnosis(testDiagnosis.getId(), 0, 10);
        assertEquals(1, result.getTotalElements());
        assertEquals(testPatient.getName(), result.getContent().get(0).getPatient().getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void getPatientsByDiagnosis_AsDoctor_ShouldBeDenied_ErrorCase() {
        assertThrows(AccessDeniedException.class, () -> diagnosisService.getPatientsByDiagnosis(testDiagnosis.getId(), 0, 10));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getMostFrequentDiagnoses_AsAdmin_ShouldReturnCorrectCount_HappyPath() {
        Visit visit1 = Visit.builder().patient(testPatient).doctor(testDoctor).diagnosis(testDiagnosis).visitDate(LocalDate.now()).visitTime(LocalTime.now()).build();
        visitRepository.save(visit1);

        Patient anotherPatient = new Patient();
        anotherPatient.setName("Another Patient");
        anotherPatient.setEgn(TestDataUtils.generateValidEgn());
        anotherPatient.setGeneralPractitioner(testDoctor);
        anotherPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patientRepository.save(anotherPatient);

        Visit visit2 = Visit.builder().patient(anotherPatient).doctor(testDoctor).diagnosis(testDiagnosis).visitDate(LocalDate.now()).visitTime(LocalTime.now()).build();
        visitRepository.save(visit2);

        List<nbu.cscb869.data.dto.DiagnosisVisitCountDTO> result = diagnosisService.getMostFrequentDiagnoses();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Flu", result.get(0).getDiagnosis().getName());
        assertEquals(2, result.get(0).getVisitCount());
    }
}
