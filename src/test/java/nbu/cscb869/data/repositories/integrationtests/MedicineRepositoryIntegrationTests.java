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
class MedicineRepositoryIntegrationTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MedicineRepository medicineRepository;

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
        medicineRepository.deleteAll();
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

    private Medicine createMedicine(String name, String dosage, String frequency, Treatment treatment) {
        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setDosage(dosage);
        medicine.setFrequency(frequency);
        medicine.setTreatment(treatment);
        return medicine;
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
    void Save_WithValidMedicine_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Smith", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Antibiotic therapy", visit);
        treatment = treatmentRepository.save(treatment);
        Medicine medicine = createMedicine("Aspirin", "500mg", "Once daily", treatment);
        Medicine savedMedicine = medicineRepository.save(medicine);
        Optional<Medicine> foundMedicine = medicineRepository.findById(savedMedicine.getId());

        assertTrue(foundMedicine.isPresent());
        assertEquals("Aspirin", foundMedicine.get().getName());
        assertEquals("500mg", foundMedicine.get().getDosage());
        assertEquals("Once daily", foundMedicine.get().getFrequency());
        assertEquals(treatment.getId(), foundMedicine.get().getTreatment().getId());
        assertFalse(foundMedicine.get().getIsDeleted());
        assertNotNull(foundMedicine.get().getCreatedOn());
    }

    @Test
    void FindAllActive_WithMultipleMedicines_ReturnsAll() {
        Doctor doctor = createDoctor("Dr. Brown", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Bob White", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Pain relief", visit);
        treatment = treatmentRepository.save(treatment);
        Medicine medicine1 = createMedicine("Ibuprofen", "200mg", "Twice daily", treatment);
        Medicine medicine2 = createMedicine("Paracetamol", "500mg", "As needed", treatment);
        medicineRepository.save(medicine1);
        medicineRepository.save(medicine2);

        List<Medicine> activeMedicines = medicineRepository.findAllActive();

        assertEquals(2, activeMedicines.size());
        assertTrue(activeMedicines.stream().anyMatch(m -> m.getName().equals("Ibuprofen")));
        assertTrue(activeMedicines.stream().anyMatch(m -> m.getName().equals("Paracetamol")));
    }

    @Test
    void FindAllActivePaged_WithMultipleMedicines_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Green", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Alice Lee", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Antibiotic course", visit);
        treatment = treatmentRepository.save(treatment);
        Medicine medicine1 = createMedicine("Amoxicillin", "250mg", "Three times daily", treatment);
        Medicine medicine2 = createMedicine("Metformin", "500mg", "Twice daily", treatment);
        medicineRepository.save(medicine1);
        medicineRepository.save(medicine2);

        Page<Medicine> activeMedicines = medicineRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeMedicines.getTotalElements());
        assertEquals(1, activeMedicines.getContent().size());
    }

    @Test
    void SoftDelete_WithExistingMedicine_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Taylor", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Tom Green", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Hypertension", "High blood pressure");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Hypertension management", visit);
        treatment = treatmentRepository.save(treatment);
        Medicine medicine = createMedicine("Lisinopril", "10mg", "Once daily", treatment);
        Medicine savedMedicine = medicineRepository.save(medicine);

        medicineRepository.delete(savedMedicine);
        entityManager.flush();
        Optional<Medicine> deletedMedicine = medicineRepository.findById(savedMedicine.getId());
        if (deletedMedicine.isPresent()) {
            entityManager.refresh(deletedMedicine.get());
        }

        assertTrue(deletedMedicine.isPresent());
        assertTrue(deletedMedicine.get().getIsDeleted());
        assertNotNull(deletedMedicine.get().getDeletedOn());
        assertEquals(0, medicineRepository.findAllActive().size());
    }

    @Test
    void HardDelete_WithExistingMedicine_RemovesMedicine() {
        Doctor doctor = createDoctor("Dr. Wilson", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Sarah Lee", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Hyperlipidemia", "High cholesterol");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Cholesterol management", visit);
        treatment = treatmentRepository.save(treatment);
        Medicine medicine = createMedicine("Atorvastatin", "20mg", "Once daily", treatment);
        Medicine savedMedicine = medicineRepository.save(medicine);
        entityManager.flush();

        medicineRepository.hardDeleteById(savedMedicine.getId());
        entityManager.flush();

        assertTrue(medicineRepository.findById(savedMedicine.getId()).isEmpty());
    }

    // Error Cases
    @Test
    void HardDelete_WithNonExistentId_DoesNotThrow() {
        assertDoesNotThrow(() -> medicineRepository.hardDeleteById(999L));
    }

    // Edge Cases
    @Test
    void Save_WithMaximumNameLength_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Adams", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Mike Brown", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Migraine", "Severe headache");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Pain management", visit);
        treatment = treatmentRepository.save(treatment);
        String maxName = "A".repeat(100); // NAME_MAX_LENGTH = 100
        Medicine medicine = createMedicine(maxName, "100mg", "Once daily", treatment);

        Medicine savedMedicine = medicineRepository.save(medicine);

        assertEquals(maxName, savedMedicine.getName());
    }

    @Test
    void Save_WithMaximumDosageLength_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Clark", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Emma White", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("GERD", "Acid reflux");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Acid reflux treatment", visit);
        treatment = treatmentRepository.save(treatment);
        String maxDosage = "D".repeat(50); // DOSAGE_MAX_LENGTH = 50
        Medicine medicine = createMedicine("Omeprazole", maxDosage, "Once daily", treatment);

        Medicine savedMedicine = medicineRepository.save(medicine);

        assertEquals(maxDosage, savedMedicine.getDosage());
    }

    @Test
    void Save_WithMaximumFrequencyLength_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Evans", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Olivia Green", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Hypothyroidism", "Thyroid deficiency");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Thyroid treatment", visit);
        treatment = treatmentRepository.save(treatment);
        String maxFrequency = "F".repeat(100); // FREQUENCY_MAX_LENGTH = 100
        Medicine medicine = createMedicine("Levothyroxine", "50mcg", maxFrequency, treatment);

        Medicine savedMedicine = medicineRepository.save(medicine);

        assertEquals(maxFrequency, savedMedicine.getFrequency());
    }

    @Test
    void FindAllActive_WithNoMedicines_ReturnsEmpty() {
        List<Medicine> activeMedicines = medicineRepository.findAllActive();

        assertTrue(activeMedicines.isEmpty());
    }

    @Test
    void FindAllActivePaged_WithNoMedicines_ReturnsEmpty() {
        Page<Medicine> activeMedicines = medicineRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, activeMedicines.getTotalElements());
        assertTrue(activeMedicines.getContent().isEmpty());
    }

    @Test
    void SoftDelete_WithNonExistentMedicine_DoesNotThrow() {
        Medicine medicine = new Medicine();
        medicine.setId(999L);

        assertDoesNotThrow(() -> medicineRepository.delete(medicine));
    }
}