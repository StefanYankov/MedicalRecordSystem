package nbu.cscb869.data.repositories.integrationtests;

import jakarta.validation.ConstraintViolationException;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TreatmentRepositoryIntegrationTests.TestConfig.class)
class TreatmentRepositoryIntegrationTests {

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
    private TreatmentRepository treatmentRepository;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @BeforeEach
    void setUp() {
        treatmentRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
    }

    private Doctor createDoctor(String uniqueIdNumber, boolean isGeneralPractitioner, String name) {
        return Doctor.builder()
                .uniqueIdNumber(uniqueIdNumber)
                .isGeneralPractitioner(isGeneralPractitioner)
                .name(name)
                .keycloakId(TestDataUtils.generateKeycloakId())
                .build();
    }

    private Patient createPatient(String egn, Doctor generalPractitioner, LocalDate lastInsurancePaymentDate) {
        return Patient.builder()
                .egn(egn)
                .name("Test Patient")
                .generalPractitioner(generalPractitioner)
                .lastInsurancePaymentDate(lastInsurancePaymentDate)
                .keycloakId(TestDataUtils.generateKeycloakId())
                .build();
    }

    private Diagnosis createDiagnosis(String name, String description) {
        return Diagnosis.builder()
                .name(name)
                .description(description)
                .build();
    }

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate, LocalTime visitTime, SickLeave sickLeave) {
        Visit visit = Visit.builder()
                .patient(patient)
                .doctor(doctor)
                .diagnosis(diagnosis)
                .visitDate(visitDate)
                .visitTime(visitTime)
                .status(VisitStatus.COMPLETED) // FIX: Add default status
                .build();
        if (sickLeave != null) {
            visit.setSickLeave(sickLeave);
            sickLeave.setVisit(visit);
        }
        return visit;
    }

    private Treatment createTreatment(String description, Visit visit) {
        return Treatment.builder()
                .visit(visit)
                .description(description)
                .build();
    }

    @Test
    void save_WithValidTreatment_SavesSuccessfully_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Antibiotic therapy", visit);
        Treatment saved = treatmentRepository.save(treatment);

        Optional<Treatment> found = treatmentRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Antibiotic therapy", found.get().getDescription());
    }

    @Test
    void findAll_WithMultipleTreatments_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null)
        );
        visitRepository.saveAll(visits);
        List<Treatment> treatments = List.of(
                createTreatment("Antibiotic course", visits.get(0)),
                createTreatment("Inhaler therapy", visits.get(1))
        );
        treatmentRepository.saveAll(treatments);

        Page<Treatment> result = treatmentRepository.findAll(PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void save_WithNullVisit_ThrowsException_ErrorCase() {
        Treatment treatment = createTreatment("Invalid therapy", null);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            treatmentRepository.save(treatment);
        });
    }

    @Test
    void save_WithOverMaxDescription_ThrowsException_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Asthma", "Respiratory condition");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visit = visitRepository.save(visit);
        String overMaxDescription = "D".repeat(ValidationConfig.DESCRIPTION_MAX_LENGTH + 1);
        Treatment treatment = createTreatment(overMaxDescription, visit);

        assertThrows(ConstraintViolationException.class, () -> {
            treatmentRepository.save(treatment);
        });
    }

    @Test
    void findAll_WithNoTreatments_ReturnsEmptyPage_ErrorCase() {
        Page<Treatment> result = treatmentRepository.findAll(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void findAll_WithLargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Allergy", "Hypersensitivity");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            visits.add(createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null));
        }
        visitRepository.saveAll(visits);
        List<Treatment> treatments = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            treatments.add(createTreatment("Treatment" + i, visits.get(i)));
        }
        treatmentRepository.saveAll(treatments);

        Page<Treatment> result = treatmentRepository.findAll(PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
    }
}
