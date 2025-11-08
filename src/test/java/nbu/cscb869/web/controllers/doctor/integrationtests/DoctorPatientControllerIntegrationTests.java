package nbu.cscb869.web.controllers.doctor.integrationtests;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
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

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(DoctorPatientControllerIntegrationTests.TestConfig.class)
class DoctorPatientControllerIntegrationTests {

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

    private Doctor testDoctor;
    private Patient patient1;
    private Patient patient2;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        doctorRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setName("Dr. Test");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId("doctor-keycloak-id");
        testDoctor.setApproved(true);
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        patient1 = new Patient();
        patient1.setName("Patient One");
        patient1.setEgn(TestDataUtils.generateValidEgn());
        patient1.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient1.setGeneralPractitioner(testDoctor);
        patientRepository.save(patient1);

        patient2 = new Patient();
        patient2.setName("Patient Two");
        patient2.setEgn(TestDataUtils.generateValidEgn());
        patient2.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient2.setGeneralPractitioner(testDoctor);
        patientRepository.save(patient2);
    }

    @Nested
    @DisplayName("Happy Path Tests")
    @WithMockKeycloakUser(keycloakId = "doctor-keycloak-id", authorities = "ROLE_DOCTOR")
    class HappyPathTests {

        @Test
        void listPatients_AsDoctor_ShouldReturnAllPatients_HappyPath() throws Exception {
            mockMvc.perform(get("/doctor/patients"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/patients/list"))
                    .andExpect(model().attributeExists("patientPage"))
                    .andExpect(model().attribute("patientPage", hasProperty("totalElements", is(2L))));
        }

        @Test
        void showPatientHistory_AsDoctorWithNoDirectRelation_ShouldSucceed_HappyPath() throws Exception {
            mockMvc.perform(get("/doctor/patients/{id}/history", patient2.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/patients/history"))
                    .andExpect(model().attributeExists("patient"));
        }
    }

    @Nested
    @DisplayName("Error Case Tests")
    class ErrorCaseTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
        void listPatients_AsPatient_ShouldBeForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/doctor/patients"))
                    .andExpect(status().isForbidden());
        }
    }
}
