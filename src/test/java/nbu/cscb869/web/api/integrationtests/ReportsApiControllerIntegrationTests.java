package nbu.cscb869.web.api.integrationtests;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(ReportsApiControllerIntegrationTests.TestConfig.class)
@DisplayName("ReportsApiController Integration Tests")
class ReportsApiControllerIntegrationTests {

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
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private VisitRepository visitRepository;

    @MockBean
    private Keycloak keycloak;

    private Doctor testDoctor;
    private Patient testPatient;
    private Diagnosis testDiagnosis;

    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setName("Dr. Test");
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        testPatient = new Patient();
        testPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setName("Test Patient");
        testPatient.setGeneralPractitioner(testDoctor);
        testPatient.setLastInsurancePaymentDate(LocalDate.now());
        patientRepository.save(testPatient);

        testDiagnosis = new Diagnosis();
        testDiagnosis.setName("Flu");
        diagnosisRepository.save(testDiagnosis);

        Visit visit = new Visit();
        visit.setDoctor(testDoctor);
        visit.setPatient(testPatient);
        visit.setDiagnosis(testDiagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(LocalTime.of(10, 0));
        visit.setStatus(VisitStatus.COMPLETED);
        visitRepository.save(visit);
    }

    @AfterEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void tearDown() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/reports/doctor-visit-counts - Should Return Correct Report")
    void getDoctorVisitCounts_ShouldReturnCorrectReport() throws Exception {
        mockMvc.perform(get("/api/reports/doctor-visit-counts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctor.name").value("Dr. Test"))
                .andExpect(jsonPath("$[0].visitCount").value(1));
    }

    @Test
    @DisplayName("GET /api/reports/patients-by-diagnosis - Should Return Correct Patients")
    void getPatientsByDiagnosis_ShouldReturnCorrectPatients() throws Exception {
        mockMvc.perform(get("/api/reports/patients-by-diagnosis")
                        .param("diagnosisId", testDiagnosis.getId().toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Patient"));
    }

    @Test
    @DisplayName("GET /api/reports/most-frequent-diagnoses - Should Return Correct Report")
    void getMostFrequentDiagnoses_ShouldReturnCorrectReport() throws Exception {
        mockMvc.perform(get("/api/reports/most-frequent-diagnoses")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosisName").value("Flu"))
                .andExpect(jsonPath("$[0].visitCount").value(1));
    }

    @Test
    @DisplayName("GET /api/reports/gp-patient-counts - Should Return Correct Report")
    void getGpPatientCounts_ShouldReturnCorrectReport() throws Exception {
        mockMvc.perform(get("/api/reports/gp-patient-counts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dr. Test"))
                .andExpect(jsonPath("$[0].patientCount").value(1));
    }
}
