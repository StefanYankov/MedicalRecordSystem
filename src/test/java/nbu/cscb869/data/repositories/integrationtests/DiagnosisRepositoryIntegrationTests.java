package nbu.cscb869.data.repositories.integrationtests;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.PatientDiagnosisDTO;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
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
@Import(DiagnosisRepositoryIntegrationTests.TestConfig.class)
class DiagnosisRepositoryIntegrationTests {

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
    private DiagnosisRepository diagnosisRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private VisitRepository visitRepository;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
    }

    private Diagnosis createDiagnosis(String name, String description) {
        return Diagnosis.builder()
                .name(name)
                .description(description)
                .build();
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

    // Happy Path: Basic save operation
    @Test
    void save_WithValidDiagnosis_SavesSuccessfully_HappyPath() {
        Diagnosis diagnosis = createDiagnosis("Flu", "Influenza infection");
        Diagnosis saved = diagnosisRepository.save(diagnosis);

        Optional<Diagnosis> found = diagnosisRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Flu", found.get().getName());
    }

    // Error Case: Duplicate name
    @Test
    void save_WithDuplicateName_ThrowsException_ErrorCase() {
        Diagnosis diagnosis1 = createDiagnosis("Flu", "Influenza");
        Diagnosis diagnosis2 = createDiagnosis("Flu", "Another description");
        diagnosisRepository.save(diagnosis1);

        assertThrows(DataIntegrityViolationException.class, () -> diagnosisRepository.save(diagnosis2));
    }

    // Edge Case: Maximum name length
    @Test
    void save_WithMaximumNameLength_SavesSuccessfully_EdgeCase() {
        String maxName = "A".repeat(100);
        Diagnosis diagnosis = createDiagnosis(maxName, "Description");
        Diagnosis saved = diagnosisRepository.save(diagnosis);

        assertEquals(maxName, saved.getName());
    }

    // Happy Path: Find by existing name
    @Test
    void findByName_WithExistingName_ReturnsDiagnosis_HappyPath() {
        Diagnosis diagnosis = createDiagnosis("Hypertension", "High blood pressure");
        diagnosisRepository.save(diagnosis);

        Optional<Diagnosis> found = diagnosisRepository.findByName("Hypertension");

        assertTrue(found.isPresent());
        assertEquals("Hypertension", found.get().getName());
    }

    // Error Case: Non-existent name
    @Test
    void findByName_WithNonExistentName_ReturnsEmpty_ErrorCase() {
        Optional<Diagnosis> found = diagnosisRepository.findByName("Nonexistent");

        assertFalse(found.isPresent());
    }

    // Happy Path: Partial name match with pagination
    @Test
    void findByNameContainingIgnoreCase_PartialName_ReturnsPaged_HappyPath() {
        Diagnosis d1 = createDiagnosis("Influenza", "Viral infection");
        Diagnosis d2 = createDiagnosis("Flu", "Similar to influenza");
        diagnosisRepository.saveAll(List.of(d1, d2));

        Page<Diagnosis> result = diagnosisRepository.findByNameContainingIgnoreCase("flu", PageRequest.of(0, 2));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(d -> d.getName().equals("Flu")));
    }

    // Error Case: Non-existent partial name
    @Test
    void findByNameContainingIgnoreCase_NonExistentName_ReturnsEmptyPage_ErrorCase() {
        Page<Diagnosis> result = diagnosisRepository.findByNameContainingIgnoreCase("xyz", PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
    }

    // Edge Case: Empty filter returns all
    @Test
    void findByNameContainingIgnoreCase_EmptyFilter_ReturnsAll_EdgeCase() {
        Diagnosis d = createDiagnosis("Migraine", "Severe headache");
        diagnosisRepository.save(d);

        Page<Diagnosis> result = diagnosisRepository.findByNameContainingIgnoreCase("", PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
    }

    // Happy Path: Find patients with diagnosis using relationships
    @Test
    void findPatientsByDiagnosis_WithMultipleVisits_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient1 = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Patient patient2 = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        doctor = doctorRepository.save(doctor);
        patient1 = patientRepository.save(patient1);
        patient2 = patientRepository.save(patient2);
        Diagnosis diagnosis = createDiagnosis("Diabetes", "Type 2 diabetes");
        diagnosis = diagnosisRepository.save(diagnosis);
        visitRepository.saveAll(List.of(
                createVisit(patient1, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient2, doctor, diagnosis, LocalDate.now(), LocalTime.of(11, 0), null)
        ));

        Page<PatientDiagnosisDTO> patients = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(2, patients.getTotalElements());
        assertEquals(1, patients.getContent().size());
        assertEquals("Diabetes", patients.getContent().getFirst().getDiagnosisName());
    }

    // Error Case: No visits for diagnosis
    @Test
    void findPatientsByDiagnosis_WithNoVisits_ReturnsEmpty_ErrorCase() {
        Diagnosis diagnosis = createDiagnosis("Cancer", "Malignant growth");
        diagnosisRepository.save(diagnosis);

        Page<PatientDiagnosisDTO> patients = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(0, patients.getTotalElements());
    }

    // Edge Case: Large page size
    @Test
    void findPatientsByDiagnosis_LargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Influenza");
        diagnosis = diagnosisRepository.save(diagnosis);
        visitRepository.save(createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 0), null));

        Page<PatientDiagnosisDTO> patients = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(0, 100));

        assertEquals(1, patients.getTotalElements());
    }

    // Happy Path: Find most frequent diagnoses
    @Test
    void findMostFrequentDiagnoses_WithMultipleVisits_ReturnsSorted_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        Diagnosis diagnosis1 = createDiagnosis("Asthma", "Chronic respiratory");
        Diagnosis diagnosis2 = createDiagnosis("Flu", "Influenza");
        diagnosisRepository.saveAll(List.of(diagnosis1, diagnosis2));
        visitRepository.saveAll(List.of(
                createVisit(patient, doctor, diagnosis1, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis1, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null),
                createVisit(patient, doctor, diagnosis2, LocalDate.now().minusDays(2), LocalTime.of(12, 0), null)
        ));

        List<DiagnosisVisitCountDTO> frequent = diagnosisRepository.findMostFrequentDiagnoses();

        assertEquals(2, frequent.size());
        assertEquals("Asthma", frequent.getFirst().getDiagnosisName());
        assertEquals(2, frequent.getFirst().getVisitCount());
    }

    // Error Case: No visits
    @Test
    void findMostFrequentDiagnoses_WithNoVisits_ReturnsEmpty_ErrorCase() {
        Diagnosis diagnosis = createDiagnosis("Hepatitis", "Liver inflammation");
        diagnosisRepository.save(diagnosis);

        List<DiagnosisVisitCountDTO> frequent = diagnosisRepository.findMostFrequentDiagnoses();

        assertTrue(frequent.isEmpty());
    }

    // Edge Case: Maximum visit count
    @Test
    void findMostFrequentDiagnoses_MaxCount_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Influenza");
        diagnosisRepository.save(diagnosis);
        for (int i = 0; i < 100; i++) {
            visitRepository.save(createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(i), LocalTime.of(10, 0), null));
        }

        List<DiagnosisVisitCountDTO> frequent = diagnosisRepository.findMostFrequentDiagnoses();

        assertEquals(1, frequent.size());
        assertEquals(100, frequent.getFirst().getVisitCount());
    }

    // Happy Path: Delete existing diagnosis
    @Test
    void delete_WithExistingDiagnosis_RemovesDiagnosis_HappyPath() {
        Diagnosis diagnosis = createDiagnosis("Gastritis", "Stomach inflammation");
        Diagnosis saved = diagnosisRepository.save(diagnosis);

        diagnosisRepository.delete(saved);

        assertTrue(diagnosisRepository.findById(saved.getId()).isEmpty());
    }

    // Error Case: Delete non-existent diagnosis
    @Test
    void delete_WithNonExistentId_DoesNotThrow_ErrorCase() {
        assertDoesNotThrow(() -> diagnosisRepository.delete(Diagnosis.builder().name("Nonexistent").build()));
    }
}
