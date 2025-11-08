package nbu.cscb869.web.controllers.admin.integrationtests;

import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.DiagnosisRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(AdminDiagnosisControllerIntegrationTests.TestConfig.class)
class AdminDiagnosisControllerIntegrationTests {

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

    private Diagnosis testDiagnosis;

    @BeforeEach
    void setUp() {
        diagnosisRepository.deleteAll();
        testDiagnosis = new Diagnosis();
        testDiagnosis.setName("Common Cold");
        testDiagnosis.setDescription("Viral infection");
        diagnosisRepository.save(testDiagnosis);
    }

    @Nested
    @DisplayName("List Diagnoses")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class ListDiagnosesTests {
        @Test
        void listDiagnoses_ShouldReturnListViewWithDiagnoses_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/diagnoses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/list"))
                    .andExpect(content().string(containsString("Common Cold")));
        }
    }

    @Nested
    @DisplayName("Create Diagnosis")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class CreateDiagnosisTests {
        @Test
        void showCreateDiagnosisForm_ShouldReturnCreateView_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/diagnoses/create"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/create"));
        }

        @Test
        void createDiagnosis_WithValidData_ShouldCreateDiagnosisAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/diagnoses/create")
                            .param("name", "Influenza")
                            .param("description", "A serious viral infection")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses"))
                    .andExpect(flash().attribute("successMessage", "Diagnosis created successfully."));

            assertTrue(diagnosisRepository.findByName("Influenza").isPresent());
        }

        @Test
        void createDiagnosis_WithInvalidData_ShouldRedirectBackToFormAndShowErrors_ErrorCase() throws Exception {
            mockMvc.perform(post("/admin/diagnoses/create").param("name", "").with(csrf())) // Empty name is invalid
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses/create"))
                    .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.diagnosis"));
        }

        @Test
        void createDiagnosis_WithDuplicateName_ShouldRedirectBackToFormAndShowError_ErrorCase() throws Exception {
            mockMvc.perform(post("/admin/diagnoses/create")
                            .param("name", "Common Cold") // Duplicate name
                            .param("description", "Another description")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses/create"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Edit Diagnosis")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class EditDiagnosisTests {
        @Test
        void showEditDiagnosisForm_ShouldDisplayCorrectDiagnosis_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/diagnoses/edit/{id}", testDiagnosis.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/edit"))
                    .andExpect(model().attributeExists("diagnosis"))
                    .andExpect(content().string(containsString("Common Cold")));
        }

        @Test
        void editDiagnosis_WithValidData_ShouldUpdateDiagnosisAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/diagnoses/edit/{id}", testDiagnosis.getId())
                            .param("name", "Updated Cold")
                            .param("description", "Updated description")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses"))
                    .andExpect(flash().attribute("successMessage", "Diagnosis updated successfully."));

            Diagnosis updatedDiagnosis = diagnosisRepository.findById(testDiagnosis.getId()).orElseThrow();
            assertTrue(updatedDiagnosis.getName().equals("Updated Cold"));
        }

        @Test
        void showEditDiagnosisForm_ForNonExistentDiagnosis_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/diagnoses/edit/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Delete Diagnosis")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class DeleteDiagnosisTests {
        @Test
        void showDeleteConfirmation_ShouldDisplayConfirmationPage_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/diagnoses/delete/{id}", testDiagnosis.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/delete-confirm"))
                    .andExpect(content().string(containsString("Are you sure you want to delete this diagnosis?")))
                    .andExpect(content().string(containsString("Common Cold")));
        }

        @Test
        void deleteDiagnosisConfirmed_ShouldDeleteDiagnosisAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/diagnoses/delete/{id}", testDiagnosis.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses"))
                    .andExpect(flash().attribute("successMessage", "Diagnosis deleted successfully."));

            assertFalse(diagnosisRepository.findById(testDiagnosis.getId()).isPresent());
        }

        @Test
        void showDeleteConfirmation_ForNonExistentDiagnosis_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/diagnoses/delete/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void listDiagnoses_AsDoctor_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/admin/diagnoses"))
                    .andExpect(status().isForbidden());
        }
    }
}
