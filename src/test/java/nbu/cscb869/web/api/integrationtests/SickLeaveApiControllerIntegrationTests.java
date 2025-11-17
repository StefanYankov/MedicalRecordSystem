package nbu.cscb869.web.api.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import org.junit.jupiter.api.*;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(SickLeaveApiControllerIntegrationTests.TestConfig.class)
@DisplayName("SickLeaveApiController Integration Tests")
class SickLeaveApiControllerIntegrationTests {

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
    private ObjectMapper objectMapper;

    @Autowired
    private SickLeaveRepository sickLeaveRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @MockBean
    private Keycloak keycloak;

    private Visit testVisit;

    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setUp() {
        sickLeaveRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();

        Doctor doctor = new Doctor();
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setName("Dr. Test");
        doctorRepository.save(doctor);

        Patient patient = new Patient();
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setName("Test Patient");
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(doctor); // Set the GP
        patientRepository.save(patient);

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        diagnosisRepository.save(diagnosis);

        testVisit = new Visit();
        testVisit.setDoctor(doctor);
        testVisit.setPatient(patient);
        testVisit.setDiagnosis(diagnosis);
        testVisit.setVisitDate(LocalDate.now());
        testVisit.setVisitTime(LocalTime.of(10, 0));
        testVisit.setStatus(VisitStatus.COMPLETED);
        visitRepository.save(testVisit);
    }

    @AfterEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void tearDown() {
        sickLeaveRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {
        @Test
        @DisplayName("createSickLeave_WithValidDataAsAdmin_ShouldCreateSickLeaveInDb")
        void createSickLeave_WithValidDataAsAdmin_ShouldCreateSickLeaveInDb() throws Exception {
            // ARRANGE
            SickLeaveCreateDTO createDTO = new SickLeaveCreateDTO();
            createDTO.setVisitId(testVisit.getId());
            createDTO.setStartDate(LocalDate.now());
            createDTO.setDurationDays(5);

            long sickLeavesBefore = sickLeaveRepository.count();

            // ACT
            mockMvc.perform(post("/api/sick-leaves")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isCreated());

            // ASSERT
            long sickLeavesAfter = sickLeaveRepository.count();
            assertThat(sickLeavesAfter).isEqualTo(sickLeavesBefore + 1);
            SickLeave createdSickLeave = sickLeaveRepository.findAll().get(0);
            assertThat(createdSickLeave.getVisit().getId()).isEqualTo(testVisit.getId());
            assertThat(createdSickLeave.getDurationDays()).isEqualTo(5);
        }

        @Test
        @DisplayName("getAllSickLeaves_WhenSomeExist_ShouldReturnPageWithCorrectData")
        void getAllSickLeaves_WhenSomeExist_ShouldReturnPageWithCorrectData() throws Exception {
            // ARRANGE
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(testVisit);
            sickLeave.setStartDate(LocalDate.now());
            sickLeave.setDurationDays(3);
            sickLeaveRepository.save(sickLeave);

            // ACT & ASSERT
            mockMvc.perform(get("/api/sick-leaves")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].durationDays").value(3));
        }

        @Test
        @DisplayName("deleteSickLeave_WithExistingId_ShouldRemoveFromDb")
        void deleteSickLeave_WithExistingId_ShouldRemoveFromDb() throws Exception {
            // ARRANGE
            SickLeave sickLeave = new SickLeave();
            sickLeave.setVisit(testVisit);
            sickLeave.setStartDate(LocalDate.now());
            sickLeave.setDurationDays(7);
            SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);

            long sickLeavesBefore = sickLeaveRepository.count();

            // ACT
            mockMvc.perform(delete("/api/sick-leaves/" + savedSickLeave.getId())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            // ASSERT
            long sickLeavesAfter = sickLeaveRepository.count();
            assertThat(sickLeavesAfter).isEqualTo(sickLeavesBefore - 1);
            assertThat(sickLeaveRepository.findById(savedSickLeave.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Error Case Tests")
    class ErrorCaseTests {
        @Test
        @DisplayName("createSickLeave_WithNonExistentVisitId_ShouldReturnNotFound")
        void createSickLeave_WithNonExistentVisitId_ShouldReturnNotFound() throws Exception {
            // ARRANGE
            SickLeaveCreateDTO createDTO = new SickLeaveCreateDTO();
            createDTO.setVisitId(999L);
            createDTO.setStartDate(LocalDate.now());
            createDTO.setDurationDays(5);

            // ACT & ASSERT
            mockMvc.perform(post("/api/sick-leaves")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("createSickLeave_AsDoctor_ShouldReturnForbidden")
        void createSickLeave_AsDoctor_ShouldReturnForbidden() throws Exception {
            // ARRANGE
            SickLeaveCreateDTO createDTO = new SickLeaveCreateDTO();
            createDTO.setVisitId(testVisit.getId());
            createDTO.setStartDate(LocalDate.now());
            createDTO.setDurationDays(5);

            // ACT & ASSERT
            mockMvc.perform(post("/api/sick-leaves")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deleteSickLeave_WithNonExistentId_ShouldReturnNotFound")
        void deleteSickLeave_WithNonExistentId_ShouldReturnNotFound() throws Exception {
            // ACT & ASSERT
            mockMvc.perform(delete("/api/sick-leaves/999")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
