package nbu.cscb869.web.controllers.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.data.dtos.VisitCreateDTO;
import nbu.cscb869.services.services.utility.contracts.NotificationService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(VisitControllerIntegrationTests.TestConfig.class)
class VisitControllerIntegrationTests {

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

        @Bean
        @Primary
        public NotificationService notificationService() {
            return Mockito.mock(NotificationService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private NotificationService notificationService;

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
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setName("Dr. Test");
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        testPatient = new Patient();
        testPatient.setKeycloakId("patient-owner-id");
        testPatient.setEgn(TestDataUtils.generateValidEgn());
        testPatient.setName("Patient Owner");
        testPatient.setGeneralPractitioner(testDoctor);
        testPatient.setLastInsurancePaymentDate(LocalDate.now());
        patientRepository.save(testPatient);

        testDiagnosis = new Diagnosis();
        testDiagnosis.setName("Flu");
        diagnosisRepository.save(testDiagnosis);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createVisit_WithValidDataAsDoctor_ShouldCallNotificationService_HappyPath() throws Exception {
        // ARRANGE
        VisitCreateDTO createDTO = new VisitCreateDTO();
        createDTO.setPatientId(testPatient.getId());
        createDTO.setDoctorId(testDoctor.getId());
        createDTO.setDiagnosisId(testDiagnosis.getId());
        createDTO.setVisitDate(LocalDate.now().plusDays(1));
        createDTO.setVisitTime(LocalTime.of(10, 0));

        // ACT & ASSERT
        mockMvc.perform(post("/api/visits")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated());

        // VERIFY
        verify(notificationService, Mockito.times(1)).sendVisitConfirmation(any(), any());
    }

    @Test
    void createVisit_AsPatient_ShouldReturnForbidden_ErrorCase() throws Exception {
        // ARRANGE
        VisitCreateDTO createDTO = new VisitCreateDTO();
        createDTO.setPatientId(testPatient.getId());
        createDTO.setDoctorId(testDoctor.getId());
        createDTO.setDiagnosisId(testDiagnosis.getId());
        createDTO.setVisitDate(LocalDate.now().plusDays(1));
        createDTO.setVisitTime(LocalTime.of(10, 0));

        // ACT & ASSERT
        mockMvc.perform(post("/api/visits")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PATIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createVisit_AsUnauthenticatedUser_ShouldReturnUnauthorized_ErrorCase() throws Exception {
        // ARRANGE
        VisitCreateDTO createDTO = new VisitCreateDTO();

        // ACT & ASSERT
        mockMvc.perform(post("/api/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createVisit_WithMissingPatientId_ShouldReturnBadRequest_ErrorCase() throws Exception {
        // ARRANGE
        VisitCreateDTO createDTO = new VisitCreateDTO();
        createDTO.setDoctorId(testDoctor.getId());
        createDTO.setVisitDate(LocalDate.now().plusDays(1));
        createDTO.setVisitTime(LocalTime.of(10, 0));

        // ACT & ASSERT
        mockMvc.perform(post("/api/visits")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createVisit_WhenPatientHasInvalidInsurance_ShouldReturnBadRequest_ErrorCase() throws Exception {
        // ARRANGE
        testPatient.setLastInsurancePaymentDate(LocalDate.now().minusMonths(7));
        patientRepository.save(testPatient);

        VisitCreateDTO createDTO = new VisitCreateDTO();
        createDTO.setPatientId(testPatient.getId());
        createDTO.setDoctorId(testDoctor.getId());
        createDTO.setVisitDate(LocalDate.now().plusDays(1));
        createDTO.setVisitTime(LocalTime.of(10, 0));

        // ACT & ASSERT
        mockMvc.perform(post("/api/visits")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }
}
