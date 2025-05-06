package nbu.cscb869.data.repositories.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
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
class VisitRepositoryIntegrationTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
            .withExposedPorts(3306)
            .withEnv("MYSQL_ROOT_PASSWORD", "test");

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private SickLeaveRepository sickLeaveRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        treatmentRepository.deleteAll();
        sickLeaveRepository.deleteAll();
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

    private Treatment createTreatment(String description, Visit visit) {
        Treatment treatment = new Treatment();
        treatment.setDescription(description);
        treatment.setVisit(visit);
        return treatment;
    }

    private SickLeave createSickLeave(LocalDate startDate, int durationDays, Visit visit) {
        SickLeave sickLeave = new SickLeave();
        sickLeave.setStartDate(startDate);
        sickLeave.setDurationDays(durationDays);
        sickLeave.setVisit(visit);
        return sickLeave;
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
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit savedVisit = visitRepository.save(visit);
        entityManager.flush();
        Optional<Visit> foundVisit = visitRepository.findById(savedVisit.getId());

        assertTrue(foundVisit.isPresent());
        assertEquals(LocalDate.now(), foundVisit.get().getVisitDate());
        assertEquals(LocalTime.of(10, 30), foundVisit.get().getVisitTime());
        assertFalse(foundVisit.get().isSickLeaveIssued());
        assertEquals(patient.getId(), foundVisit.get().getPatient().getId());
        assertEquals(doctor.getId(), foundVisit.get().getDoctor().getId());
        assertEquals(diagnosis.getId(), foundVisit.get().getDiagnosis().getId());
        assertFalse(foundVisit.get().getIsDeleted());
        assertNotNull(foundVisit.get().getCreatedOn());
    }

    @Test
    void Save_WithSickLeave_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Brown", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Bob White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), true);
        Visit savedVisit = visitRepository.save(visit);
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 5, savedVisit);
        sickLeaveRepository.save(sickLeave);
        entityManager.flush();
        Optional<Visit> foundVisit = visitRepository.findById(savedVisit.getId());

        assertTrue(foundVisit.isPresent());
        assertTrue(foundVisit.get().isSickLeaveIssued());
        assertNotNull(foundVisit.get().getSickLeave());
        assertEquals(5, foundVisit.get().getSickLeave().getDurationDays());
    }

    @Test
    void Save_WithTreatment_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Green", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Alice Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit savedVisit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Antibiotic therapy", savedVisit);
        treatmentRepository.save(treatment);
        entityManager.flush();
        Optional<Visit> foundVisit = visitRepository.findById(savedVisit.getId());

        assertTrue(foundVisit.isPresent());
        assertNotNull(foundVisit.get().getTreatment());
        assertEquals("Antibiotic therapy", foundVisit.get().getTreatment().getDescription());
    }

    @Test
    void FindAllActive_WithMultipleVisits_ReturnsAll() {
        Doctor doctor = createDoctor("Dr. Brown", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Bob White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), true);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        entityManager.flush();

        List<Visit> activeVisits = visitRepository.findAllActive();

        assertEquals(2, activeVisits.size());
        assertTrue(activeVisits.stream().anyMatch(v -> v.getVisitDate().equals(LocalDate.now()) && v.getVisitTime().equals(LocalTime.of(10, 30))));
        assertTrue(activeVisits.stream().anyMatch(v -> v.getVisitDate().equals(LocalDate.now().minusDays(1)) && v.getVisitTime().equals(LocalTime.of(11, 0))));
    }

    @Test
    void FindAllActivePaged_WithMultipleVisits_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Green", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Alice Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        entityManager.flush();

        Page<Visit> activeVisits = visitRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeVisits.getTotalElements());
        assertEquals(1, activeVisits.getContent().size());
        assertEquals(LocalTime.of(10, 30), activeVisits.getContent().getFirst().getVisitTime());
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
        Visit visit1 = createVisit(patient1, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient1, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), false);
        Visit visit3 = createVisit(patient2, doctor, diagnosis, LocalDate.now(), LocalTime.of(12, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);
        entityManager.flush();

        Page<Visit> patientVisits = visitRepository.findByPatient(patient1, PageRequest.of(0, 1));

        assertEquals(2, patientVisits.getTotalElements());
        assertEquals(1, patientVisits.getContent().size());
        assertEquals(LocalTime.of(10, 30), patientVisits.getContent().getFirst().getVisitTime());
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
        Visit visit1 = createVisit(patient, doctor1, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient, doctor1, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), false);
        Visit visit3 = createVisit(patient, doctor2, diagnosis, LocalDate.now(), LocalTime.of(12, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);
        entityManager.flush();

        Page<Visit> doctorVisits = visitRepository.findByDoctor(doctor1, PageRequest.of(0, 1));

        assertEquals(2, doctorVisits.getTotalElements());
        assertEquals(1, doctorVisits.getContent().size());
        assertEquals(LocalTime.of(10, 30), doctorVisits.getContent().getFirst().getVisitTime());
    }

    @Test
    void FindByDateRange_WithMultipleVisits_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Clark", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Emma White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), false);
        Visit visit3 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(10), LocalTime.of(12, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);
        entityManager.flush();

        Page<Visit> dateRangeVisits = visitRepository.findByDateRange(
                LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(2, dateRangeVisits.getTotalElements());
        assertEquals(1, dateRangeVisits.getContent().size());
        assertEquals(LocalTime.of(10, 30), dateRangeVisits.getContent().getFirst().getVisitTime());
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
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor1, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient, doctor1, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), false);
        Visit visit3 = createVisit(patient, doctor2, diagnosis, LocalDate.now(), LocalTime.of(12, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);
        entityManager.flush();

        Page<Visit> doctorDateRangeVisits = visitRepository.findByDoctorAndDateRange(
                doctor1, LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(2, doctorDateRangeVisits.getTotalElements());
        assertEquals(1, doctorDateRangeVisits.getContent().size());
        assertEquals(LocalTime.of(10, 30), doctorDateRangeVisits.getContent().getFirst().getVisitTime());
    }

    @Test
    void FindByDiagnosis_WithMultipleVisits_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis1 = createDiagnosis("Flu", "Viral infection");
        Diagnosis diagnosis2 = createDiagnosis("Cold", "Common cold");
        diagnosis1 = diagnosisRepository.save(diagnosis1);
        diagnosis2 = diagnosisRepository.save(diagnosis2);
        Visit visit1 = createVisit(patient, doctor, diagnosis1, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient, doctor, diagnosis1, LocalDate.now().minusDays(1), LocalTime.of(11, 0), false);
        Visit visit3 = createVisit(patient, doctor, diagnosis2, LocalDate.now(), LocalTime.of(12, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);
        entityManager.flush();

        Page<Visit> diagnosisVisits = visitRepository.findByDiagnosis(diagnosis1, PageRequest.of(0, 1));

        assertEquals(2, diagnosisVisits.getTotalElements());
        assertEquals(1, diagnosisVisits.getContent().size());
        assertEquals(LocalTime.of(10, 30), diagnosisVisits.getContent().getFirst().getVisitTime());
    }

    @Test
    void FindMostFrequentDiagnoses_WithMultipleVisits_ReturnsList() {
        Doctor doctor = createDoctor("Dr. Brown", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Bob White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis1 = createDiagnosis("Flu", "Viral infection");
        Diagnosis diagnosis2 = createDiagnosis("Cold", "Common cold");
        diagnosis1 = diagnosisRepository.save(diagnosis1);
        diagnosis2 = diagnosisRepository.save(diagnosis2);
        Visit visit1 = createVisit(patient, doctor, diagnosis1, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient, doctor, diagnosis1, LocalDate.now().minusDays(1), LocalTime.of(11, 0), false);
        Visit visit3 = createVisit(patient, doctor, diagnosis2, LocalDate.now(), LocalTime.of(12, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);
        entityManager.flush();

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertEquals(2, result.size());
        assertEquals("Flu", result.getFirst().getDiagnosis().getName());
        assertEquals(2L, result.getFirst().getVisitCount());
        assertEquals("Cold", result.get(1).getDiagnosis().getName());
        assertEquals(1L, result.get(1).getVisitCount());
    }

    @Test
    void CountVisitsByDoctor_WithMultipleVisits_ReturnsList() {
        Doctor doctor1 = createDoctor("Dr. Green", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Adams", TestDataUtils.generateUniqueIdNumber(), false);
        doctor1 = doctorRepository.save(doctor1);
        doctor2 = doctorRepository.save(doctor2);
        Patient patient = createPatient("Alice Lee", TestDataUtils.generateValidEgn(), doctor1, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor1, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit visit2 = createVisit(patient, doctor1, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), false);
        Visit visit3 = createVisit(patient, doctor2, diagnosis, LocalDate.now(), LocalTime.of(12, 0), false);
        visitRepository.save(visit1);
        visitRepository.save(visit2);
        visitRepository.save(visit3);
        entityManager.flush();

        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertEquals(2, result.size());
        assertEquals("Dr. Green", result.getFirst().getDoctor().getName());
        assertEquals(2L, result.getFirst().getVisitCount());
        assertEquals("Dr. Adams", result.get(1).getDoctor().getName());
        assertEquals(1L, result.get(1).getVisitCount());
    }

    @Test
    void FindByPatientOrDoctorFilter_WithPatientName_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        visitRepository.save(visit);
        entityManager.flush();

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter("%Jane%", PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Jane Doe", result.getContent().getFirst().getPatient().getName());
        assertEquals(LocalTime.of(10, 30), result.getContent().getFirst().getVisitTime());
    }

    @Test
    void FindByPatientOrDoctorFilter_WithDoctorUniqueId_ReturnsPaged() {
        Doctor doctor = createDoctor("Dr. Smith", "DOC12345", true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        visitRepository.save(visit);
        entityManager.flush();

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter("%DOC12345%", PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Dr. Smith", result.getContent().getFirst().getDoctor().getName());
        assertEquals(LocalTime.of(10, 30), result.getContent().getFirst().getVisitTime());
    }

    @Test
    void FindByPatientOrDoctorFilter_WithNoMatches_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        visitRepository.save(visit);
        entityManager.flush();

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter("%Nonexistent%", PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void FindByDoctorAndDateTime_WithValidParams_ReturnsVisit() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        visitRepository.save(visit);
        entityManager.flush();

        Optional<Visit> result = visitRepository.findByDoctorAndDateTime(doctor, LocalDate.now(), LocalTime.of(10, 30));

        assertTrue(result.isPresent());
        assertEquals(LocalDate.now(), result.get().getVisitDate());
        assertEquals(LocalTime.of(10, 30), result.get().getVisitTime());
    }

    @Test
    void FindByDoctorAndDateTime_WithNoMatch_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        visitRepository.save(visit);
        entityManager.flush();

        Optional<Visit> result = visitRepository.findByDoctorAndDateTime(doctor, LocalDate.now(), LocalTime.of(11, 0));

        assertFalse(result.isPresent());
    }

    @Test
    void SoftDelete_WithExistingVisit_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Taylor", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Tom Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Hypertension", "High blood pressure");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit savedVisit = visitRepository.save(visit);
        entityManager.flush();

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
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit savedVisit = visitRepository.save(visit);
        entityManager.flush();

        visitRepository.hardDeleteById(savedVisit.getId());
        entityManager.flush();

        assertTrue(visitRepository.findById(savedVisit.getId()).isEmpty());
    }

    // Error Cases
    @Test
    void Save_WithNullVisitDate_ThrowsException() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, null, LocalTime.of(10, 30), false);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            visitRepository.save(visit);
            entityManager.flush();
        });
    }

    @Test
    void Save_WithNullVisitTime_ThrowsException() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), null, false);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            visitRepository.save(visit);
            entityManager.flush();
        });
    }

    @Test
    void Save_WithNullSickLeaveIssued_ThrowsException() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(LocalTime.of(10, 30));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            visitRepository.save(visit);
            entityManager.flush();
        });
    }

    @Test
    void Save_WithNullPatient_ThrowsException() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(null, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            visitRepository.save(visit);
            entityManager.flush();
        });
    }

    @Test
    void Save_WithNullDoctor_ThrowsException() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, null, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            visitRepository.save(visit);
            entityManager.flush();
        });
    }

    @Test
    void Save_WithNullDiagnosis_ThrowsException() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Visit visit = createVisit(patient, doctor, null, LocalDate.now(), LocalTime.of(10, 30), false);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            visitRepository.save(visit);
            entityManager.flush();
        });
    }

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
    void FindByDateRange_WithInvalidRange_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Evans", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Olivia Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        visitRepository.save(visit);
        entityManager.flush();

        Page<Visit> dateRangeVisits = visitRepository.findByDateRange(
                LocalDate.now(), LocalDate.now().minusDays(5), PageRequest.of(0, 1));

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

    @Test
    void FindByDoctorAndDateRange_WithInvalidRange_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Lee", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Olivia Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        visitRepository.save(visit);
        entityManager.flush();

        Page<Visit> doctorDateRangeVisits = visitRepository.findByDoctorAndDateRange(
                doctor, LocalDate.now(), LocalDate.now().minusDays(5), PageRequest.of(0, 1));

        assertEquals(0, doctorDateRangeVisits.getTotalElements());
        assertTrue(doctorDateRangeVisits.getContent().isEmpty());
    }

    @Test
    void FindByDiagnosis_WithNoVisits_ReturnsEmpty() {
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);

        Page<Visit> diagnosisVisits = visitRepository.findByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(0, diagnosisVisits.getTotalElements());
        assertTrue(diagnosisVisits.getContent().isEmpty());
    }

    @Test
    void FindMostFrequentDiagnoses_WithNoVisits_ReturnsEmpty() {
        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertTrue(result.isEmpty());
    }

    @Test
    void CountVisitsByDoctor_WithNoVisits_ReturnsEmpty() {
        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertTrue(result.isEmpty());
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
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), true);
        Visit savedVisit = visitRepository.save(visit);
        entityManager.flush();

        assertTrue(savedVisit.isSickLeaveIssued());
        assertEquals(LocalTime.of(10, 30), savedVisit.getVisitTime());
    }

    @Test
    void Save_WithTimeAtMidnight_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(0, 0), false);
        Visit savedVisit = visitRepository.save(visit);
        entityManager.flush();

        assertEquals(LocalTime.of(0, 0), savedVisit.getVisitTime());
    }

    @Test
    void Save_WithTimeAtEndOfDay_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(23, 59), false);
        Visit savedVisit = visitRepository.save(visit);
        entityManager.flush();

        assertEquals(LocalTime.of(23, 59), savedVisit.getVisitTime());
    }

    @Test
    void FindByPatient_WithSoftDeletedVisit_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit savedVisit = visitRepository.save(visit);
        visitRepository.delete(savedVisit);
        entityManager.flush();

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
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit savedVisit = visitRepository.save(visit);
        visitRepository.delete(savedVisit);
        entityManager.flush();

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
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit savedVisit = visitRepository.save(visit);
        visitRepository.delete(savedVisit);
        entityManager.flush();

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
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), false);
        Visit savedVisit = visitRepository.save(visit);
        visitRepository.delete(savedVisit);
        entityManager.flush();

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