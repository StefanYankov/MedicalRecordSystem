package nbu.cscb869.web.controllers.admin.integrationtests;

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

import java.time.LocalDate;

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
@Import(AdminPatientControllerIntegrationTests.TestConfig.class)
class AdminPatientControllerIntegrationTests {

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
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private Patient testPatient;
    private Doctor testDoctor;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        doctorRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setName("Dr. Patient GP");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        testPatient = new Patient();
        testPatient.setName("Patient One");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
        testPatient.setGeneralPractitioner(testDoctor);
        testPatient.setLastInsurancePaymentDate(LocalDate.now());
        patientRepository.save(testPatient);
    }

    @Nested
    @DisplayName("List Patients")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class ListPatientsTests {
        @Test
        void listPatients_ShouldReturnListViewWithPatients_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/patients"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/patients/list"))
                    .andExpect(content().string(containsString("Patient One")));
        }
    }

    @Nested
    @DisplayName("Edit Patient")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class EditPatientTests {
        @Test
        void showEditPatientForm_ShouldDisplayCorrectPatient_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/patients/edit/{id}", testPatient.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/patients/edit"))
                    .andExpect(model().attributeExists("patient", "doctors"))
                    .andExpect(content().string(containsString("Patient One")));
        }

        @Test
        void editPatient_WithValidData_ShouldUpdatePatientAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/patients/edit/{id}", testPatient.getId())
                            .param("name", "Patient Updated")
                            .param("egn", testPatient.getEgn())
                            .param("generalPractitionerId", testDoctor.getId().toString())
                            .param("lastInsurancePaymentDate", LocalDate.now().minusMonths(1).toString())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/patients"))
                    .andExpect(flash().attribute("successMessage", "Patient updated successfully."));

            Patient updatedPatient = patientRepository.findById(testPatient.getId()).orElseThrow();
            assertTrue(updatedPatient.getName().equals("Patient Updated"));
        }

        @Test
        void showEditPatientForm_ForNonExistentPatient_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/patients/edit/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Delete Patient")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class DeletePatientTests {
        @Test
        void showDeleteConfirmation_ShouldDisplayConfirmationPage_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/patients/delete/{id}", testPatient.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/patients/delete-confirm"))
                    .andExpect(content().string(containsString("Are you sure you want to delete this patient?")))
                    .andExpect(content().string(containsString("Patient One")));
        }

        @Test
        void deletePatientConfirmed_ShouldDeletePatientAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/patients/delete/{id}", testPatient.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/patients"))
                    .andExpect(flash().attribute("successMessage", "Patient deleted successfully."));

            assertFalse(patientRepository.findById(testPatient.getId()).isPresent());
        }

        @Test
        void showDeleteConfirmation_ForNonExistentPatient_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/patients/delete/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Update Insurance Status")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class UpdateInsuranceStatusTests {
        @Test
        void updateInsuranceStatus_ShouldUpdateAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/patients/{id}/update-insurance", testPatient.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(String.format("/admin/patients/edit/%d", testPatient.getId())))
                    .andExpect(flash().attribute("successMessage", containsString("Successfully updated insurance status")));

            Patient updatedPatient = patientRepository.findById(testPatient.getId()).orElseThrow();
            assertTrue(updatedPatient.getLastInsurancePaymentDate().isAfter(LocalDate.now().minusMonths(1)));
        }

        @Test
        void updateInsuranceStatusManual_ShouldUpdateAndRedirect_HappyPath() throws Exception {
            LocalDate newDate = LocalDate.now().minusMonths(2);
            mockMvc.perform(post("/admin/patients/{id}/update-insurance-manual", testPatient.getId())
                            .param("manualInsuranceDate", newDate.toString())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(String.format("/admin/patients/edit/%d", testPatient.getId())))
                    .andExpect(flash().attribute("successMessage", containsString("Successfully updated insurance status")));

            Patient updatedPatient = patientRepository.findById(testPatient.getId()).orElseThrow();
            assertTrue(updatedPatient.getLastInsurancePaymentDate().isEqual(newDate));
        }

        @Test
        void updateInsuranceStatus_ForNonExistentPatient_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(post("/admin/patients/9999/update-insurance").with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void listPatients_AsDoctor_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/admin/patients"))
                    .andExpect(status().isForbidden());
        }
    }
}
