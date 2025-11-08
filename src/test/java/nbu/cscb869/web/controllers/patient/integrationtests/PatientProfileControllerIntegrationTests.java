package nbu.cscb869.web.controllers.patient.integrationtests;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
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

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(PatientProfileControllerIntegrationTests.TestConfig.class)
class PatientProfileControllerIntegrationTests {

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
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private Patient testPatient;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        doctorRepository.deleteAll();

        Doctor gp = new Doctor();
        gp.setName("Dr. House");
        gp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        gp.setKeycloakId(TestDataUtils.generateKeycloakId());
        gp.setGeneralPractitioner(true);
        doctorRepository.save(gp);

        testPatient = new Patient();
        testPatient.setKeycloakId("test-patient-id"); // This ID must match the one in @WithMockKeycloakUser
        testPatient.setName("John Doe");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setLastInsurancePaymentDate(LocalDate.now());
        testPatient.setGeneralPractitioner(gp);
        patientRepository.save(testPatient);
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "test-patient-id", authorities = "ROLE_PATIENT")
    void GetProfile_AsAuthenticatedPatient_ShouldReturnOkAndContainProfileData_HappyPath() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/profile"))
                .andExpect(model().attributeExists("patient"))
                .andExpect(model().attribute("patient", hasProperty("name", is("John Doe"))))
                .andExpect(model().attribute("patient", hasProperty("egn", is(testPatient.getEgn()))));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void GetProfile_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void GetProfile_AsUnauthenticatedUser_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/profile").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
    }
}
