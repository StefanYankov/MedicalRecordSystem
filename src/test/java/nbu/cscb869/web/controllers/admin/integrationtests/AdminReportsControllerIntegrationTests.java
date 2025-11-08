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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockKeycloakUser(authorities = "ROLE_ADMIN")
@Import(AdminReportsControllerIntegrationTests.TestConfig.class)
class AdminReportsControllerIntegrationTests {

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
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private VisitRepository visitRepository;

    private Doctor testDoctor;
    private Patient testPatient;
    private Diagnosis testDiagnosis;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setName("Dr. Test");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        testPatient = new Patient();
        testPatient.setName("Test Patient");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
        testPatient.setGeneralPractitioner(testDoctor);
        testPatient.setLastInsurancePaymentDate(LocalDate.now());
        patientRepository.save(testPatient);

        testDiagnosis = new Diagnosis();
        testDiagnosis.setName("Test Diagnosis");
        diagnosisRepository.save(testDiagnosis);

        Visit visit = new Visit();
        visit.setDoctor(testDoctor);
        visit.setPatient(testPatient);
        visit.setDiagnosis(testDiagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(LocalTime.now());
        visit.setStatus(VisitStatus.COMPLETED);
        visitRepository.save(visit);
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void getReportsIndex_AsDoctor_ShouldBeForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/reports"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /admin/reports")
    class IndexTests {
        @Test
        void index_ShouldReturnReportsIndexPage_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/reports"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/index"))
                    .andExpect(content().string(containsString("Reports")));
        }
    }

    @Nested
    @DisplayName("GET /admin/reports/patients-by-diagnosis")
    class PatientsByDiagnosisTests {
        @Test
        void getPatientsByDiagnosis_WithData_ShouldDisplayPatient_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/reports/patients-by-diagnosis").param("diagnosisId", testDiagnosis.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/patients-by-diagnosis"))
                    .andExpect(content().string(containsString(testPatient.getName())));
        }
    }

    @Nested
    @DisplayName("GET /admin/reports/most-frequent-diagnoses")
    class MostFrequentDiagnosesTests {
        @Test
        void getMostFrequentDiagnoses_WithData_ShouldDisplayDiagnosis_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/reports/most-frequent-diagnoses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/most-frequent-diagnoses"))
                    .andExpect(content().string(containsString(testDiagnosis.getName())));
        }
    }

    @Nested
    @DisplayName("GET /admin/reports/patients-by-gp")
    class PatientsByGpTests {
        @Test
        void getPatientsByGp_WithData_ShouldDisplayPatient_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/reports/patients-by-gp").param("gpId", testDoctor.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/patients-by-gp"))
                    .andExpect(content().string(containsString(testPatient.getName())));
        }

        @Test
        void getPatientsByGp_WithNoPatients_ShouldShowNoPatientsMessage_EdgeCase() throws Exception {
            Doctor gpWithNoPatients = new Doctor();
            gpWithNoPatients.setName("Dr. Lonely");
            gpWithNoPatients.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            gpWithNoPatients.setKeycloakId(TestDataUtils.generateKeycloakId());
            gpWithNoPatients.setGeneralPractitioner(true);
            doctorRepository.save(gpWithNoPatients);

            mockMvc.perform(get("/admin/reports/patients-by-gp").param("gpId", gpWithNoPatients.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/patients-by-gp"))
                    .andExpect(content().string(containsString("No patients found")));
        }

        @Test
        void getPatientsByGp_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/reports/patients-by-gp").param("gpId", "9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /admin/reports/patient-count-by-gp")
    class PatientCountByGpTests {
        @Test
        void getPatientCountByGp_WithData_ShouldDisplayCount_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/reports/patient-count-by-gp"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/patient-count-by-gp"))
                    .andExpect(content().string(containsString("1"))); // Expecting count of 1
        }
    }

    @Nested
    @DisplayName("GET /admin/reports/visit-count-by-doctor")
    class VisitCountByDoctorTests {
        @Test
        void getVisitCountByDoctor_WithData_ShouldDisplayCount_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/reports/visit-count-by-doctor"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/visit-count-by-doctor"))
                    .andExpect(content().string(containsString("1"))); // Expecting count of 1
        }
    }
}
