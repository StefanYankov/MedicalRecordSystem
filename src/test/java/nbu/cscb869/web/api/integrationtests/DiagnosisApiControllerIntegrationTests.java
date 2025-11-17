package nbu.cscb869.web.api.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(DiagnosisApiControllerIntegrationTests.TestConfig.class)
class DiagnosisApiControllerIntegrationTests {

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
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Diagnosis testDiagnosis;

    @BeforeEach
    void setUp() {
        diagnosisRepository.deleteAll();
        testDiagnosis = new Diagnosis();
        testDiagnosis.setName("Flu");
        testDiagnosis.setDescription("Seasonal influenza");
        diagnosisRepository.save(testDiagnosis);
    }

    @Nested
    @DisplayName("GET /api/diagnoses")
    class GetTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getAllDiagnoses_AsAdmin_ShouldReturnDiagnoses_HappyPath() throws Exception {
            mockMvc.perform(get("/api/diagnoses"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Flu"));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void getAllDiagnoses_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/diagnoses"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getDiagnosisById_AsAdmin_WithValidId_ShouldReturnDiagnosis_HappyPath() throws Exception {
            mockMvc.perform(get("/api/diagnoses/{id}", testDiagnosis.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testDiagnosis.getId()))
                    .andExpect(jsonPath("$.name").value("Flu"));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getDiagnosisById_AsAdmin_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/diagnoses/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/diagnoses")
    class PostTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void createDiagnosis_AsAdminWithValidData_ShouldReturnCreated_HappyPath() throws Exception {
            DiagnosisCreateDTO newDiagnosis = new DiagnosisCreateDTO();
            newDiagnosis.setName("Common Cold");
            newDiagnosis.setDescription("Mild viral infection");

            mockMvc.perform(post("/api/diagnoses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newDiagnosis)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Common Cold"));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void createDiagnosis_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            DiagnosisCreateDTO newDiagnosis = new DiagnosisCreateDTO();
            newDiagnosis.setName("Unauthorized Diagnosis");

            mockMvc.perform(post("/api/diagnoses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newDiagnosis)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void createDiagnosis_WithInvalidData_ShouldReturnBadRequest_ErrorCase() throws Exception {
            DiagnosisCreateDTO newDiagnosis = new DiagnosisCreateDTO(); // Missing required name

            mockMvc.perform(post("/api/diagnoses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newDiagnosis)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/diagnoses/{id}")
    class PatchTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void updateDiagnosis_AsAdminWithValidData_ShouldReturnOk_HappyPath() throws Exception {
            DiagnosisCreateDTO update = new DiagnosisCreateDTO();
            update.setName("Seasonal Flu Updated");

            mockMvc.perform(patch("/api/diagnoses/{id}", testDiagnosis.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Seasonal Flu Updated"));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void updateDiagnosis_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            DiagnosisCreateDTO update = new DiagnosisCreateDTO();
            update.setName("Unauthorized Update");

            mockMvc.perform(patch("/api/diagnoses/{id}", testDiagnosis.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void updateDiagnosis_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            DiagnosisCreateDTO update = new DiagnosisCreateDTO();
            update.setName("Non Existent");

            mockMvc.perform(patch("/api/diagnoses/9999")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/diagnoses/{id}")
    class DeleteTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void deleteDiagnosis_AsAdmin_ShouldReturnNoContent_HappyPath() throws Exception {
            mockMvc.perform(delete("/api/diagnoses/{id}", testDiagnosis.getId()).with(csrf()))
                    .andExpect(status().isNoContent());

            assertFalse(diagnosisRepository.findById(testDiagnosis.getId()).isPresent());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void deleteDiagnosis_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(delete("/api/diagnoses/{id}", testDiagnosis.getId()).with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void deleteDiagnosis_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(delete("/api/diagnoses/9999").with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
