package nbu.cscb869.web.controllers.admin.integrationtests;

import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.*;
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
@Import(AdminSickLeaveControllerIntegrationTests.TestConfig.class)
class AdminSickLeaveControllerIntegrationTests {

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
    private SickLeaveRepository sickLeaveRepository;

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
    private SickLeave testSickLeave;

    @BeforeEach
    void setUp() {
        sickLeaveRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setName("Dr. Sick");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        testPatient = new Patient();
        testPatient.setName("Patient Sick");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
        testPatient.setGeneralPractitioner(testDoctor);
        testPatient.setLastInsurancePaymentDate(LocalDate.now());
        patientRepository.save(testPatient);

        testDiagnosis = new Diagnosis();
        testDiagnosis.setName("Flu");
        diagnosisRepository.save(testDiagnosis);

        testVisit = new Visit();
        testVisit.setDoctor(testDoctor);
        testVisit.setPatient(testPatient);
        testVisit.setDiagnosis(testDiagnosis);
        testVisit.setVisitDate(LocalDate.now());
        testVisit.setVisitTime(LocalTime.of(10, 0));
        testVisit.setStatus(VisitStatus.COMPLETED);
        visitRepository.save(testVisit);

        testSickLeave = new SickLeave();
        testSickLeave.setVisit(testVisit);
        testSickLeave.setStartDate(LocalDate.now());
        testSickLeave.setDurationDays(5);
        sickLeaveRepository.save(testSickLeave);

        testVisit.setSickLeave(testSickLeave);
        visitRepository.save(testVisit);
    }

    @Nested
    @DisplayName("List Sick Leaves")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class ListSickLeavesTests {
        @Test
        void listSickLeaves_ShouldReturnListViewWithSickLeaves_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/sick-leaves"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/sick-leaves/list"))
                    .andExpect(content().string(containsString(String.valueOf(testSickLeave.getDurationDays()))));
        }
    }

    @Nested
    @DisplayName("Create Sick Leave")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class CreateSickLeaveTests {
        @Test
        void showCreateSickLeaveForm_ShouldReturnCreateView_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/sick-leaves/create"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/sick-leaves/create"));
        }

        @Test
        void createSickLeave_WithValidData_ShouldCreateSickLeaveAndRedirect_HappyPath() throws Exception {
            Visit newVisit = new Visit();
            newVisit.setDoctor(testDoctor);
            newVisit.setPatient(testPatient);
            newVisit.setDiagnosis(testDiagnosis);
            newVisit.setVisitDate(LocalDate.now().plusDays(1));
            newVisit.setVisitTime(LocalTime.of(11, 0));
            newVisit.setStatus(VisitStatus.COMPLETED);
            visitRepository.save(newVisit);

            mockMvc.perform(post("/admin/sick-leaves/create")
                            .param("startDate", LocalDate.now().plusDays(1).toString())
                            .param("durationDays", "10")
                            .param("visitId", newVisit.getId().toString())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/sick-leaves"))
                    .andExpect(flash().attribute("successMessage", "Sick leave created successfully."));

            assertTrue(sickLeaveRepository.count() > 1);
        }

        @Test
        void createSickLeave_ForNonExistentVisit_ShouldReturnError_ErrorCase() throws Exception {
            mockMvc.perform(post("/admin/sick-leaves/create")
                            .param("startDate", LocalDate.now().plusDays(1).toString())
                            .param("durationDays", "10")
                            .param("visitId", "9999")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/sick-leaves/create"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Edit Sick Leave")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class EditSickLeaveTests {
        @Test
        void showEditSickLeaveForm_ShouldDisplayCorrectSickLeave_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/sick-leaves/edit/{id}", testSickLeave.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/sick-leaves/edit"))
                    .andExpect(model().attributeExists("sickLeave"))
                    .andExpect(content().string(containsString(String.valueOf(testSickLeave.getDurationDays()))));
        }

        @Test
        void editSickLeave_WithValidData_ShouldUpdateSickLeaveAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/sick-leaves/edit/{id}", testSickLeave.getId())
                            .param("startDate", testSickLeave.getStartDate().toString())
                            .param("durationDays", "15")
                            .param("visitId", testVisit.getId().toString())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/sick-leaves"))
                    .andExpect(flash().attribute("successMessage", "Sick leave updated successfully."));

            SickLeave updatedSickLeave = sickLeaveRepository.findById(testSickLeave.getId()).orElseThrow();
            assertTrue(updatedSickLeave.getDurationDays() == 15);
        }
    }

    @Nested
    @DisplayName("Delete Sick Leave")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class DeleteSickLeaveTests {
        @Test
        void showDeleteConfirmation_ShouldDisplayConfirmationPage_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/sick-leaves/delete/{id}", testSickLeave.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/sick-leaves/delete-confirm"))
                    .andExpect(content().string(containsString("Are you sure you want to delete this sick leave?")))
                    .andExpect(content().string(containsString(String.valueOf(testSickLeave.getDurationDays()))));
        }

        @Test
        void deleteSickLeaveConfirmed_ShouldDeleteSickLeaveAndRedirect_HappyPath() throws Exception {
            testVisit.setSickLeave(null);
            visitRepository.save(testVisit);

            mockMvc.perform(post("/admin/sick-leaves/delete/{id}", testSickLeave.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/sick-leaves"))
                    .andExpect(flash().attribute("successMessage", "Sick leave deleted successfully."));

            assertFalse(sickLeaveRepository.findById(testSickLeave.getId()).isPresent());
        }

        @Test
        void showDeleteConfirmation_ForNonExistentSickLeave_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/sick-leaves/delete/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void listSickLeaves_AsDoctor_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/admin/sick-leaves"))
                    .andExpect(status().isForbidden());
        }
    }
}
