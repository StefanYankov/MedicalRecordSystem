package nbu.cscb869.web.controllers.integrationtests;

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
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(VisitDocumentationControllerIntegrationTests.TestConfig.class)
class VisitDocumentationControllerIntegrationTests {

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
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    private Doctor testDoctor;
    private Visit testVisit;
    private Diagnosis testDiagnosis;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setKeycloakId("test-doctor-id");
        testDoctor.setName("Dr. House");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        Patient testPatient = new Patient();
        testPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
        testPatient.setName("Scheduled Patient");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setGeneralPractitioner(testDoctor);
        testPatient.setLastInsurancePaymentDate(LocalDate.now());
        patientRepository.save(testPatient);

        testDiagnosis = new Diagnosis();
        testDiagnosis.setName("Influenza");
        diagnosisRepository.save(testDiagnosis);

        testVisit = new Visit();
        testVisit.setDoctor(testDoctor);
        testVisit.setPatient(testPatient);
        testVisit.setVisitDate(LocalDate.now());
        testVisit.setVisitTime(LocalTime.of(11, 0));
        testVisit.setStatus(VisitStatus.SCHEDULED);
        visitRepository.save(testVisit);
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "test-doctor-id", authorities = "ROLE_DOCTOR")
    void getDocumentVisitForm_AsCorrectDoctor_ShouldReturnOk_HappyPath() throws Exception {
        mockMvc.perform(get("/visit/document/" + testVisit.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("visit/document"))
                .andExpect(model().attributeExists("visit"));
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "test-doctor-id", authorities = "ROLE_DOCTOR")
    void postDocumentVisitForm_WithValidData_ShouldRedirectToSchedule_HappyPath() throws Exception {
        mockMvc.perform(post("/visit/document/" + testVisit.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("diagnosisId", testDiagnosis.getId().toString())
                        .param("notes", "Patient has classic flu symptoms.")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/schedule"));

        Visit updatedVisit = visitRepository.findById(testVisit.getId()).get();
        assertEquals(VisitStatus.COMPLETED, updatedVisit.getStatus());
        assertEquals("Patient has classic flu symptoms.", updatedVisit.getNotes());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void getDocumentVisitForm_AsPatient_ShouldReturnForbidden_ErrorCase() throws Exception {
        mockMvc.perform(get("/visit/document/" + testVisit.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "wrong-doctor-id", authorities = "ROLE_DOCTOR")
    void getDocumentVisitForm_AsWrongDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
        mockMvc.perform(get("/visit/document/" + testVisit.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "wrong-doctor-id", authorities = "ROLE_DOCTOR")
    void postDocumentVisitForm_AsWrongDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
        mockMvc.perform(post("/visit/document/" + testVisit.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("diagnosisId", testDiagnosis.getId().toString())
                        .param("notes", "Attempted by wrong doctor.")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "test-doctor-id", authorities = "ROLE_DOCTOR")
    void postDocumentVisitForm_WithInvalidData_ShouldReturnFormWithErrors_ErrorCase() throws Exception {
        mockMvc.perform(post("/visit/document/" + testVisit.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("notes", "") // Invalid: notes are required
                        .with(csrf()))
                .andExpect(status().isOk()) // Returns to the form
                .andExpect(view().name("visit/document"))
                .andExpect(model().hasErrors());
    }

    @Test
    void getDocumentVisitForm_AsUnauthenticatedUser_ShouldRedirectToLogin_ErrorCase() throws Exception {
        mockMvc.perform(get("/visit/document/" + testVisit.getId()).accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
    }
}
