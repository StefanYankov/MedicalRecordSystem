package nbu.cscb869.web.controllers.admin.integrationtests;

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
@Import(AdminVisitControllerIntegrationTests.TestConfig.class)
class AdminVisitControllerIntegrationTests {

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
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    private Doctor testDoctor;
    private Patient testPatient;
    private Diagnosis testDiagnosis;
    private Visit testVisit;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setName("Dr. Visit");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        testPatient = new Patient();
        testPatient.setName("Patient Visit");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
        testPatient.setGeneralPractitioner(testDoctor);
        testPatient.setLastInsurancePaymentDate(LocalDate.now());
        patientRepository.save(testPatient);

        testDiagnosis = new Diagnosis();
        testDiagnosis.setName("Common Cold");
        diagnosisRepository.save(testDiagnosis);

        testVisit = new Visit();
        testVisit.setDoctor(testDoctor);
        testVisit.setPatient(testPatient);
        testVisit.setDiagnosis(testDiagnosis);
        testVisit.setVisitDate(LocalDate.now());
        testVisit.setVisitTime(LocalTime.of(10, 0));
        testVisit.setStatus(VisitStatus.SCHEDULED);
        visitRepository.save(testVisit);
    }

    @Nested
    @DisplayName("List Visits")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class ListVisitsTests {
        @Test
        void listVisits_ShouldReturnListViewWithVisits_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/visits"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/visits/list"))
                    .andExpect(content().string(containsString("Patient Visit")));
        }

        @Test
        void listVisits_WithFilter_ShouldReturnFilteredListView_HappyPath() throws Exception {
            Visit filteredVisit = new Visit();
            filteredVisit.setDoctor(testDoctor);
            filteredVisit.setPatient(testPatient);
            filteredVisit.setDiagnosis(testDiagnosis);
            filteredVisit.setVisitDate(LocalDate.now().plusDays(1));
            filteredVisit.setVisitTime(LocalTime.of(11, 0));
            filteredVisit.setStatus(VisitStatus.COMPLETED);
            filteredVisit.getPatient().setEgn("1111111111"); // Set a unique EGN for filtering
            visitRepository.save(filteredVisit);

            mockMvc.perform(get("/admin/visits").param("filter", "1111111111"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/visits/list"))
                    .andExpect(content().string(containsString("1111111111")));
        }
    }

    @Nested
    @DisplayName("Edit Visit")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class EditVisitTests {
        @Test
        void showEditVisitForm_ShouldDisplayCorrectVisit_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/visits/edit/{id}", testVisit.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/visits/edit"))
                    .andExpect(model().attributeExists("visit", "patients", "doctors", "diagnoses", "statuses"))
                    .andExpect(content().string(containsString("Patient Visit")));
        }

        @Test
        void editVisit_WithValidData_ShouldUpdateVisitAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/visits/edit/{id}", testVisit.getId())
                            .param("visitDate", LocalDate.now().plusDays(1).toString())
                            .param("visitTime", LocalTime.of(10, 0).toString())
                            .param("patientId", testPatient.getId().toString())
                            .param("doctorId", testDoctor.getId().toString())
                            .param("diagnosisId", testDiagnosis.getId().toString())
                            .param("status", VisitStatus.COMPLETED.name())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/visits"))
                    .andExpect(flash().attribute("successMessage", "Visit updated successfully."));

            Visit updatedVisit = visitRepository.findById(testVisit.getId()).orElseThrow();
            assertTrue(updatedVisit.getVisitDate().isEqual(LocalDate.now().plusDays(1)));
            assertTrue(updatedVisit.getStatus().equals(VisitStatus.COMPLETED));
        }

        @Test
        void showEditVisitForm_ForNonExistentVisit_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/visits/edit/9999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void editVisit_WithInvalidData_ShouldRedirectBackToFormAndShowErrors_ErrorCase() throws Exception {
            mockMvc.perform(post("/admin/visits/edit/{id}", testVisit.getId())
                            .param("visitDate", "invalid-date") // Invalid date format
                            .param("visitTime", LocalTime.of(10, 0).toString())
                            .param("patientId", testPatient.getId().toString())
                            .param("doctorId", testDoctor.getId().toString())
                            .param("diagnosisId", testDiagnosis.getId().toString())
                            .param("status", VisitStatus.SCHEDULED.name())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(String.format("/admin/visits/edit/%d", testVisit.getId())))
                    .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.visit"));
        }
    }

    @Nested
    @DisplayName("Delete Visit")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class DeleteVisitTests {
        @Test
        void showDeleteConfirmation_ShouldDisplayConfirmationPage_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/visits/delete/{id}", testVisit.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/visits/delete-confirm"))
                    .andExpect(content().string(containsString("Are you sure you want to delete this visit?")))
                    .andExpect(content().string(containsString("Patient Visit")));
        }

        @Test
        void deleteVisitConfirmed_ShouldDeleteVisitAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/visits/delete/{id}", testVisit.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/visits"))
                    .andExpect(flash().attribute("successMessage", "Visit deleted successfully."));

            assertFalse(visitRepository.findById(testVisit.getId()).isPresent());
        }

        @Test
        void showDeleteConfirmation_ForNonExistentVisit_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/visits/delete/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void listVisits_AsDoctor_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/admin/visits"))
                    .andExpect(status().isForbidden());
        }
    }
}
