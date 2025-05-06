package nbu.cscb869.data.repositories.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.PatientDiagnosisDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DiagnosisRepositoryIntegrationTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
            .withExposedPorts(3306)
            .withEnv("MYSQL_ROOT_PASSWORD", "test");

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
    }

    private Diagnosis createDiagnosis(String name, String description) {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName(name);
        diagnosis.setDescription(description);
        return diagnosis;
    }

    private Doctor createDoctor(String name, String uniqueIdNumber, boolean isGeneralPractitioner) {
        Doctor doctor = new Doctor();
        doctor.setName(name);
        doctor.setUniqueIdNumber(uniqueIdNumber);
        doctor.setGeneralPractitioner(isGeneralPractitioner);
        return doctor;
    }

    private Patient createPatient(String name, String egn, Doctor generalPractitioner, LocalDate lastInsurancePaymentDate) {
        Patient patient = new Patient();
        patient.setName(name);
        patient.setEgn(egn);
        patient.setGeneralPractitioner(generalPractitioner);
        patient.setLastInsurancePaymentDate(lastInsurancePaymentDate);
        return patient;
    }

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate, LocalTime visitTime, boolean sickLeaveIssued) {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(visitDate);
        visit.setVisitTime(visitTime);
        visit.setSickLeaveIssued(sickLeaveIssued);
        return visit;
    }

    private void softDeleteRelatedVisits(Diagnosis diagnosis) {
        Page<Visit> visits = visitRepository.findByDiagnosis(diagnosis, PageRequest.of(0, Integer.MAX_VALUE));
        visits.forEach(visitRepository::delete);
        entityManager.flush();
    }

    // Happy Path
    @Test
    void Save_WithValidDiagnosis_SavesSuccessfully() {
        Diagnosis diagnosis = createDiagnosis("Flu", "Influenza infection");
        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Optional<Diagnosis> foundDiagnosis = diagnosisRepository.findById(savedDiagnosis.getId());

        assertTrue(foundDiagnosis.isPresent());
        assertEquals("Flu", foundDiagnosis.get().getName());
        assertEquals("Influenza infection", foundDiagnosis.get().getDescription());
        assertFalse(foundDiagnosis.get().getIsDeleted());
        assertNotNull(foundDiagnosis.get().getCreatedOn());
    }

    @Test
    void FindByName_WithExistingName_ReturnsDiagnosis() {
        Diagnosis diagnosis = createDiagnosis("Hypertension", "High blood pressure");
        diagnosisRepository.save(diagnosis);
        entityManager.flush();

        Optional<Diagnosis> foundDiagnosis = diagnosisRepository.findByName("Hypertension");

        assertTrue(foundDiagnosis.isPresent());
        assertEquals("Hypertension", foundDiagnosis.get().getName());
    }

    @Test
    void FindPatientsByDiagnosis_WithMultipleVisits_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. John Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient1 = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Patient patient2 = createPatient("Bob White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient1 = patientRepository.save(patient1);
        patient2 = patientRepository.save(patient2);
        Diagnosis diagnosis = createDiagnosis("Diabetes", "Type 2 diabetes");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient1, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient2, doctor, diagnosis, LocalDate.now(), LocalTime.of(11, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        entityManager.flush();

        Page<PatientDiagnosisDTO> patients = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(2, patients.getTotalElements());
        assertEquals(1, patients.getContent().size());
        assertTrue(patients.getContent().stream().anyMatch(dto -> dto.getPatient().getName().equals("Jane Doe")));
    }

    @Test
    void FindMostFrequentDiagnoses_WithMultipleVisits_ReturnsSorted() {
        Doctor doctor = createDoctor("Dr. Alice Brown", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Tom Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis1 = createDiagnosis("Asthma", "Chronic respiratory condition");
        Diagnosis diagnosis2 = createDiagnosis("Flu", "Influenza infection");
        diagnosis1 = diagnosisRepository.save(diagnosis1);
        diagnosis2 = diagnosisRepository.save(diagnosis2);
        Visit visit1 = createVisit(patient, doctor, diagnosis1, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient, doctor, diagnosis1, LocalDate.now().minusDays(1), LocalTime.of(11, 0), false);
        Visit visit3 = createVisit(patient, doctor, diagnosis2, LocalDate.now().minusDays(2), LocalTime.of(12, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);
        entityManager.flush();

        List<DiagnosisVisitCountDTO> frequentDiagnoses = diagnosisRepository.findMostFrequentDiagnoses();

        assertEquals(2, frequentDiagnoses.size());
        assertEquals("Asthma", frequentDiagnoses.get(0).getDiagnosis().getName());
        assertEquals(2, frequentDiagnoses.get(0).getVisitCount());
        assertEquals("Flu", frequentDiagnoses.get(1).getDiagnosis().getName());
        assertEquals(1, frequentDiagnoses.get(1).getVisitCount());
    }

    @Test
    void FindAllActive_WithMultipleDiagnoses_ReturnsAll() {
        Diagnosis diagnosis1 = createDiagnosis("Migraine", "Severe headache");
        Diagnosis diagnosis2 = createDiagnosis("Bronchitis", "Inflammation of bronchi");
        diagnosisRepository.save(diagnosis1);
        diagnosisRepository.save(diagnosis2);
        entityManager.flush();

        List<Diagnosis> activeDiagnoses = diagnosisRepository.findAllActive();

        assertEquals(2, activeDiagnoses.size());
        assertTrue(activeDiagnoses.stream().anyMatch(d -> d.getName().equals("Migraine")));
        assertTrue(activeDiagnoses.stream().anyMatch(d -> d.getName().equals("Bronchitis")));
    }

    @Test
    void FindAllActivePaged_WithMultipleDiagnoses_ReturnsPaged() {
        Diagnosis diagnosis1 = createDiagnosis("Pneumonia", "Lung infection");
        Diagnosis diagnosis2 = createDiagnosis("Allergy", "Immune response");
        diagnosisRepository.save(diagnosis1);
        diagnosisRepository.save(diagnosis2);
        entityManager.flush();

        Page<Diagnosis> activeDiagnoses = diagnosisRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeDiagnoses.getTotalElements());
        assertEquals(1, activeDiagnoses.getContent().size());
    }

    @Test
    void SoftDelete_WithExistingDiagnosis_SetsIsDeleted() {
        Diagnosis diagnosis = createDiagnosis("Arthritis", "Joint inflammation");
        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        diagnosisRepository.delete(savedDiagnosis);
        entityManager.flush();
        Optional<Diagnosis> deletedDiagnosis = diagnosisRepository.findById(savedDiagnosis.getId());
        if (deletedDiagnosis.isPresent()) {
            entityManager.refresh(deletedDiagnosis.get());
        }

        assertTrue(deletedDiagnosis.isPresent());
        assertTrue(deletedDiagnosis.get().getIsDeleted());
        assertNotNull(deletedDiagnosis.get().getDeletedOn());
        assertEquals(0, diagnosisRepository.findAllActive().size());
    }

    @Test
    void HardDelete_WithExistingDiagnosis_RemovesDiagnosis() {
        Diagnosis diagnosis = createDiagnosis("Gastritis", "Stomach inflammation");
        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        softDeleteRelatedVisits(savedDiagnosis);
        diagnosisRepository.hardDeleteById(savedDiagnosis.getId());
        entityManager.flush();

        assertTrue(diagnosisRepository.findById(savedDiagnosis.getId()).isEmpty());
    }

    // Error Cases
    @Test
    void Save_WithDuplicateName_ThrowsException() {
        Diagnosis diagnosis1 = createDiagnosis("Flu", "Influenza infection");
        Diagnosis diagnosis2 = createDiagnosis("Flu", "Another description");
        diagnosisRepository.save(diagnosis1);
        entityManager.flush();
        assertThrows(DataIntegrityViolationException.class, () -> {
            diagnosisRepository.save(diagnosis2);
            entityManager.flush();
        });
    }

    @Test
    void FindByName_WithNonExistentName_ReturnsEmpty() {
        Optional<Diagnosis> foundDiagnosis = diagnosisRepository.findByName("Nonexistent");

        assertFalse(foundDiagnosis.isPresent());
    }

    @Test
    void FindPatientsByDiagnosis_WithNoVisits_ReturnsEmpty() {
        Diagnosis diagnosis = createDiagnosis("Cancer", "Malignant growth");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        Page<PatientDiagnosisDTO> patients = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(0, patients.getTotalElements());
        assertTrue(patients.getContent().isEmpty());
    }

    @Test
    void FindMostFrequentDiagnoses_WithNoVisits_ReturnsEmpty() {
        Diagnosis diagnosis = createDiagnosis("Hepatitis", "Liver inflammation");
        diagnosisRepository.save(diagnosis);
        entityManager.flush();

        List<DiagnosisVisitCountDTO> frequentDiagnoses = diagnosisRepository.findMostFrequentDiagnoses();

        assertTrue(frequentDiagnoses.isEmpty());
    }

    @Test
    void HardDelete_WithNonExistentId_DoesNotThrow() {
        assertDoesNotThrow(() -> diagnosisRepository.hardDeleteById(999L));
    }

    // Edge Cases
    @Test
    void Save_WithMaximumNameLength_SavesSuccessfully() {
        String maxName = "A".repeat(100); // DIAGNOSIS_NAME_MAX_LENGTH = 100
        Diagnosis diagnosis = createDiagnosis(maxName, "Description");
        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        assertEquals(maxName, savedDiagnosis.getName());
    }

    @Test
    void Save_WithMaximumDescriptionLength_SavesSuccessfully() {
        String maxDescription = "D".repeat(500); // DESCRIPTION_MAX_LENGTH = 500
        Diagnosis diagnosis = createDiagnosis("Cold", maxDescription);
        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();

        assertEquals(maxDescription, savedDiagnosis.getDescription());
    }

    @Test
    void FindAllActive_WithNoDiagnoses_ReturnsEmpty() {
        List<Diagnosis> activeDiagnoses = diagnosisRepository.findAllActive();

        assertTrue(activeDiagnoses.isEmpty());
    }

    @Test
    void FindAllActivePaged_WithNoDiagnoses_ReturnsEmpty() {
        Page<Diagnosis> activeDiagnoses = diagnosisRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, activeDiagnoses.getTotalElements());
        assertTrue(activeDiagnoses.getContent().isEmpty());
    }

    @Test
    void SoftDelete_WithNonExistentDiagnosis_DoesNotThrow() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setId(999L);

        assertDoesNotThrow(() -> diagnosisRepository.delete(diagnosis));
    }

    @Test
    void FindByName_WithSoftDeletedDiagnosis_ReturnsEmpty() {
        Diagnosis diagnosis = createDiagnosis("Tuberculosis", "Bacterial infection");
        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        diagnosisRepository.delete(savedDiagnosis);
        entityManager.flush();

        Optional<Diagnosis> foundDiagnosis = diagnosisRepository.findByName("Tuberculosis");

        assertFalse(foundDiagnosis.isPresent());
    }
}