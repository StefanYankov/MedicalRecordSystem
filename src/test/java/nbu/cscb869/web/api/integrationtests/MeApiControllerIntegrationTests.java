package nbu.cscb869.web.api.integrationtests;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(MeApiControllerIntegrationTests.TestConfig.class)
class MeApiControllerIntegrationTests {

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    private Doctor testDoctor;
    private Patient testPatient;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setKeycloakId("doctor-id");
        testDoctor.setName("Dr. Me");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        testPatient = new Patient();
        testPatient.setKeycloakId("patient-id");
        testPatient.setName("Mr. Me");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setGeneralPractitioner(testDoctor);
        patientRepository.save(testPatient);
    }

    @Nested
    @DisplayName("GET /api/me/dashboard")
    class GetDashboardTests {
        @Test
        @WithMockKeycloakUser(keycloakId = "doctor-id", authorities = "ROLE_DOCTOR")
        void getMyDashboard_AsDoctor_ShouldReturnDoctorVisits_HappyPath() throws Exception {
            Visit visit = new Visit();
            visit.setDoctor(testDoctor);
            visit.setPatient(testPatient);
            visit.setStatus(VisitStatus.SCHEDULED);
            visit.setVisitDate(LocalDate.now().plusDays(1));
            visit.setVisitTime(LocalTime.NOON);
            visitRepository.save(visit);

            mockMvc.perform(get("/api/me/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "patient-id", authorities = "ROLE_PATIENT")
        void getMyDashboard_AsPatient_ShouldReturnPatientProfile_HappyPath() throws Exception {
            mockMvc.perform(get("/api/me/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Mr. Me"))
                    .andExpect(jsonPath("$.id").value(testPatient.getId()));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getMyDashboard_AsAdmin_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/me/dashboard"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/me/history")
    class GetHistoryTests {
        @Test
        @WithMockKeycloakUser(keycloakId = "patient-id", authorities = "ROLE_PATIENT")
        void getMyHistory_AsPatient_ShouldReturnPatientHistory_HappyPath() throws Exception {
            Visit visit = new Visit();
            visit.setDoctor(testDoctor);
            visit.setPatient(testPatient);
            visit.setStatus(VisitStatus.COMPLETED);
            visit.setVisitDate(LocalDate.now().minusDays(1));
            visit.setVisitTime(LocalTime.NOON);
            visitRepository.save(visit);

            mockMvc.perform(get("/api/me/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(visit.getId()));
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "doctor-id", authorities = "ROLE_DOCTOR")
        void getMyHistory_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/me/history"))
                    .andExpect(status().isForbidden());
        }
    }
}
