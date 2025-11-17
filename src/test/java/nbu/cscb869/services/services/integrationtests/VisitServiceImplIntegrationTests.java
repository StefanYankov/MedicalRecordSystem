package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.common.exceptions.PatientInsuranceException;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.contracts.VisitService;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@Import({VisitServiceImplIntegrationTests.AsyncTestConfig.class, VisitServiceImplIntegrationTests.TestConfig.class})
class VisitServiceImplIntegrationTests {

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

    @MockBean
    private Keycloak keycloak;

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private VisitService visitService;

    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DiagnosisRepository diagnosisRepository;
    @Autowired
    private SickLeaveRepository sickLeaveRepository;

    private Doctor testDoctor;
    private Patient testPatient;
    private Diagnosis testDiagnosis;

    @BeforeEach
    void setUp() {
        testDoctor = doctorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Doctor d = new Doctor();
            d.setKeycloakId(TestDataUtils.generateKeycloakId());
            d.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            d.setName("Dr. Test");
            d.setGeneralPractitioner(true);
            return doctorRepository.save(d);
        });

        testPatient = patientRepository.findByKeycloakId("patient-owner-id").orElseGet(() -> {
            Patient p = new Patient();
            p.setKeycloakId("patient-owner-id");
            p.setEgn(TestDataUtils.generateValidEgn());
            p.setName("Patient Owner");
            p.setGeneralPractitioner(testDoctor);
            p.setLastInsurancePaymentDate(LocalDate.now());
            return patientRepository.save(p);
        });

        testDiagnosis = diagnosisRepository.findByName("Flu").orElseGet(() -> {
            Diagnosis d = new Diagnosis();
            d.setName("Flu");
            return diagnosisRepository.save(d);
        });

        visitRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
        void create_AsPatient_ShouldBeDenied_ErrorCase() {
            VisitCreateDTO createDTO = new VisitCreateDTO();
            assertThrows(AccessDeniedException.class, () -> visitService.create(createDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void create_WithInvalidInsurance_ShouldThrowPatientInsuranceException_ErrorCase() {
            testPatient.setLastInsurancePaymentDate(LocalDate.now().minusMonths(7));
            patientRepository.save(testPatient);

            VisitCreateDTO createDTO = new VisitCreateDTO();
            createDTO.setPatientId(testPatient.getId());
            createDTO.setDoctorId(testDoctor.getId());
            createDTO.setDiagnosisId(testDiagnosis.getId());
            createDTO.setVisitDate(LocalDate.now());
            createDTO.setVisitTime(LocalTime.of(11, 0));

            assertThrows(PatientInsuranceException.class, () -> visitService.create(createDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void create_WithBookedTimeSlot_ShouldThrowInvalidInputException_ErrorCase() {
            testPatient.setLastInsurancePaymentDate(LocalDate.now());
            patientRepository.save(testPatient);

            Visit existingVisit = Visit.builder()
                    .visitDate(LocalDate.now())
                    .visitTime(LocalTime.of(14, 0))
                    .patient(testPatient)
                    .doctor(testDoctor)
                    .diagnosis(testDiagnosis)
                    .status(VisitStatus.SCHEDULED)
                    .build();
            visitRepository.save(existingVisit);

            VisitCreateDTO createDTO = new VisitCreateDTO();
            createDTO.setPatientId(testPatient.getId());
            createDTO.setDoctorId(testDoctor.getId());
            createDTO.setDiagnosisId(testDiagnosis.getId());
            createDTO.setVisitDate(LocalDate.now());
            createDTO.setVisitTime(LocalTime.of(14, 0));

            assertThrows(InvalidInputException.class, () -> visitService.create(createDTO));
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void update_WithValidData_ShouldUpdateAggregate_HappyPath() {
            Visit visit = visitRepository.save(Visit.builder().visitDate(LocalDate.now()).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).diagnosis(testDiagnosis).status(VisitStatus.COMPLETED).build());
            VisitUpdateDTO updateDTO = VisitUpdateDTO.builder()
                    .id(visit.getId())
                    .visitDate(LocalDate.now())
                    .visitTime(LocalTime.of(12, 0))
                    .patientId(testPatient.getId())
                    .doctorId(testDoctor.getId())
                    .diagnosisId(testDiagnosis.getId())
                    .status(VisitStatus.COMPLETED)
                    .build();

            visitService.update(updateDTO);

            Visit updatedVisit = visitRepository.findById(visit.getId()).get();
            assertEquals(LocalTime.of(12, 0), updatedVisit.getVisitTime());
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void delete_AsDoctor_ShouldDeleteAggregate_HappyPath() {
            Visit visit = visitRepository.save(Visit.builder().visitDate(LocalDate.now()).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).diagnosis(testDiagnosis).status(VisitStatus.COMPLETED).build());
            long visitId = visit.getId();

            visitService.delete(visitId);

            assertFalse(visitRepository.findById(visitId).isPresent());
        }
    }

    @Nested
    @DisplayName("GetById Tests")
    class GetByIdTests {
        @Test
        @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
        void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
            Visit visit = visitRepository.save(Visit.builder().visitDate(LocalDate.now()).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).diagnosis(testDiagnosis).status(VisitStatus.COMPLETED).build());

            VisitViewDTO result = visitService.getById(visit.getId());

            assertNotNull(result);
            assertEquals(visit.getId(), result.getId());
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "other-patient-id", authorities = "ROLE_PATIENT")
        void getById_AsOtherPatient_ShouldBeDenied_ErrorCase() {
            Visit visit = visitRepository.save(Visit.builder().visitDate(LocalDate.now()).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).diagnosis(testDiagnosis).status(VisitStatus.COMPLETED).build());
            long visitId = visit.getId();

            assertThrows(AccessDeniedException.class, () -> visitService.getById(visitId));
        }
    }

    @Nested
    @DisplayName("GetAll Tests")
    class GetAllTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getAll_AsAdmin_ShouldReturnPage_HappyPath() throws ExecutionException, InterruptedException {
            visitRepository.save(Visit.builder().visitDate(LocalDate.now()).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).diagnosis(testDiagnosis).status(VisitStatus.COMPLETED).build());

            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();

            CompletableFuture<Page<VisitViewDTO>> future = visitService.getAll(0, 10, "visitDate", true, null);
            Page<VisitViewDTO> result = future.get();

            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("Reporting Tests")
    class ReportingTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getVisitCountByDoctor_ShouldReturnCorrectCount_HappyPath() {
            visitRepository.save(Visit.builder().visitDate(LocalDate.now()).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).diagnosis(testDiagnosis).status(VisitStatus.COMPLETED).build());

            var result = visitService.getVisitCountByDoctor();

            assertFalse(result.isEmpty());
            assertEquals(1, result.getFirst().getVisitCount());
            assertEquals(testDoctor.getName(), result.getFirst().getDoctor().getName());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getMostFrequentDiagnoses_ShouldReturnCorrectCount_HappyPath() {
            visitRepository.save(Visit.builder().visitDate(LocalDate.now()).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).diagnosis(testDiagnosis).status(VisitStatus.COMPLETED).build());

            var result = visitService.getMostFrequentDiagnoses();

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(testDiagnosis.getName(), result.getFirst().getDiagnosisName());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void getVisitsByDoctorAndStatusAndDateRange_ShouldReturnCorrectVisits_HappyPath() {
            Visit scheduledVisit = Visit.builder()
                    .doctor(testDoctor)
                    .patient(testPatient)
                    .status(VisitStatus.SCHEDULED)
                    .visitDate(LocalDate.now().plusDays(1))
                    .visitTime(LocalTime.NOON)
                    .build();

            Visit completedVisit = Visit.builder()
                    .doctor(testDoctor)
                    .patient(testPatient)
                    .status(VisitStatus.COMPLETED)
                    .visitDate(LocalDate.now().plusDays(2))
                    .visitTime(LocalTime.NOON)
                    .build();

            visitRepository.saveAll(List.of(scheduledVisit, completedVisit));

            Page<VisitViewDTO> result = visitService.getVisitsByDoctorAndStatusAndDateRange(
                    testDoctor.getId(),
                    VisitStatus.SCHEDULED,
                    LocalDate.now(),
                    LocalDate.now().plusDays(3),
                    0, 10);

            assertEquals(1, result.getTotalElements());
            assertEquals(VisitStatus.SCHEDULED, result.getContent().getFirst().getStatus());
        }
    }

    @Nested
    @DisplayName("Additional Service Flow Tests")
    class AdditionalServiceFlowsTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
        void scheduleNewVisitByPatient_WithValidInsurance_ShouldCreateVisit_HappyPath() {
            // ARRANGE
            VisitCreateDTO dto = new VisitCreateDTO();
            dto.setPatientId(testPatient.getId());
            dto.setDoctorId(testDoctor.getId());
            dto.setVisitDate(LocalDate.now().plusDays(2));
            dto.setVisitTime(LocalTime.of(11, 30));

            // ACT
            visitService.scheduleNewVisitByPatient(dto);

            // ASSERT
            List<Visit> visits = visitRepository.findAll();
            assertThat(visits).hasSize(1);
            assertThat(visits.get(0).getStatus()).isEqualTo(VisitStatus.SCHEDULED);
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void documentVisit_WithValidData_ShouldUpdateAndCompleteVisit_HappyPath() {
            // ARRANGE
            Visit visit = visitRepository.save(Visit.builder().visitDate(LocalDate.now()).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).status(VisitStatus.SCHEDULED).build());
            VisitDocumentationDTO dto = new VisitDocumentationDTO();
            dto.setDiagnosisId(testDiagnosis.getId());
            dto.setNotes("Integration test notes.");

            // ACT
            visitService.documentVisit(visit.getId(), dto);

            // ASSERT
            Visit updatedVisit = visitRepository.findById(visit.getId()).get();
            assertThat(updatedVisit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
            assertThat(updatedVisit.getNotes()).isEqualTo("Integration test notes.");
            assertThat(updatedVisit.getDiagnosis().getId()).isEqualTo(testDiagnosis.getId());
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
        void cancelVisit_ByPatientOwner_ShouldUpdateStatusToCancelled_HappyPath() {
            // ARRANGE
            Visit visit = visitRepository.save(Visit.builder().visitDate(LocalDate.now().plusDays(5)).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).status(VisitStatus.SCHEDULED).build());

            // ACT
            visitService.cancelVisit(visit.getId());

            // ASSERT
            Visit cancelledVisit = visitRepository.findById(visit.getId()).get();
            assertThat(cancelledVisit.getStatus()).isEqualTo(VisitStatus.CANCELLED_BY_PATIENT);
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getMostFrequentSickLeaveMonth_WhenDataExists_ShouldReturnCorrectMonth_HappyPath() {
            // ARRANGE
            Visit visit = visitRepository.save(Visit.builder().visitDate(LocalDate.now()).visitTime(LocalTime.now()).patient(testPatient).doctor(testDoctor).status(VisitStatus.COMPLETED).build());
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(visit);
            sickLeave.setStartDate(LocalDate.of(2025, 10, 15)); // October
            sickLeave.setDurationDays(5);
            sickLeaveRepository.save(sickLeave);

            // ACT
            var result = visitService.getMostFrequentSickLeaveMonth();

            // ASSERT
            assertFalse(result.isEmpty());
            assertEquals(10, result.getFirst().getMonth()); // Check for October
            assertEquals(1, result.getFirst().getSickLeaveCount());
        }
    }
}
