package nbu.cscb869.data.repositories.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
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
class DoctorRepositoryIntegrationTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EntityManager entityManager;

    private static final int[] EGN_WEIGHTS = {2, 4, 8, 5, 10, 9, 7, 3, 6};
    private static final Random RANDOM = new Random();

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to avoid cached entities
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

    private String generateUniqueIdNumber() {
        return "DOC" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5); // 8 chars
    }

    private String generateValidEgn() {
        int year = 2000 + RANDOM.nextInt(26);
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28);
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
    void Save_WithValidDoctor_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. John Smith", generateUniqueIdNumber(), true);
        Doctor savedDoctor = doctorRepository.save(doctor);
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
        String uniqueId = generateUniqueIdNumber();
        Doctor doctor = createDoctor("Dr. Jane Doe", uniqueId, false);
        doctorRepository.save(doctor);

        Optional<Doctor> foundDoctor = doctorRepository.findByUniqueIdNumber(uniqueId);

        assertTrue(foundDoctor.isPresent());
        assertEquals("Dr. Jane Doe", foundDoctor.get().getName());
        assertFalse(foundDoctor.get().isGeneralPractitioner());
    }

    @Test
    void FindAllActive_WithMultipleDoctors_ReturnsAll() {
        Doctor doctor1 = createDoctor("Dr. Alice Brown", generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Bob White", generateUniqueIdNumber(), false);
        doctorRepository.save(doctor1);
        doctorRepository.save(doctor2);
        entityManager.flush();
        entityManager.clear();

        List<Doctor> activeDoctors = doctorRepository.findAllActive();

        assertEquals(2, activeDoctors.size());
        assertTrue(activeDoctors.stream().anyMatch(d -> d.getName().equals("Dr. Alice Brown")));
        assertTrue(activeDoctors.stream().anyMatch(d -> d.getName().equals("Dr. Bob White")));
    }

    @Test
    void FindAllActivePaged_WithMultipleDoctors_ReturnsPaged() {
        Doctor doctor1 = createDoctor("Dr. Alice Brown", generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Bob White", generateUniqueIdNumber(), false);
        doctorRepository.save(doctor1);
        doctorRepository.save(doctor2);
        entityManager.flush();
        entityManager.clear();

        Page<Doctor> activeDoctors = doctorRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeDoctors.getTotalElements());
        assertEquals(1, activeDoctors.getContent().size());
    }

    @Test
    void SoftDelete_WithExistingDoctor_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Tom Green", generateUniqueIdNumber(), true);
        Doctor savedDoctor = doctorRepository.save(doctor);
        entityManager.flush();
        entityManager.clear();

        doctorRepository.delete(savedDoctor);
        entityManager.flush();
        entityManager.clear();

        Optional<Doctor> deletedDoctor = doctorRepository.findById(savedDoctor.getId());
        assertTrue(deletedDoctor.isPresent());
        assertTrue(deletedDoctor.get().getIsDeleted());
        assertNotNull(deletedDoctor.get().getDeletedOn());
        assertEquals(0, doctorRepository.findAllActive().size());
    }

    @Test
    void HardDelete_WithExistingDoctor_RemovesDoctor() {
        Doctor doctor = createDoctor("Dr. Sarah Lee", generateUniqueIdNumber(), true);
        Doctor savedDoctor = doctorRepository.save(doctor);
        entityManager.flush();
        entityManager.clear();

        doctorRepository.hardDeleteById(savedDoctor.getId());
        assertTrue(doctorRepository.findById(savedDoctor.getId()).isEmpty());
    }

    @Test
    void FindPatientCountByGeneralPractitioner_WithSoftDeletedPatients_ExcludesDeleted() {
        Doctor sharedDoctor = createDoctor("Dr. Shared", generateUniqueIdNumber(), true);
        sharedDoctor = doctorRepository.save(sharedDoctor);
        Patient patient1 = createPatient("Patient 1", generateValidEgn(), sharedDoctor, LocalDate.now());
        Patient patient2 = createPatient("Patient 2", generateValidEgn(), sharedDoctor, LocalDate.now());
        patient1 = patientRepository.save(patient1);
        patient2 = patientRepository.save(patient2);
        entityManager.flush();
        entityManager.clear();

        patientRepository.delete(patient1);
        entityManager.flush();
        entityManager.clear();

        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();

        assertEquals(1, result.size(), "Should return one doctor with active patients");
        assertEquals(sharedDoctor.getId(), result.getFirst().getDoctor().getId(), "Doctor ID should match");
        assertEquals(1, result.getFirst().getPatientCount(), "Only one active patient should be counted");
    }

    // Error Cases
    @Test
    void Save_WithDuplicateUniqueIdNumber_ThrowsException() {
        String uniqueId = generateUniqueIdNumber();
        Doctor doctor1 = createDoctor("Dr. John Smith", uniqueId, true);
        Doctor doctor2 = createDoctor("Dr. Jane Doe", uniqueId, false);
        doctorRepository.save(doctor1);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> doctorRepository.saveAndFlush(doctor2));
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

    // Edge Cases
    @Test
    void Save_WithMinimumNameLength_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. AB", generateUniqueIdNumber(), true);

        Doctor savedDoctor = doctorRepository.save(doctor);

        assertEquals("Dr. AB", savedDoctor.getName());
    }

    @Test
    void Save_WithMaximumNameLength_SavesSuccessfully() {
        String maxName = "Dr. " + "A".repeat(46);
        Doctor doctor = createDoctor(maxName, generateUniqueIdNumber(), true);

        Doctor savedDoctor = doctorRepository.save(doctor);

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
        String uniqueId = generateUniqueIdNumber();
        Doctor doctor = createDoctor("Dr. Tom Green", uniqueId, true);
        Doctor savedDoctor = doctorRepository.save(doctor);
        entityManager.flush();
        entityManager.clear();

        doctorRepository.delete(savedDoctor);
        entityManager.flush();
        entityManager.clear();

        Optional<Doctor> foundDoctor = doctorRepository.findByUniqueIdNumber(uniqueId);

        assertFalse(foundDoctor.isPresent());
    }
}