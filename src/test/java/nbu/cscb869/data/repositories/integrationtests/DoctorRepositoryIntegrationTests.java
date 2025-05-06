package nbu.cscb869.data.repositories.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
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
class DoctorRepositoryIntegrationTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
            .withExposedPorts(3306)
            .withEnv("MYSQL_ROOT_PASSWORD", "test");

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private SickLeaveRepository sickLeaveRepository;

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

    private SickLeave createSickLeave(Visit visit, LocalDate startDate, int durationDays) {
        SickLeave sickLeave = new SickLeave();
        sickLeave.setVisit(visit);
        sickLeave.setStartDate(startDate);
        sickLeave.setDurationDays(durationDays);
        return sickLeave;
    }

    // Happy Path
    @Test
    void Save_WithValidDoctor_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. John Smith", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor savedDoctor = doctorRepository.save(doctor);
        entityManager.flush();
        Optional<Doctor> foundDoctor = doctorRepository.findById(savedDoctor.getId());

        assertTrue(foundDoctor.isPresent());
        assertEquals("Dr. John Smith", foundDoctor.get().getName());
        assertEquals(savedDoctor.getUniqueIdNumber(), foundDoctor.get().getUniqueIdNumber());
        assertTrue(foundDoctor.get().isGeneralPractitioner());
        assertFalse(foundDoctor.get().getIsDeleted());
        assertNotNull(foundDoctor.get().getCreatedOn());
    }

    @Test
    void FindByUniqueIdNumber_WithExistingId_ReturnsDoctor() {
        String uniqueId = TestDataUtils.generateUniqueIdNumber();
        Doctor doctor = createDoctor("Dr. Jane Doe", uniqueId, false);
        doctorRepository.save(doctor);
        entityManager.flush();

        Optional<Doctor> foundDoctor = doctorRepository.findByUniqueIdNumber(uniqueId);

        assertTrue(foundDoctor.isPresent());
        assertEquals("Dr. Jane Doe", foundDoctor.get().getName());
        assertFalse(foundDoctor.get().isGeneralPractitioner());
    }

    @Test
    void FindAllActive_WithMultipleDoctors_ReturnsAll() {
        Doctor doctor1 = createDoctor("Dr. Alice Brown", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Bob White", TestDataUtils.generateUniqueIdNumber(), false);
        doctorRepository.save(doctor1);
        doctorRepository.save(doctor2);
        entityManager.flush();

        List<Doctor> activeDoctors = doctorRepository.findAllActive();

        assertEquals(2, activeDoctors.size());
        assertTrue(activeDoctors.stream().anyMatch(d -> d.getName().equals("Dr. Alice Brown")));
        assertTrue(activeDoctors.stream().anyMatch(d -> d.getName().equals("Dr. Bob White")));
    }

    @Test
    void FindAllActivePaged_WithMultipleDoctors_ReturnsPaged() {
        Doctor doctor1 = createDoctor("Dr. Alice Brown", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Bob White", TestDataUtils.generateUniqueIdNumber(), false);
        doctorRepository.save(doctor1);
        doctorRepository.save(doctor2);
        entityManager.flush();

        Page<Doctor> activeDoctors = doctorRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeDoctors.getTotalElements());
        assertEquals(1, activeDoctors.getContent().size());
    }

    @Test
    void SoftDelete_WithExistingDoctor_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Tom Green", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor savedDoctor = doctorRepository.save(doctor);
        entityManager.flush();

        doctorRepository.delete(savedDoctor);
        entityManager.flush();
        Optional<Doctor> deletedDoctor = doctorRepository.findById(savedDoctor.getId());
        if (deletedDoctor.isPresent()) {
            entityManager.refresh(deletedDoctor.get());
        }

        assertTrue(deletedDoctor.isPresent());
        assertTrue(deletedDoctor.get().getIsDeleted());
        assertNotNull(deletedDoctor.get().getDeletedOn());
        assertEquals(0, doctorRepository.findAllActive().size());
    }

    @Test
    void HardDelete_WithExistingDoctor_RemovesDoctor() {
        Doctor doctor = createDoctor("Dr. Sarah Lee", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor savedDoctor = doctorRepository.save(doctor);
        entityManager.flush();

        doctorRepository.hardDeleteById(savedDoctor.getId());
        entityManager.flush();

        assertTrue(doctorRepository.findById(savedDoctor.getId()).isEmpty());
    }

    @Test
    void FindPatientCountByGeneralPractitioner_WithSoftDeletedPatients_ExcludesDeleted() {
        Doctor sharedDoctor = createDoctor("Dr. Shared", TestDataUtils.generateUniqueIdNumber(), true);
        sharedDoctor = doctorRepository.save(sharedDoctor);
        Patient patient1 = createPatient("Patient 1", TestDataUtils.generateValidEgn(), sharedDoctor, LocalDate.now());
        Patient patient2 = createPatient("Patient 2", TestDataUtils.generateValidEgn(), sharedDoctor, LocalDate.now());
        patient1 = patientRepository.save(patient1);
        patient2 = patientRepository.save(patient2);
        entityManager.flush();

        patientRepository.delete(patient1);
        entityManager.flush();

        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();

        assertEquals(1, result.size());
        assertEquals(sharedDoctor.getId(), result.getFirst().getDoctor().getId());
        assertEquals(1, result.getFirst().getPatientCount());
    }

    @Test
    void FindVisitCountByDoctor_WithMultipleVisits_ReturnsList() {
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

        List<DoctorVisitCountDTO> result = doctorRepository.findVisitCountByDoctor();

        assertEquals(2, result.size());
        assertEquals("Dr. Green", result.getFirst().getDoctor().getName());
        assertEquals(2L, result.getFirst().getVisitCount());
        assertEquals("Dr. Adams", result.get(1).getDoctor().getName());
        assertEquals(1L, result.get(1).getVisitCount());
    }

    @Test
    void FindDoctorsWithMostSickLeaves_WithMultipleSickLeaves_ReturnsList() {
        Doctor doctor1 = createDoctor("Dr. Lee", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Brown", TestDataUtils.generateUniqueIdNumber(), false);
        doctor1 = doctorRepository.save(doctor1);
        doctor2 = doctorRepository.save(doctor2);
        Patient patient = createPatient("Bob White", TestDataUtils.generateValidEgn(), doctor1, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit1 = createVisit(patient, doctor1, diagnosis, LocalDate.now(), LocalTime.of(10, 30), true);
        Visit visit2 = createVisit(patient, doctor1, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), true);
        Visit visit3 = createVisit(patient, doctor2, diagnosis, LocalDate.now(), LocalTime.of(12, 0), true);
        visit1 = visitRepository.save(visit1);
        visit2 = visitRepository.save(visit2);
        visit3 = visitRepository.save(visit3);
        SickLeave sickLeave1 = createSickLeave(visit1, LocalDate.now(), 5);
        SickLeave sickLeave2 = createSickLeave(visit2, LocalDate.now().minusDays(1), 3);
        SickLeave sickLeave3 = createSickLeave(visit3, LocalDate.now(), 7);
        sickLeaveRepository.save(sickLeave1);
        sickLeaveRepository.save(sickLeave2);
        sickLeaveRepository.save(sickLeave3);
        entityManager.flush();

        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();

        assertEquals(2, result.size());
        assertEquals("Dr. Lee", result.getFirst().getDoctor().getName());
        assertEquals(2L, result.getFirst().getSickLeaveCount());
        assertEquals("Dr. Brown", result.get(1).getDoctor().getName());
        assertEquals(1L, result.get(1).getSickLeaveCount());
    }

    // Error Cases
    @Test
    void Save_WithDuplicateUniqueIdNumber_ThrowsException() {
        String uniqueId = TestDataUtils.generateUniqueIdNumber();
        Doctor doctor1 = createDoctor("Dr. John Smith", uniqueId, true);
        Doctor doctor2 = createDoctor("Dr. Jane Doe", uniqueId, false);
        doctorRepository.save(doctor1);
        entityManager.flush();

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            doctorRepository.save(doctor2);
            entityManager.flush();
        });
    }

    @Test
    void FindByUniqueIdNumber_WithNonExistentId_ReturnsEmpty() {
        Optional<Doctor> foundDoctor = doctorRepository.findByUniqueIdNumber("NONEXISTENT");

        assertFalse(foundDoctor.isPresent());
    }

    @Test
    void HardDelete_WithNonExistentId_DoesNotThrow() {
        assertDoesNotThrow(() -> doctorRepository.hardDeleteById(999L));
    }

    @Test
    void FindPatientCountByGeneralPractitioner_WithNoPatients_ReturnsZeroCount() {
        Doctor doctor = createDoctor("Dr. Smith", TestDataUtils.generateUniqueIdNumber(), true);
        doctorRepository.save(doctor);
        entityManager.flush();

        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();

        assertEquals(1, result.size());
        assertEquals(doctor.getName(), result.getFirst().getDoctor().getName());
        assertEquals(0, result.getFirst().getPatientCount());
    }

    @Test
    void FindVisitCountByDoctor_WithNoVisits_ReturnsZeroCount() {
        Doctor doctor = createDoctor("Dr. Green", TestDataUtils.generateUniqueIdNumber(), true);
        doctorRepository.save(doctor);
        entityManager.flush();

        List<DoctorVisitCountDTO> result = doctorRepository.findVisitCountByDoctor();

        assertEquals(1, result.size());
        assertEquals(doctor.getName(), result.getFirst().getDoctor().getName());
        assertEquals(0, result.getFirst().getVisitCount());
    }

    @Test
    void FindDoctorsWithMostSickLeaves_WithNoSickLeaves_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Lee", TestDataUtils.generateUniqueIdNumber(), true);
        doctorRepository.save(doctor);
        entityManager.flush();

        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();

        assertTrue(result.isEmpty());
    }

    // Edge Cases
    @Test
    void Save_WithMinimumNameLength_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. AB", TestDataUtils.generateUniqueIdNumber(), true);
        Doctor savedDoctor = doctorRepository.save(doctor);
        entityManager.flush();

        assertEquals("Dr. AB", savedDoctor.getName());
    }

    @Test
    void Save_WithMaximumNameLength_SavesSuccessfully() {
        String maxName = "Dr. " + "A".repeat(46);
        Doctor doctor = createDoctor(maxName, TestDataUtils.generateUniqueIdNumber(), true);
        Doctor savedDoctor = doctorRepository.save(doctor);
        entityManager.flush();

        assertEquals(maxName, savedDoctor.getName());
    }

    @Test
    void FindAllActive_WithNoDoctors_ReturnsEmpty() {
        List<Doctor> activeDoctors = doctorRepository.findAllActive();

        assertTrue(activeDoctors.isEmpty());
    }

    @Test
    void FindAllActivePaged_WithNoDoctors_ReturnsEmpty() {
        Page<Doctor> activeDoctors = doctorRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, activeDoctors.getTotalElements());
        assertTrue(activeDoctors.getContent().isEmpty());
    }

    @Test
    void SoftDelete_WithNonExistentDoctor_DoesNotThrow() {
        Doctor doctor = new Doctor();
        doctor.setId(999L);

        assertDoesNotThrow(() -> doctorRepository.delete(doctor));
    }

    @Test
    void FindByUniqueIdNumber_WithSoftDeletedDoctor_ReturnsEmpty() {
        String uniqueId = TestDataUtils.generateUniqueIdNumber();
        Doctor doctor = createDoctor("Dr. Tom Green", uniqueId, true);
        Doctor savedDoctor = doctorRepository.save(doctor);
        entityManager.flush();

        doctorRepository.delete(savedDoctor);
        entityManager.flush();

        Optional<Doctor> foundDoctor = doctorRepository.findByUniqueIdNumber(uniqueId);

        assertFalse(foundDoctor.isPresent());
    }
}