package nbu.cscb869.web.controllers.integrationtests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.VisitCreateDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@Import({VisitControllerIntegrationTests.AsyncTestConfig.class, VisitControllerIntegrationTests.RestTemplateConfig.class})
class VisitControllerIntegrationTests {

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        @Primary
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @TestConfiguration
    static class RestTemplateConfig {
        @Bean
        public RestTemplateCustomizer restTemplateCustomizer() {
            return restTemplate -> {
                MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
                List<MediaType> supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
                supportedMediaTypes.add(MediaType.valueOf("text/json")); // Add support for MailHog's content type
                converter.setSupportedMediaTypes(supportedMediaTypes);
                restTemplate.getMessageConverters().add(0, converter);
            };
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DiagnosisRepository diagnosisRepository;

    private final String mailHogApiUrl = "http://localhost:8025/api/v2/messages";

    private Doctor testDoctor;
    private Patient testPatient;
    private Diagnosis testDiagnosis;

    @BeforeEach
    void setUp() {
        testDoctor = doctorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Doctor d = new Doctor();
            d.setKeycloakId(TestDataUtils.generateKeycloakId());
            d.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            d.setName("Dr. Test");
            d.setGeneralPractitioner(true);
            return doctorRepository.save(d);
        });

        testPatient = patientRepository.findByKeycloakId("patient-owner-id").orElseGet(() -> {
            Patient p = new Patient();
            p.setKeycloakId("patient-owner-id");
            p.setEgn(TestDataUtils.generateValidEgn());
            p.setName("Patient Owner");
            p.setGeneralPractitioner(testDoctor);
            p.setLastInsurancePaymentDate(LocalDate.now());
            return patientRepository.save(p);
        });

        testDiagnosis = diagnosisRepository.findByName("Flu").orElseGet(() -> {
            Diagnosis d = new Diagnosis();
            d.setName("Flu");
            return diagnosisRepository.save(d);
        });

        visitRepository.deleteAll();
        restTemplate.delete("http://localhost:8025/api/v1/messages"); // Clear MailHog messages
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR", email = "test.patient@example.com")
    void createVisit_WithValidData_ShouldSendConfirmationEmail_HappyPath() {
        VisitCreateDTO createDTO = new VisitCreateDTO();
        createDTO.setPatientId(testPatient.getId());
        createDTO.setDoctorId(testDoctor.getId());
        createDTO.setDiagnosisId(testDiagnosis.getId());
        createDTO.setVisitDate(LocalDate.now());
        createDTO.setVisitTime(LocalTime.of(10, 0));

        // Start a transaction for the controller call
        TestTransaction.start();

        // Call the controller endpoint
        ResponseEntity<VisitViewDTO> response = restTemplate.postForEntity("/api/visits", createDTO, VisitViewDTO.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());

        // Manually commit the transaction to trigger async event listeners and ensure data is visible
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Now that the transaction is committed and @Async is synchronous, the email should have been sent.
        ResponseEntity<MailHogMessageResponse> mailHogResponse = restTemplate.getForEntity(mailHogApiUrl, MailHogMessageResponse.class);
        MailHogMessage[] messages = mailHogResponse.getBody().getItems();

        assertEquals(1, messages.length, "Expected exactly one email to be sent.");
        MailHogMessage email = messages[0];
        assertEquals("test.patient@example.com", email.getTo().get(0).getMailbox() + "@" + email.getTo().get(0).getDomain());
        assertTrue(email.getContent().getBody().contains("Dear Patient Owner"));

        // Start a new transaction for the test framework to roll back after the test
        TestTransaction.start();
    }

    // Inner classes for MailHog API response parsing
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MailHogMessageResponse {
        private MailHogMessage[] items;
        public MailHogMessage[] getItems() { return items; }
        public void setItems(MailHogMessage[] items) { this.items = items; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MailHogMessage {
        private List<MailHogAddress> to;
        private MailHogContent content;
        public List<MailHogAddress> getTo() { return to; }
        public void setTo(List<MailHogAddress> to) { this.to = to; }
        public MailHogContent getContent() { return content; }
        public void setContent(MailHogContent content) { this.content = content; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MailHogAddress {
        private String mailbox;
        private String domain;
        public String getMailbox() { return mailbox; }
        public void setMailbox(String mailbox) { this.mailbox = mailbox; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MailHogContent {
        private String body;
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
}
