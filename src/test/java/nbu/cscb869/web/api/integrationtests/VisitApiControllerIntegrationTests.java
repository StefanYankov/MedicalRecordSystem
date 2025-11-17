package nbu.cscb869.web.api.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.data.dtos.VisitCreateDTO;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(VisitApiControllerIntegrationTests.TestConfig.class)
@DisplayName("VisitApiController Integration Tests")
class VisitApiControllerIntegrationTests {

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @MockBean
    private Keycloak keycloak;

    private Doctor testDoctor;
    private Patient testPatient;
    private Diagnosis testDiagnosis;

    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setName("Dr. Test");
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        testPatient = new Patient();
        testPatient.setKeycloakId("patient-owner-id");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setName("Patient Owner");
        testPatient.setGeneralPractitioner(testDoctor);
        testPatient.setLastInsurancePaymentDate(LocalDate.now());
        patientRepository.save(testPatient);

        testDiagnosis = new Diagnosis();
        testDiagnosis.setName("Flu");
        diagnosisRepository.save(testDiagnosis);
    }

    @AfterEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void tearDown() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {
        @Test
        @DisplayName("createVisit_WithValidDataAsDoctor_ShouldCreateVisitInDb")
        void createVisit_WithValidDataAsDoctor_ShouldCreateVisitInDb() throws Exception {
            // ARRANGE
            VisitCreateDTO createDTO = new VisitCreateDTO();
            createDTO.setPatientId(testPatient.getId());
            createDTO.setDoctorId(testDoctor.getId());
            createDTO.setDiagnosisId(testDiagnosis.getId());
            createDTO.setVisitDate(LocalDate.now().plusDays(1));
            createDTO.setVisitTime(LocalTime.of(10, 0));

            long visitsBefore = visitRepository.count();

            // ACT
            mockMvc.perform(post("/api/visits")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isCreated());

            // ASSERT
            long visitsAfter = visitRepository.count();
            assertThat(visitsAfter).isEqualTo(visitsBefore + 1);
            Visit createdVisit = visitRepository.findAll().get(0);
            assertThat(createdVisit.getPatient().getId()).isEqualTo(testPatient.getId());
            assertThat(createdVisit.getDoctor().getId()).isEqualTo(testDoctor.getId());
        }

        @Test
        @DisplayName("getAllVisits_WhenSomeExist_ShouldReturnPageWithCorrectData")
        void getAllVisits_WhenSomeExist_ShouldReturnPageWithCorrectData() throws Exception {
            // ARRANGE
            Visit visit = new Visit();
            visit.setDoctor(testDoctor);
            visit.setPatient(testPatient);
            visit.setDiagnosis(testDiagnosis);
            visit.setVisitDate(LocalDate.now());
            visit.setVisitTime(LocalTime.of(11, 0));
            visit.setStatus(VisitStatus.COMPLETED);
            visitRepository.save(visit);

            // ACT & ASSERT
            mockMvc.perform(get("/api/visits")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(visit.getId()));
        }

        @Test
        @DisplayName("deleteVisit_WithExistingIdAsAdmin_ShouldRemoveFromDb")
        void deleteVisit_WithExistingIdAsAdmin_ShouldRemoveFromDb() throws Exception {
            // ARRANGE
            Visit visit = new Visit();
            visit.setDoctor(testDoctor);
            visit.setPatient(testPatient);
            visit.setVisitDate(LocalDate.now());
            visit.setVisitTime(LocalTime.of(12, 0));
            visit.setStatus(VisitStatus.SCHEDULED);
            Visit savedVisit = visitRepository.save(visit);

            long visitsBefore = visitRepository.count();

            // ACT
            mockMvc.perform(delete("/api/visits/" + savedVisit.getId())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            // ASSERT
            long visitsAfter = visitRepository.count();
            assertThat(visitsAfter).isEqualTo(visitsBefore - 1);
            assertThat(visitRepository.findById(savedVisit.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Error Case Tests")
    class ErrorCaseTests {
        @Test
        @DisplayName("createVisit_AsPatient_ShouldReturnForbidden")
        void createVisit_AsPatient_ShouldReturnForbidden() throws Exception {
            // ARRANGE
            VisitCreateDTO createDTO = new VisitCreateDTO();
            createDTO.setPatientId(testPatient.getId());
            createDTO.setDoctorId(testDoctor.getId());
            createDTO.setDiagnosisId(testDiagnosis.getId());
            createDTO.setVisitDate(LocalDate.now().plusDays(1));
            createDTO.setVisitTime(LocalTime.of(10, 0));

            // ACT & ASSERT
            mockMvc.perform(post("/api/visits")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PATIENT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deleteVisit_WithNonExistentId_ShouldReturnNotFound")
        void deleteVisit_WithNonExistentId_ShouldReturnNotFound() throws Exception {
            // ACT & ASSERT
            mockMvc.perform(delete("/api/visits/999")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("getVisitById_AsUnrelatedPatient_ShouldReturnForbidden")
        void getVisitById_AsUnrelatedPatient_ShouldReturnForbidden() throws Exception {
            // ARRANGE
            Visit visit = new Visit();
            visit.setDoctor(testDoctor);
            visit.setPatient(testPatient); // Belongs to "patient-owner-id"
            visit.setVisitDate(LocalDate.now());
            visit.setVisitTime(LocalTime.of(12, 0));
            visit.setStatus(VisitStatus.SCHEDULED);
            visitRepository.save(visit);

            // ACT & ASSERT
            mockMvc.perform(get("/api/visits/" + visit.getId())
                            .with(jwt().jwt(jwt -> jwt.subject("another-patient-id")).authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))))
                    .andExpect(status().isForbidden());
        }
    }
}
