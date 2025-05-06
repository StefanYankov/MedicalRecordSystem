package nbu.cscb869.data.repositories.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
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
class SickLeaveRepositoryIntegrationTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private SickLeaveRepository sickLeaveRepository;

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
        sickLeaveRepository.deleteAll();
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
        visit.setSickLeaveIssued(true);
        return visit;
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
    void Save_WithValidSickLeave_SavesSuccessfully() {
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
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 5, visit);
        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);
        entityManager.flush();
        Optional<SickLeave> foundSickLeave = sickLeaveRepository.findById(savedSickLeave.getId());

        assertTrue(foundSickLeave.isPresent());
        assertEquals(LocalDate.now(), foundSickLeave.get().getStartDate());
        assertEquals(5, foundSickLeave.get().getDurationDays());
        assertEquals(visit.getId(), foundSickLeave.get().getVisit().getId());
        assertFalse(foundSickLeave.get().getIsDeleted());
        assertNotNull(foundSickLeave.get().getCreatedOn());
    }

    @Test
    void FindAllActive_WithMultipleSickLeaves_ReturnsAll() {
        Doctor doctor = createDoctor("Dr. Brown", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Bob White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0));
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        entityManager.flush();
        SickLeave sickLeave1 = createSickLeave(LocalDate.now(), 3, visit1);
        SickLeave sickLeave2 = createSickLeave(LocalDate.now().minusDays(1), 7, visit2);
        sickLeaveRepository.save(sickLeave1);
        sickLeaveRepository.save(sickLeave2);
        entityManager.flush();

        List<SickLeave> activeSickLeaves = sickLeaveRepository.findAllActive();

        assertEquals(2, activeSickLeaves.size());
        assertTrue(activeSickLeaves.stream().anyMatch(s -> s.getDurationDays() == 3));
        assertTrue(activeSickLeaves.stream().anyMatch(s -> s.getDurationDays() == 7));
    }

    @Test
    void FindAllActivePaged_WithMultipleSickLeaves_ReturnsPaged() {
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
        SickLeave sickLeave1 = createSickLeave(LocalDate.now(), 5, visit1);
        SickLeave sickLeave2 = createSickLeave(LocalDate.now().minusDays(1), 10, visit2);
        sickLeaveRepository.save(sickLeave1);
        sickLeaveRepository.save(sickLeave2);
        entityManager.flush();

        Page<SickLeave> activeSickLeaves = sickLeaveRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeSickLeaves.getTotalElements());
        assertEquals(1, activeSickLeaves.getContent().size());
    }

    @Test
    void FindYearMonthWithMostSickLeaves_WithMultipleSickLeaves_ReturnsSorted() {
        Doctor doctor = createDoctor("Dr. Taylor", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Tom Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(11, 0));
        Visit visit3 = createVisit(patient, doctor, diagnosis, LocalDate.of(2024, 1, 1), LocalTime.of(10, 30));
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        visit3 = visitRepository.save(visit3);
        entityManager.flush();
        SickLeave sickLeave1 = createSickLeave(LocalDate.of(2025, 5, 1), 5, visit1);
        SickLeave sickLeave2 = createSickLeave(LocalDate.of(2025, 5, 2), 7, visit2);
        SickLeave sickLeave3 = createSickLeave(LocalDate.of(2024, 1, 1), 3, visit3);
        sickLeaveRepository.save(sickLeave1);
        sickLeaveRepository.save(sickLeave2);
        sickLeaveRepository.save(sickLeave3);
        entityManager.flush();

        List<YearMonthSickLeaveCountDTO> result = sickLeaveRepository.findYearMonthWithMostSickLeaves();

        assertEquals(2, result.size());
        assertEquals(2025, result.get(0).getYear());
        assertEquals(5, result.get(0).getMonth());
        assertEquals(2, result.get(0).getCount());
        assertEquals(2024, result.get(1).getYear());
        assertEquals(1, result.get(1).getMonth());
        assertEquals(1, result.get(1).getCount());
    }

    @Test
    void FindDoctorsWithMostSickLeaves_WithMultipleSickLeaves_ReturnsSorted() {
        Doctor doctor1 = createDoctor("Dr. Wilson", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Adams", TestDataUtils.generateUniqueIdNumber(), true);
        doctor1 = doctorRepository.save(doctor1);
        doctor2 = doctorRepository.save(doctor2);
        entityManager.flush();
        Patient patient = createPatient("Sarah Lee", TestDataUtils.generateValidEgn(), doctor1, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Pneumonia", "Lung infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit1 = createVisit(patient, doctor1, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        Visit visit2 = createVisit(patient, doctor1, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0));
        Visit visit3 = createVisit(patient, doctor2, diagnosis, LocalDate.now().minusDays(2), LocalTime.of(10, 30));
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        visit3 = visitRepository.save(visit3);
        entityManager.flush();
        SickLeave sickLeave1 = createSickLeave(LocalDate.now(), 14, visit1);
        SickLeave sickLeave2 = createSickLeave(LocalDate.now().minusDays(1), 7, visit2);
        SickLeave sickLeave3 = createSickLeave(LocalDate.now().minusDays(2), 5, visit3);
        sickLeaveRepository.save(sickLeave1);
        sickLeaveRepository.save(sickLeave2);
        sickLeaveRepository.save(sickLeave3);
        entityManager.flush();

        List<DoctorSickLeaveCountDTO> result = sickLeaveRepository.findDoctorsWithMostSickLeaves();

        assertEquals(2, result.size());
        assertEquals("Dr. Wilson", result.get(0).getDoctor().getName());
        assertEquals(2, result.get(0).getSickLeaveCount());
        assertEquals("Dr. Adams", result.get(1).getDoctor().getName());
        assertEquals(1, result.get(1).getSickLeaveCount());
    }

    @Test
    void SoftDelete_WithExistingSickLeave_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Wilson", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Sarah Lee", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Pneumonia", "Lung infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 14, visit);
        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);
        entityManager.flush();

        sickLeaveRepository.delete(savedSickLeave);
        entityManager.flush();
        Optional<SickLeave> deletedSickLeave = sickLeaveRepository.findById(savedSickLeave.getId());
        if (deletedSickLeave.isPresent()) {
            entityManager.refresh(deletedSickLeave.get());
        }

        assertTrue(deletedSickLeave.isPresent());
        assertTrue(deletedSickLeave.get().getIsDeleted());
        assertNotNull(deletedSickLeave.get().getDeletedOn());
        assertEquals(0, sickLeaveRepository.findAllActive().size());
    }

    @Test
    void HardDelete_WithExistingSickLeave_RemovesSickLeave() {
        Doctor doctor = createDoctor("Dr. Adams", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Mike Brown", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Influenza", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 7, visit);
        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);
        entityManager.flush();

        sickLeaveRepository.hardDeleteById(savedSickLeave.getId());
        entityManager.flush();

        assertTrue(sickLeaveRepository.findById(savedSickLeave.getId()).isEmpty());
    }

    // Error Cases
    @Test
    void HardDelete_WithNonExistentId_DoesNotThrow() {
        assertDoesNotThrow(() -> sickLeaveRepository.hardDeleteById(999L));
    }

    @Test
    void FindYearMonthWithMostSickLeaves_WithNoSickLeaves_ReturnsEmpty() {
        List<YearMonthSickLeaveCountDTO> result = sickLeaveRepository.findYearMonthWithMostSickLeaves();

        assertTrue(result.isEmpty());
    }

    // Edge Cases
    @Test
    void Save_WithMinimumDuration_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Clark", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Emma White", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Sprain", "Ankle injury");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 1, visit);

        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);
        entityManager.flush();

        assertEquals(1, savedSickLeave.getDurationDays());
    }

    @Test
    void Save_WithMaximumDuration_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Evans", TestDataUtils.generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        entityManager.flush();
        Patient patient = createPatient("Olivia Green", TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        entityManager.flush();
        Diagnosis diagnosis = createDiagnosis("Fracture", "Bone injury");
        diagnosis = diagnosisRepository.save(diagnosis);
        entityManager.flush();
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30));
        visit = visitRepository.save(visit);
        entityManager.flush();
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 30, visit);

        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);
        entityManager.flush();

        assertEquals(30, savedSickLeave.getDurationDays());
    }

    @Test
    void FindAllActive_WithNoSickLeaves_ReturnsEmpty() {
        List<SickLeave> activeSickLeaves = sickLeaveRepository.findAllActive();

        assertTrue(activeSickLeaves.isEmpty());
    }

    @Test
    void FindAllActivePaged_WithNoSickLeaves_ReturnsEmpty() {
        Page<SickLeave> activeSickLeaves = sickLeaveRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, activeSickLeaves.getTotalElements());
        assertTrue(activeSickLeaves.getContent().isEmpty());
    }

    @Test
    void SoftDelete_WithNonExistentSickLeave_DoesNotThrow() {
        SickLeave sickLeave = new SickLeave();
        sickLeave.setId(999L);

        assertDoesNotThrow(() -> sickLeaveRepository.delete(sickLeave));
    }
}