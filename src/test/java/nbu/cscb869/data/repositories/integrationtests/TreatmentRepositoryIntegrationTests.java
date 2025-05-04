package nbu.cscb869.data.repositories.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TreatmentRepositoryIntegrationTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

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

    @Autowired
    private EntityManager entityManager;

    private static final int[] EGN_WEIGHTS = {2, 4, 8, 5, 10, 9, 7, 3, 6};
    private static final Random RANDOM = new Random();

    @BeforeEach
    void setUp() {
        treatmentRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
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

    private Diagnosis createDiagnosis(String name, String description) {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName(name);
        diagnosis.setDescription(description);
        return diagnosis;
    }

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate) {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(visitDate);
        visit.setSickLeaveIssued(false);
        return visit;
    }

    private Treatment createTreatment(String description, Visit visit) {
        Treatment treatment = new Treatment();
        treatment.setDescription(description);
        treatment.setVisit(visit);
        return treatment;
    }

    private String generateUniqueIdNumber() {
        int length = 5 + (int) (Math.random() * 6); // 5-10 chars
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, length);
    }

    private String generateValidEgn() {
        int year = 2000 + RANDOM.nextInt(26); // 2000–2025
        int month = 1 + RANDOM.nextInt(12); // 1–12
        int day = 1 + RANDOM.nextInt(28); // 1–28 to avoid invalid days
        LocalDate date = LocalDate.of(year, month, day);

        int egnMonth = month + 40;
        String yy = String.format("%02d", year % 100);
        String mm = String.format("%02d", egnMonth);
        String dd = String.format("%02d", day);

        String region = String.format("%03d", RANDOM.nextInt(1000));

        String baseEgn = yy + mm + dd + region;
        int[] digits = baseEgn.chars().map(c -> c - '0').toArray();
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += digits[i] * EGN_WEIGHTS[i];
        }
        int checksum = sum % 11;
        if (checksum == 10) {
            checksum = 0;
        }

        return baseEgn + checksum;
    }

    // Happy Path
    @Test
    void Save_WithValidTreatment_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Smith", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Antibiotic therapy", visit);
        Treatment savedTreatment = treatmentRepository.save(treatment);
        Optional<Treatment> foundTreatment = treatmentRepository.findById(savedTreatment.getId());

        assertTrue(foundTreatment.isPresent());
        assertEquals("Antibiotic therapy", foundTreatment.get().getDescription());
        assertEquals(visit.getId(), foundTreatment.get().getVisit().getId());
        assertFalse(foundTreatment.get().getIsDeleted());
        assertNotNull(foundTreatment.get().getCreatedOn());
    }

    @Test
    void FindAllActive_WithMultipleTreatments_ReturnsAll() {
        Doctor doctor = createDoctor("Dr. Brown", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Bob White", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now());
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1));
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        Treatment treatment1 = createTreatment("Pain relief", visit1);
        Treatment treatment2 = createTreatment("Cough syrup", visit2);
        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);

        List<Treatment> activeTreatments = treatmentRepository.findAllActive();

        assertEquals(2, activeTreatments.size());
        assertTrue(activeTreatments.stream().anyMatch(t -> t.getDescription().equals("Pain relief")));
        assertTrue(activeTreatments.stream().anyMatch(t -> t.getDescription().equals("Cough syrup")));
    }

    @Test
    void FindAllActivePaged_WithMultipleTreatments_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Green", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Alice Lee", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now());
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1));
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        Treatment treatment1 = createTreatment("Antibiotic course", visit1);
        Treatment treatment2 = createTreatment("Inhaler therapy", visit2);
        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);

        Page<Treatment> activeTreatments = treatmentRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeTreatments.getTotalElements());
        assertEquals(1, activeTreatments.getContent().size());
    }

    @Test
    void SoftDelete_WithExistingTreatment_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Taylor", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Tom Green", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Hypertension", "High blood pressure");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Hypertension management", visit);
        Treatment savedTreatment = treatmentRepository.save(treatment);

        treatmentRepository.delete(savedTreatment);
        entityManager.flush();
        Optional<Treatment> deletedTreatment = treatmentRepository.findById(savedTreatment.getId());
        if (deletedTreatment.isPresent()) {
            entityManager.refresh(deletedTreatment.get());
        }

        assertTrue(deletedTreatment.isPresent());
        assertTrue(deletedTreatment.get().getIsDeleted());
        assertNotNull(deletedTreatment.get().getDeletedOn());
        assertEquals(0, treatmentRepository.findAllActive().size());
    }

    @Test
    void HardDelete_WithExistingTreatment_RemovesTreatment() {
        Doctor doctor = createDoctor("Dr. Wilson", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Sarah Lee", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Hyperlipidemia", "High cholesterol");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Cholesterol management", visit);
        Treatment savedTreatment = treatmentRepository.save(treatment);
        entityManager.flush();

        treatmentRepository.hardDeleteById(savedTreatment.getId());
        entityManager.flush();

        assertTrue(treatmentRepository.findById(savedTreatment.getId()).isEmpty());
    }

    // Error Cases
    @Test
    void HardDelete_WithNonExistentId_DoesNotThrow() {
        assertDoesNotThrow(() -> treatmentRepository.hardDeleteById(999L));
    }

    // Edge Cases
    @Test
    void Save_WithMaximumDescriptionLength_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Adams", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Mike Brown", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Migraine", "Severe headache");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        String maxDescription = "D".repeat(500); // DESCRIPTION_MAX_LENGTH = 500
        Treatment treatment = createTreatment(maxDescription, visit);

        Treatment savedTreatment = treatmentRepository.save(treatment);

        assertEquals(maxDescription, savedTreatment.getDescription());
    }

    @Test
    void Save_WithNullDescription_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Clark", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Emma White", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("GERD", "Acid reflux");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment(null, visit);

        Treatment savedTreatment = treatmentRepository.save(treatment);

        assertNull(savedTreatment.getDescription());
    }

    @Test
    void FindAllActive_WithNoTreatments_ReturnsEmpty() {
        List<Treatment> activeTreatments = treatmentRepository.findAllActive();

        assertTrue(activeTreatments.isEmpty());
    }

    @Test
    void FindAllActivePaged_WithNoTreatments_ReturnsEmpty() {
        Page<Treatment> activeTreatments = treatmentRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, activeTreatments.getTotalElements());
        assertTrue(activeTreatments.getContent().isEmpty());
    }

    @Test
    void SoftDelete_WithNonExistentTreatment_DoesNotThrow() {
        Treatment treatment = new Treatment();
        treatment.setId(999L);

        assertDoesNotThrow(() -> treatmentRepository.delete(treatment));
    }
}