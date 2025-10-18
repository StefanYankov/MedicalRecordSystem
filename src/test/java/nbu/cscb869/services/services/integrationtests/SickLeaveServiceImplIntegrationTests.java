package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import nbu.cscb869.services.services.contracts.SickLeaveService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(SickLeaveServiceImplIntegrationTests.AsyncTestConfig.class)
class SickLeaveServiceImplIntegrationTests {

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private SickLeaveService sickLeaveService;

    @Autowired
    private SickLeaveRepository sickLeaveRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    private Visit testVisit;
    private Patient patientOwner;

    @BeforeEach
    void setUp() {
        sickLeaveRepository.deleteAll();
        visitRepository.deleteAll();
        diagnosisRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();

        Doctor doctor = doctorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Doctor newDoctor = new Doctor();
            newDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
            newDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            newDoctor.setName("Dr. Test");
            newDoctor.setGeneralPractitioner(true);
            return doctorRepository.save(newDoctor);
        });

        patientOwner = patientRepository.findByKeycloakId("patient-owner-id").orElseGet(() -> {
            Patient newPatient = new Patient();
            newPatient.setKeycloakId("patient-owner-id");
            newPatient.setEgn(TestDataUtils.generateValidEgn());
            newPatient.setName("Patient Owner");
            newPatient.setGeneralPractitioner(doctor);
            return patientRepository.save(newPatient);
        });

        Diagnosis diagnosis = diagnosisRepository.findByName("Flu").orElseGet(() -> {
            Diagnosis newDiagnosis = new Diagnosis();
            newDiagnosis.setName("Flu");
            return diagnosisRepository.save(newDiagnosis);
        });

        testVisit = new Visit();
        testVisit.setDoctor(doctor);
        testVisit.setPatient(patientOwner);
        testVisit.setDiagnosis(diagnosis);
        testVisit.setVisitDate(LocalDate.now());
        testVisit.setVisitTime(LocalTime.now());
        visitRepository.save(testVisit);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void create_AsDoctorWithValidData_ShouldPersistSickLeave_HappyPath() {
        // ARRANGE
        SickLeaveCreateDTO createDTO = new SickLeaveCreateDTO();
        createDTO.setVisitId(testVisit.getId());
        createDTO.setStartDate(LocalDate.now());
        createDTO.setDurationDays(5);

        // ACT
        SickLeaveViewDTO result = sickLeaveService.create(createDTO);

        // ASSERT
        assertNotNull(result);
        assertNotNull(result.getId());
        assertTrue(sickLeaveRepository.findById(result.getId()).isPresent());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void update_AsDoctorWithValidData_ShouldUpdateSickLeave_HappyPath() {
        // ARRANGE
        SickLeave sickLeave = new SickLeave();
        sickLeave.setVisit(testVisit);
        sickLeave.setStartDate(LocalDate.now().minusDays(10));
        sickLeave.setDurationDays(3);
        sickLeave = sickLeaveRepository.save(sickLeave);

        SickLeaveUpdateDTO updateDTO = new SickLeaveUpdateDTO();
        updateDTO.setId(sickLeave.getId());
        updateDTO.setVisitId(testVisit.getId());
        updateDTO.setStartDate(LocalDate.now());
        updateDTO.setDurationDays(7);

        // ACT
        sickLeaveService.update(updateDTO);

        // ASSERT
        SickLeave updatedSickLeave = sickLeaveRepository.findById(sickLeave.getId()).get();
        assertEquals(7, updatedSickLeave.getDurationDays());
        assertEquals(LocalDate.now(), updatedSickLeave.getStartDate());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void delete_AsDoctor_ShouldDeleteSickLeave_HappyPath() {
        // ARRANGE
        SickLeave sickLeave = new SickLeave();
        sickLeave.setVisit(testVisit);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave = sickLeaveRepository.save(sickLeave);
        long sickLeaveId = sickLeave.getId();

        // ACT
        sickLeaveService.delete(sickLeaveId);

        // ASSERT
        assertFalse(sickLeaveRepository.findById(sickLeaveId).isPresent());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
    void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
        // ARRANGE
        SickLeave sickLeave = new SickLeave();
        sickLeave.setVisit(testVisit);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave = sickLeaveRepository.save(sickLeave);

        // ACT
        SickLeaveViewDTO result = sickLeaveService.getById(sickLeave.getId());

        // ASSERT
        assertNotNull(result);
        assertEquals(sickLeave.getId(), result.getId());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "other-patient-id", authorities = "ROLE_PATIENT")
    void getById_AsOtherPatient_ShouldBeDenied_ErrorCase() {
        // ARRANGE
        SickLeave sickLeave = new SickLeave();
        sickLeave.setVisit(testVisit);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave = sickLeaveRepository.save(sickLeave);
        long sickLeaveId = sickLeave.getId();

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> sickLeaveService.getById(sickLeaveId));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getAll_AsAdmin_ShouldReturnPage_HappyPath() throws ExecutionException, InterruptedException {
        // ARRANGE
        SickLeave sickLeave = new SickLeave();
        sickLeave.setVisit(testVisit);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeaveRepository.save(sickLeave);

        // Commit transaction to make data visible to the async thread
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // ACT
        CompletableFuture<Page<SickLeaveViewDTO>> future = sickLeaveService.getAll(0, 10, "startDate", true);
        Page<SickLeaveViewDTO> result = future.get();

        // ASSERT
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getMonthsWithMostSickLeaves_AsAdmin_ShouldReturnCorrectMonth_HappyPath() {
        // ARRANGE
        SickLeave sickLeave = new SickLeave();
        sickLeave.setVisit(testVisit);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeaveRepository.save(sickLeave);

        // ACT
        var result = sickLeaveService.getMonthsWithMostSickLeaves();

        // ASSERT
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(LocalDate.now().getYear(), result.get(0).getYear());
        assertEquals(LocalDate.now().getMonthValue(), result.get(0).getMonth());
        assertEquals(1, result.get(0).getCount());
    }
}
