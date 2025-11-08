package nbu.cscb869.web.controllers.doctor.integrationtests;

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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(DoctorDashboardControllerIntegrationTests.TestConfig.class)
class DoctorDashboardControllerIntegrationTests {

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

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setKeycloakId("doctor-id");
        testDoctor.setName("Dr. Dashboard");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {
        @Test
        @WithMockKeycloakUser(keycloakId = "doctor-id", authorities = "ROLE_DOCTOR")
        void doctorDashboard_AsDoctorWithScheduledVisits_ShouldReturnDashboardWithData_HappyPath() throws Exception {
            // ARRANGE
            Patient patient = new Patient();
            patient.setKeycloakId(TestDataUtils.generateKeycloakId());
            patient.setName("Test Patient");
            patient.setEgn(TestDataUtils.generateValidEgn());
            patient.setGeneralPractitioner(testDoctor);
            patientRepository.save(patient);

            Visit scheduledVisit = new Visit();
            scheduledVisit.setDoctor(testDoctor);
            scheduledVisit.setPatient(patient);
            scheduledVisit.setStatus(VisitStatus.SCHEDULED);
            scheduledVisit.setVisitDate(LocalDate.now().plusDays(1));
            scheduledVisit.setVisitTime(LocalTime.of(10, 0));
            visitRepository.save(scheduledVisit);

            // ACT & ASSERT
            mockMvc.perform(get("/doctor/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/dashboard"))
                    .andExpect(model().attributeExists("doctorName", "visits"))
                    .andExpect(model().attribute("doctorName", "Dr. Dashboard"))
                    .andExpect(model().attribute("visits", hasProperty("content", hasSize(1))));
        }
    }

    @Nested
    @DisplayName("Security Error Case Tests")
    class SecurityErrorCaseTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
        void doctorDashboard_AsPatient_ShouldBeForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/doctor/dashboard"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void doctorDashboard_AsUnauthenticatedUser_ShouldRedirectToLogin_ErrorCase() throws Exception {
            mockMvc.perform(get("/doctor/dashboard").accept(MediaType.TEXT_HTML))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
        }
    }

    @Nested
    @DisplayName("Data Edge Case Tests")
    class DataEdgeCaseTests {
        @Test
        @WithMockKeycloakUser(keycloakId = "doctor-id", authorities = "ROLE_DOCTOR")
        void doctorDashboard_WhenDoctorHasNoVisits_ShouldReturnEmptyPage_EdgeCase() throws Exception {
            // ARRANGE: No visits are created for the testDoctor.

            // ACT & ASSERT
            mockMvc.perform(get("/doctor/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/dashboard"))
                    .andExpect(model().attribute("visits", hasProperty("content", hasSize(0))));
        }
    }
}
