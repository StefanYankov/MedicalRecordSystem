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
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VisitRepositoryIntegrationTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

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

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate, boolean sickLeaveIssued) {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(visitDate);
        visit.setSickLeaveIssued(sickLeaveIssued);
        return visit;
    }

    // Happy Path
    @Test
    void Save_WithValidVisit_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit savedVisit = visitRepository.save(visit);
        Optional<Visit> foundVisit = visitRepository.findById(savedVisit.getId());

        assertTrue(foundVisit.isPresent());
        assertEquals(LocalDate.now(), foundVisit.get().getVisitDate());
        assertFalse(foundVisit.get().isSickLeaveIssued());
        assertEquals(patient.getId(), foundVisit.get().getPatient().getId());
        assertEquals(doctor.getId(), foundVisit.get().getDoctor().getId());
        assertEquals(diagnosis.getId(), foundVisit.get().getDiagnosis().getId());
        assertFalse(foundVisit.get().getIsDeleted());
        assertNotNull(foundVisit.get().getCreatedOn());
    }

    @Test
    void FindAllActive_WithMultipleVisits_ReturnsAll() {
        Doctor doctor = createDoctor("Dr. Brown", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Bob White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), true);
        visitRepository.save(visit1);
        visitRepository.save(visit2);

        List<Visit> activeVisits = visitRepository.findAllActive();

        assertEquals(2, activeVisits.size());
        assertTrue(activeVisits.stream().anyMatch(v -> v.getVisitDate().equals(LocalDate.now())));
        assertTrue(activeVisits.stream().anyMatch(v -> v.getVisitDate().equals(LocalDate.now().minusDays(1))));
    }

    @Test
    void FindAllActivePaged_WithMultipleVisits_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Green", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Alice Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);

        Page<Visit> activeVisits = visitRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeVisits.getTotalElements());
        assertEquals(1, activeVisits.getContent().size());
    }

    @Test
    void FindByPatient_WithMultipleVisits_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Taylor", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient1 = createPatient("Tom Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Patient patient2 = createPatient("Sarah Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient1 = patientRepository.save(patient1);
        patient2 = patientRepository.save(patient2);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient1, doctor, diagnosis, LocalDate.now(), false);
        Visit visit2 = createVisit(patient1, doctor, diagnosis, LocalDate.now().minusDays(1), false);
        Visit visit3 = createVisit(patient2, doctor, diagnosis, LocalDate.now(), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);

        Page<Visit> patientVisits = visitRepository.findByPatient(patient1, PageRequest.of(0, 1));

        assertEquals(2, patientVisits.getTotalElements());
        assertEquals(1, patientVisits.getContent().size());
    }

    @Test
    void FindByDoctor_WithMultipleVisits_ReturnsPaged() {
        Doctor doctor1 = createDoctor("Dr. Wilson", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Adams", TestDataUtils.generateUniqueIdNumber(), false);
        doctor1 = doctorRepository.save(doctor1);
        doctor2 = doctorRepository.save(doctor2);
        Patient patient = createPatient("Mike Brown", TestDataUtils.generateValidEgn(), doctor1, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor1, diagnosis, LocalDate.now(), false);
        Visit visit2 = createVisit(patient, doctor1, diagnosis, LocalDate.now().minusDays(1), false);
        Visit visit3 = createVisit(patient, doctor2, diagnosis, LocalDate.now(), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);

        Page<Visit> doctorVisits = visitRepository.findByDoctor(doctor1, PageRequest.of(0, 1));

        assertEquals(2, doctorVisits.getTotalElements());
        assertEquals(1, doctorVisits.getContent().size());
    }

    @Test
    void FindByDateRange_WithMultipleVisits_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Clark", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Emma White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), false);
        Visit visit3 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(10), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);

        Page<Visit> dateRangeVisits = visitRepository.findByDateRange(
                LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(2, dateRangeVisits.getTotalElements());
        assertEquals(1, dateRangeVisits.getContent().size());
    }

    @Test
    void FindByDoctorAndDateRange_WithMultipleVisits_ReturnsPaged() {
        Doctor doctor1 = createDoctor("Dr. Evans", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Lee", TestDataUtils.generateUniqueIdNumber(), false);
        doctor1 = doctorRepository.save(doctor1);
        doctor2 = doctorRepository.save(doctor2);
        Patient patient = createPatient("Olivia Green", TestDataUtils.generateValidEgn(), doctor1, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor1, diagnosis, LocalDate.now(), false);
        Visit visit2 = createVisit(patient, doctor1, diagnosis, LocalDate.now().minusDays(1), false);
        Visit visit3 = createVisit(patient, doctor2, diagnosis, LocalDate.now(), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);

        Page<Visit> doctorDateRangeVisits = visitRepository.findByDoctorAndDateRange(
                doctor1, LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(2, doctorDateRangeVisits.getTotalElements());
        assertEquals(1, doctorDateRangeVisits.getContent().size());
    }

    @Test
    void SoftDelete_WithExistingVisit_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Taylor", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Tom Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Hypertension", "High blood pressure");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit savedVisit = visitRepository.save(visit);

        visitRepository.delete(savedVisit);
        entityManager.flush();
        Optional<Visit> deletedVisit = visitRepository.findById(savedVisit.getId());
        if (deletedVisit.isPresent()) {
            entityManager.refresh(deletedVisit.get());
        }

        assertTrue(deletedVisit.isPresent());
        assertTrue(deletedVisit.get().getIsDeleted());
        assertNotNull(deletedVisit.get().getDeletedOn());
        assertEquals(0, visitRepository.findAllActive().size());
    }

    @Test
    void HardDelete_WithExistingVisit_RemovesVisit() {
        Doctor doctor = createDoctor("Dr. Wilson", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Sarah Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Hyperlipidemia", "High cholesterol");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit savedVisit = visitRepository.save(visit);
        entityManager.flush();

        visitRepository.hardDeleteById(savedVisit.getId());
        entityManager.flush();

        assertTrue(visitRepository.findById(savedVisit.getId()).isEmpty());
    }

    // Error Cases
    @Test
    void HardDelete_WithNonExistentId_DoesNotThrow() {
        assertDoesNotThrow(() -> visitRepository.hardDeleteById(999L));
    }

    @Test
    void FindByPatient_WithNoVisits_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Adams", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Mike Brown", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);

        Page<Visit> patientVisits = visitRepository.findByPatient(patient, PageRequest.of(0, 1));

        assertEquals(0, patientVisits.getTotalElements());
        assertTrue(patientVisits.getContent().isEmpty());
    }

    @Test
    void FindByDoctor_WithNoVisits_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Clark", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);

        Page<Visit> doctorVisits = visitRepository.findByDoctor(doctor, PageRequest.of(0, 1));

        assertEquals(0, doctorVisits.getTotalElements());
        assertTrue(doctorVisits.getContent().isEmpty());
    }

    @Test
    void FindByDateRange_WithNoVisits_ReturnsEmpty() {
        Page<Visit> dateRangeVisits = visitRepository.findByDateRange(
                LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(0, dateRangeVisits.getTotalElements());
        assertTrue(dateRangeVisits.getContent().isEmpty());
    }

    @Test
    void FindByDoctorAndDateRange_WithNoVisits_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Evans", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);

        Page<Visit> doctorDateRangeVisits = visitRepository.findByDoctorAndDateRange(
                doctor, LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(0, doctorDateRangeVisits.getTotalElements());
        assertTrue(doctorDateRangeVisits.getContent().isEmpty());
    }

    // Edge Cases
    @Test
    void Save_WithSickLeaveIssuedTrue_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Lee", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Olivia Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Pneumonia", "Lung infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), true);

        Visit savedVisit = visitRepository.save(visit);

        assertTrue(savedVisit.isSickLeaveIssued());
    }

    @Test
    void FindByPatient_WithSoftDeletedVisit_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit savedVisit = visitRepository.save(visit);
        visitRepository.delete(savedVisit);

        Page<Visit> patientVisits = visitRepository.findByPatient(patient, PageRequest.of(0, 1));

        assertEquals(0, patientVisits.getTotalElements());
        assertTrue(patientVisits.getContent().isEmpty());
    }

    @Test
    void FindByDoctor_WithSoftDeletedVisit_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Brown", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Bob White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit savedVisit = visitRepository.save(visit);
        visitRepository.delete(savedVisit);

        Page<Visit> doctorVisits = visitRepository.findByDoctor(doctor, PageRequest.of(0, 1));

        assertEquals(0, doctorVisits.getTotalElements());
        assertTrue(doctorVisits.getContent().isEmpty());
    }

    @Test
    void FindByDateRange_WithSoftDeletedVisit_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Green", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Alice Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit savedVisit = visitRepository.save(visit);
        visitRepository.delete(savedVisit);

        Page<Visit> dateRangeVisits = visitRepository.findByDateRange(
                LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(0, dateRangeVisits.getTotalElements());
        assertTrue(dateRangeVisits.getContent().isEmpty());
    }

    @Test
    void FindByDoctorAndDateRange_WithSoftDeletedVisit_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Taylor", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Tom Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Hypertension", "High blood pressure");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), false);
        Visit savedVisit = visitRepository.save(visit);
        visitRepository.delete(savedVisit);

        Page<Visit> doctorDateRangeVisits = visitRepository.findByDoctorAndDateRange(
                doctor, LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(0, doctorDateRangeVisits.getTotalElements());
        assertTrue(doctorDateRangeVisits.getContent().isEmpty());
    }

    @Test
    void FindAllActive_WithNoVisits_ReturnsEmpty() {
        List<Visit> activeVisits = visitRepository.findAllActive();

        assertTrue(activeVisits.isEmpty());
    }

    @Test
    void FindAllActivePaged_WithNoVisits_ReturnsEmpty() {
        Page<Visit> activeVisits = visitRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, activeVisits.getTotalElements());
        assertTrue(activeVisits.getContent().isEmpty());
    }

    @Test
    void SoftDelete_WithNonExistentVisit_DoesNotThrow() {
        Visit visit = new Visit();
        visit.setId(999L);

        assertDoesNotThrow(() -> visitRepository.delete(visit));
    }
}