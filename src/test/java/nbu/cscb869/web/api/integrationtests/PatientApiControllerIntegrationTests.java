package nbu.cscb869.web.api.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(PatientApiControllerIntegrationTests.TestConfig.class)
class PatientApiControllerIntegrationTests {

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
    private ObjectMapper objectMapper;

    private Doctor testDoctor;
    private Patient testPatient;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        doctorRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setName("Dr. Test");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        testPatient = new Patient();
        testPatient.setName("Test Patient");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setKeycloakId("patient-id");
        testPatient.setGeneralPractitioner(testDoctor);
        patientRepository.save(testPatient);
    }

    @Nested
    @DisplayName("GET /api/patients")
    class GetTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void getAllPatients_AsDoctor_ShouldReturnPatients_HappyPath() throws Exception {
            mockMvc.perform(get("/api/patients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
        void getAllPatients_AsPatient_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/patients"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "patient-id", authorities = "ROLE_PATIENT")
        void getPatientById_AsPatientOwner_ShouldReturnPatient_HappyPath() throws Exception {
            mockMvc.perform(get("/api/patients/{id}", testPatient.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testPatient.getId()));
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "other-patient-id", authorities = "ROLE_PATIENT")
        void getPatientById_AsOtherPatient_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/patients/{id}", testPatient.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/patients")
    class PostTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void createPatient_AsAdmin_ShouldReturnCreated_HappyPath() throws Exception {
            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("New Patient");
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(testDoctor.getId());
            createDTO.setKeycloakId(TestDataUtils.generateKeycloakId());

            mockMvc.perform(post("/api/patients").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void createPatient_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("New Patient");
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(testDoctor.getId());
            createDTO.setKeycloakId(TestDataUtils.generateKeycloakId());

            mockMvc.perform(post("/api/patients").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/patients/{id}")
    class DeleteTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void deletePatient_AsAdmin_ShouldReturnNoContent_HappyPath() throws Exception {
            mockMvc.perform(delete("/api/patients/{id}", testPatient.getId()).with(csrf()))
                    .andExpect(status().isNoContent());

            assertFalse(patientRepository.findById(testPatient.getId()).isPresent());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void deletePatient_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(delete("/api/patients/{id}", testPatient.getId()).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
