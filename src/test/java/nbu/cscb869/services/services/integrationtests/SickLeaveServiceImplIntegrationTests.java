package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import nbu.cscb869.services.services.contracts.SickLeaveService;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
@Import({SickLeaveServiceImplIntegrationTests.AsyncTestConfig.class, SickLeaveServiceImplIntegrationTests.TestConfig.class})
class SickLeaveServiceImplIntegrationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ClientRegistrationRepository clientRegistrationRepository() {
            return Mockito.mock(ClientRegistrationRepository.class);
        }

        @Bean
        @Primary
        public JwtDecoder jwtDecoder() {
            return Mockito.mock(JwtDecoder.class);
        }
    }

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @MockBean
    private Keycloak keycloak;

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
        testVisit.setStatus(VisitStatus.COMPLETED);
        visitRepository.save(testVisit);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void create_AsDoctorWithValidData_ShouldPersistSickLeave_HappyPath() {
            SickLeaveCreateDTO createDTO = new SickLeaveCreateDTO();
            createDTO.setVisitId(testVisit.getId());
            createDTO.setStartDate(LocalDate.now());
            createDTO.setDurationDays(5);

            SickLeaveViewDTO result = sickLeaveService.create(createDTO);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertTrue(sickLeaveRepository.findById(result.getId()).isPresent());
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void update_AsDoctorWithValidData_ShouldUpdateSickLeave_HappyPath() {
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

            sickLeaveService.update(updateDTO);

            SickLeave updatedSickLeave = sickLeaveRepository.findById(sickLeave.getId()).get();
            assertEquals(7, updatedSickLeave.getDurationDays());
            assertEquals(LocalDate.now(), updatedSickLeave.getStartDate());
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void delete_AsDoctor_ShouldDeleteSickLeave_HappyPath() {
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(testVisit);
            sickLeave.setStartDate(LocalDate.now());
            sickLeave.setDurationDays(5);
            sickLeave = sickLeaveRepository.save(sickLeave);
            long sickLeaveId = sickLeave.getId();

            sickLeaveService.delete(sickLeaveId);

            assertFalse(sickLeaveRepository.findById(sickLeaveId).isPresent());
            Visit visit = visitRepository.findById(testVisit.getId()).get();
            assertNull(visit.getSickLeave());
        }
    }

    @Nested
    @DisplayName("GetById Tests")
    class GetByIdTests {
        @Test
        @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
        void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(testVisit);
            sickLeave.setStartDate(LocalDate.now());
            sickLeave.setDurationDays(5);
            sickLeave = sickLeaveRepository.save(sickLeave);

            SickLeaveViewDTO result = sickLeaveService.getById(sickLeave.getId());

            assertNotNull(result);
            assertEquals(sickLeave.getId(), result.getId());
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "other-patient-id", authorities = "ROLE_PATIENT")
        void getById_AsOtherPatient_ShouldBeDenied_ErrorCase() {
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(testVisit);
            sickLeave.setStartDate(LocalDate.now());
            sickLeave.setDurationDays(5);
            sickLeave = sickLeaveRepository.save(sickLeave);
            long sickLeaveId = sickLeave.getId();

            assertThrows(AccessDeniedException.class, () -> sickLeaveService.getById(sickLeaveId));
        }
    }

    @Nested
    @DisplayName("GetAll Tests")
    class GetAllTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getAll_AsAdmin_ShouldReturnPage_HappyPath() throws ExecutionException, InterruptedException {
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(testVisit);
            sickLeave.setStartDate(LocalDate.now());
            sickLeave.setDurationDays(5);
            sickLeaveRepository.save(sickLeave);

            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();

            CompletableFuture<Page<SickLeaveViewDTO>> future = sickLeaveService.getAll(0, 10, "startDate", true);
            Page<SickLeaveViewDTO> result = future.get();

            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("Reporting Tests")
    class ReportingTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getMonthsWithMostSickLeaves_AsAdmin_ShouldReturnCorrectMonth_HappyPath() {
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(testVisit);
            sickLeave.setStartDate(LocalDate.now());
            sickLeave.setDurationDays(5);
            sickLeaveRepository.save(sickLeave);

            var result = sickLeaveService.getMonthsWithMostSickLeaves();

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(LocalDate.now().getYear(), result.get(0).getYear());
            assertEquals(LocalDate.now().getMonthValue(), result.get(0).getMonth());
            assertEquals(1, result.get(0).getCount());
        }
    }
}
