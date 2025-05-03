package nbu.cscb869.data.repositories.unittests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
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
class SickLeaveRepositoryUnitTests {
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

    private static final int[] EGN_WEIGHTS = {2, 4, 8, 5, 10, 9, 7, 3, 6};
    private static final Random RANDOM = new Random();

    @BeforeEach
    void setUp() {
        sickLeaveRepository.deleteAll();
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
    void Save_WithValidSickLeave_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Smith", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 5, visit);
        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);
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
        SickLeave sickLeave1 = createSickLeave(LocalDate.now(), 3, visit1);
        SickLeave sickLeave2 = createSickLeave(LocalDate.now().minusDays(1), 7, visit2);
        sickLeaveRepository.save(sickLeave1);
        sickLeaveRepository.save(sickLeave2);

        List<SickLeave> activeSickLeaves = sickLeaveRepository.findAllActive();

        assertEquals(2, activeSickLeaves.size());
        assertTrue(activeSickLeaves.stream().anyMatch(s -> s.getDurationDays() == 3));
        assertTrue(activeSickLeaves.stream().anyMatch(s -> s.getDurationDays() == 7));
    }

    @Test
    void FindAllActivePaged_WithMultipleSickLeaves_ReturnsPaged() {
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
        SickLeave sickLeave1 = createSickLeave(LocalDate.now(), 5, visit1);
        SickLeave sickLeave2 = createSickLeave(LocalDate.now().minusDays(1), 10, visit2);
        sickLeaveRepository.save(sickLeave1);
        sickLeaveRepository.save(sickLeave2);

        Page<SickLeave> activeSickLeaves = sickLeaveRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeSickLeaves.getTotalElements());
        assertEquals(1, activeSickLeaves.getContent().size());
    }

    @Test
    void FindYearMonthWithMostSickLeaves_WithMultipleSickLeaves_ReturnsSorted() {
        Doctor doctor = createDoctor("Dr. Taylor", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Tom Green", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor, diagnosis, LocalDate.now());
        Visit visit2 = createVisit(patient, doctor, diagnosis, LocalDate.now());
        Visit visit3 = createVisit(patient, doctor, diagnosis, LocalDate.of(2024, 1, 1));
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        visit3 = visitRepository.save(visit3);
        SickLeave sickLeave1 = createSickLeave(LocalDate.of(2025, 5, 1), 5, visit1);
        SickLeave sickLeave2 = createSickLeave(LocalDate.of(2025, 5, 2), 7, visit2);
        SickLeave sickLeave3 = createSickLeave(LocalDate.of(2024, 1, 1), 3, visit3);
        sickLeaveRepository.save(sickLeave1);
        sickLeaveRepository.save(sickLeave2);
        sickLeaveRepository.save(sickLeave3);

        List<YearMonthSickLeaveCountDTO> result = sickLeaveRepository.findYearMonthWithMostSickLeaves();

        assertEquals(2, result.size());
        assertEquals(2025, result.getFirst().getYear());
        assertEquals(5, result.get(0).getMonth());
        assertEquals(2, result.get(0).getCount());
        assertEquals(2024, result.get(1).getYear());
        assertEquals(1, result.get(1).getMonth());
        assertEquals(1, result.get(1).getCount());
    }

    @Test
    void SoftDelete_WithExistingSickLeave_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Wilson", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Sarah Lee", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Pneumonia", "Lung infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 14, visit);
        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);

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
        Doctor doctor = createDoctor("Dr. Adams", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Mike Brown", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Influenza", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
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
        Doctor doctor = createDoctor("Dr. Clark", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Emma White", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Sprain", "Ankle injury");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 1, visit); // DURATION_MIN_DAYS = 1

        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);

        assertEquals(1, savedSickLeave.getDurationDays());
    }

    @Test
    void Save_WithMaximumDuration_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Evans", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Olivia Green", generateValidEgn(), doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Fracture", "Bone injury");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now());
        visit = visitRepository.save(visit);
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 30, visit); // DURATION_MAX_DAYS = 30

        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);

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