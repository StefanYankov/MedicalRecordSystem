package nbu.cscb869.data.repositories.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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

    @BeforeEach
    void setUp() {
        medicineRepository.deleteAll();
        treatmentRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
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

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate, LocalTime visitTime) {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(visitDate);
        visit.setVisitTime(visitTime);
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

    // Happy Path
    @Test
    void Save_WithValidMedicine_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        Treatment treatment = createTreatment("Antibiotic therapy", visit);
        treatment = treatmentRepository.save(treatment);
        entityManager.flush();
        Medicine medicine = createMedicine("Aspirin", "500mg", "Once daily", treatment);
        Medicine savedMedicine = medicineRepository.save(medicine);
        entityManager.flush();
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
        Doctor doctor = createDoctor("Dr. Brown", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Bob White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        Treatment treatment = createTreatment("Pain relief", visit);
        treatment = treatmentRepository.save(treatment);
        entityManager.flush();
        Medicine medicine1 = createMedicine("Ibuprofen", "200mg", "Twice daily", treatment);
        Medicine medicine2 = createMedicine("Paracetamol", "500mg", "As needed", treatment);
        medicineRepository.save(medicine1);
        medicineRepository.save(medicine2);
        entityManager.flush();

        List<Medicine> activeMedicines = medicineRepository.findAllActive();

        assertEquals(2, activeMedicines.size());
        assertTrue(activeMedicines.stream().anyMatch(m -> m.getName().equals("Ibuprofen")));
        assertTrue(activeMedicines.stream().anyMatch(m -> m.getName().equals("Paracetamol")));
    }

    @Test
    void FindAllActivePaged_WithMultipleMedicines_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Green", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Alice Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        Treatment treatment = createTreatment("Antibiotic course", visit);
        treatment = treatmentRepository.save(treatment);
        entityManager.flush();
        Medicine medicine1 = createMedicine("Amoxicillin", "250mg", "Three times daily", treatment);
        Medicine medicine2 = createMedicine("Metformin", "500mg", "Twice daily", treatment);
        medicineRepository.save(medicine1);
        medicineRepository.save(medicine2);
        entityManager.flush();

        Page<Medicine> activeMedicines = medicineRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeMedicines.getTotalElements());
        assertEquals(1, activeMedicines.getContent().size());
    }

    @Test
    void SoftDelete_WithExistingMedicine_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Taylor", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Tom Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Hypertension", "High blood pressure");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        Treatment treatment = createTreatment("Hypertension management", visit);
        treatment = treatmentRepository.save(treatment);
        entityManager.flush();
        Medicine medicine = createMedicine("Lisinopril", "10mg", "Once daily", treatment);
        Medicine savedMedicine = medicineRepository.save(medicine);
        entityManager.flush();

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
        Doctor doctor = createDoctor("Dr. Wilson", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Sarah Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Hyperlipidemia", "High cholesterol");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        Treatment treatment = createTreatment("Cholesterol management", visit);
        treatment = treatmentRepository.save(treatment);
        entityManager.flush();
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
        Doctor doctor = createDoctor("Dr. Adams", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Mike Brown", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Migraine", "Severe headache");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        Treatment treatment = createTreatment("Pain management", visit);
        treatment = treatmentRepository.save(treatment);
        entityManager.flush();
        String maxName = "A".repeat(100);
        Medicine medicine = createMedicine(maxName, "100mg", "Once daily", treatment);

        Medicine savedMedicine = medicineRepository.save(medicine);
        entityManager.flush();

        assertEquals(maxName, savedMedicine.getName());
    }

    @Test
    void Save_WithMaximumDosageLength_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Clark", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Emma White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("GERD", "Acid reflux");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        Treatment treatment = createTreatment("Acid reflux treatment", visit);
        treatment = treatmentRepository.save(treatment);
        entityManager.flush();
        String maxDosage = "D".repeat(50);
        Medicine medicine = createMedicine("Omeprazole", maxDosage, "Once daily", treatment);

        Medicine savedMedicine = medicineRepository.save(medicine);
        entityManager.flush();

        assertEquals(maxDosage, savedMedicine.getDosage());
    }

    @Test
    void Save_WithMaximumFrequencyLength_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Evans", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Olivia Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Hypothyroidism", "Thyroid deficiency");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        Treatment treatment = createTreatment("Thyroid treatment", visit);
        treatment = treatmentRepository.save(treatment);
        entityManager.flush();
        String maxFrequency = "F".repeat(100);
        Medicine medicine = createMedicine("Levothyroxine", "50mcg", maxFrequency, treatment);

        Medicine savedMedicine = medicineRepository.save(medicine);
        entityManager.flush();

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