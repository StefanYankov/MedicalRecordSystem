package nbu.cscb869.data.repositories.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.common.validation.ValidationConfig;
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
    private MedicineRepository medicineRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
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
    void Save_WithValidTreatment_SavesSuccessfully() {
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
        Treatment savedTreatment = treatmentRepository.save(treatment);
        entityManager.flush();
        Optional<Treatment> foundTreatment = treatmentRepository.findById(savedTreatment.getId());

        assertTrue(foundTreatment.isPresent());
        assertEquals("Antibiotic therapy", foundTreatment.get().getDescription());
        assertEquals(visit.getId(), foundTreatment.get().getVisit().getId());
        assertFalse(foundTreatment.get().getIsDeleted());
        assertNotNull(foundTreatment.get().getCreatedOn());
    }

    @Test
    void Save_WithMedicines_SavesSuccessfully() {
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
        Medicine medicine = createMedicine("Paracetamol", "500mg", "Twice daily", treatment);
        treatment.setMedicines(List.of(medicine));
        Treatment savedTreatment = treatmentRepository.save(treatment);
        entityManager.flush();
        Optional<Treatment> foundTreatment = treatmentRepository.findById(savedTreatment.getId());

        assertTrue(foundTreatment.isPresent());
        assertEquals("Pain relief", foundTreatment.get().getDescription());
        assertEquals(1, foundTreatment.get().getMedicines().size());
        assertEquals("Paracetamol", foundTreatment.get().getMedicines().getFirst().getName());
    }

    @Test
    void FindAllActive_WithMultipleTreatments_ReturnsAll() {
        Doctor doctor = createDoctor("Dr. Green", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Alice Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0));
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        entityManager.flush();
        Treatment treatment1 = createTreatment("Antibiotic course", visit1);
        Treatment treatment2 = createTreatment("Inhaler therapy", visit2);
        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);
        entityManager.flush();

        List<Treatment> activeTreatments = treatmentRepository.findAllActive();

        assertEquals(2, activeTreatments.size());
        assertTrue(activeTreatments.stream().anyMatch(t -> t.getDescription().equals("Antibiotic course")));
        assertTrue(activeTreatments.stream().anyMatch(t -> t.getDescription().equals("Inhaler therapy")));
    }

    @Test
    void FindAllActivePaged_WithMultipleTreatments_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Taylor", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Tom Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Hypertension", "High blood pressure");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0));
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        entityManager.flush();
        Treatment treatment1 = createTreatment("Hypertension management", visit1);
        Treatment treatment2 = createTreatment("Dietary advice", visit2);
        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);
        entityManager.flush();

        Page<Treatment> activeTreatments = treatmentRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeTreatments.getTotalElements());
        assertEquals(1, activeTreatments.getContent().size());
    }

    @Test
    void FindAllActivePaged_WithLastPageFewerElements_ReturnsCorrectPage() {
        Doctor doctor = createDoctor("Dr. Wilson", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Sarah Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Hyperlipidemia", "High cholesterol");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0));
        Visit visit3 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(2), LocalTime.of(10, 30));
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        visit3 = visitRepository.save(visit3);
        entityManager.flush();
        Treatment treatment1 = createTreatment("Cholesterol management", visit1);
        Treatment treatment2 = createTreatment("Statin therapy", visit2);
        Treatment treatment3 = createTreatment("Lifestyle changes", visit3);
        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);
        treatmentRepository.save(treatment3);
        entityManager.flush();

        Page<Treatment> activeTreatments = treatmentRepository.findAllActive(PageRequest.of(1, 2));

        assertEquals(3, activeTreatments.getTotalElements());
        assertEquals(1, activeTreatments.getContent().size());
        assertEquals("Lifestyle changes", activeTreatments.getContent().getFirst().getDescription());
        assertEquals(2, activeTreatments.getTotalPages());
    }

    @Test
    void SoftDelete_WithExistingTreatment_SetsIsDeleted() {
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
        Treatment savedTreatment = treatmentRepository.save(treatment);
        entityManager.flush();

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
        Treatment treatment = createTreatment("Antacid therapy", visit);
        Treatment savedTreatment = treatmentRepository.save(treatment);
        entityManager.flush();

        treatmentRepository.hardDeleteById(savedTreatment.getId());
        entityManager.flush();

        assertTrue(treatmentRepository.findById(savedTreatment.getId()).isEmpty());
    }

    // Error Cases
    @Test
    void Save_WithNullVisit_ThrowsException() {
        Treatment treatment = createTreatment("Invalid therapy", null);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            treatmentRepository.save(treatment);
            entityManager.flush();
        });
    }

    @Test
    void Save_WithOverMaxDescription_ThrowsException() {
        Doctor doctor = createDoctor("Dr. Evans", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Olivia Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Asthma", "Respiratory condition");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        String overMaxDescription = "D".repeat(ValidationConfig.DESCRIPTION_MAX_LENGTH + 1);
        Treatment treatment = createTreatment(overMaxDescription, visit);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            treatmentRepository.save(treatment);
            entityManager.flush();
        });
    }

    @Test
    void HardDelete_WithNonExistentId_DoesNotThrow() {
        assertDoesNotThrow(() -> treatmentRepository.hardDeleteById(999L));
    }

    // Edge Cases
    @Test
    void Save_WithMaximumDescriptionLength_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Harris", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Liam Brown", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Allergy", "Hypersensitivity");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        String maxDescription = "D".repeat(ValidationConfig.DESCRIPTION_MAX_LENGTH);
        Treatment treatment = createTreatment(maxDescription, visit);
        Treatment savedTreatment = treatmentRepository.save(treatment);
        entityManager.flush();

        assertEquals(maxDescription, savedTreatment.getDescription());
    }

    @Test
    void Save_WithNullDescription_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Harris", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Liam Brown", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Allergy", "Hypersensitivity");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        Treatment treatment = createTreatment(null, visit);
        Treatment savedTreatment = treatmentRepository.save(treatment);
        entityManager.flush();

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