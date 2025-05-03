package nbu.cscb869.data.repositories.unittests;

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
class PatientRepositoryUnitTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private EntityManager entityManager;

    private static final int[] EGN_WEIGHTS = {2, 4, 8, 5, 10, 9, 7, 3, 6};
    private static final Random RANDOM = new Random();

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
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
    void Save_WithValidPatient_SavesSuccessfully() {
        Doctor doctor = createDoctor("Dr. Smith", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", generateValidEgn(), doctor, LocalDate.now());
        Patient savedPatient = patientRepository.save(patient);
        Optional<Patient> foundPatient = patientRepository.findById(savedPatient.getId());

        assertTrue(foundPatient.isPresent());
        assertEquals("Jane Doe", foundPatient.get().getName());
        assertEquals(doctor.getId(), foundPatient.get().getGeneralPractitioner().getId());
        assertEquals(LocalDate.now(), foundPatient.get().getLastInsurancePaymentDate());
        assertFalse(foundPatient.get().getIsDeleted());
        assertNotNull(foundPatient.get().getCreatedOn());
    }

    @Test
    void FindByEgn_WithValidEgn_ReturnsPatient() {
        Doctor doctor = createDoctor("Dr. Brown", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        String egn = generateValidEgn();
        Patient patient = createPatient("Bob White", egn, doctor, LocalDate.now());
        patientRepository.save(patient);

        Optional<Patient> foundPatient = patientRepository.findByEgn(egn);

        assertTrue(foundPatient.isPresent());
        assertEquals("Bob White", foundPatient.get().getName());
    }

    @Test
    void FindByGeneralPractitioner_WithMultiplePatients_ReturnsPaged() {
        Doctor doctor1 = createDoctor("Dr. Green", generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Taylor", generateUniqueIdNumber(), true);
        doctor1 = doctorRepository.save(doctor1);
        doctor2 = doctorRepository.save(doctor2);
        Patient patient1 = createPatient("Alice Lee", generateValidEgn(), doctor1, LocalDate.now());
        Patient patient2 = createPatient("Tom Green", generateValidEgn(), doctor1, LocalDate.now());
        Patient patient3 = createPatient("Sarah Lee", generateValidEgn(), doctor2, LocalDate.now());
        patientRepository.save(patient1);
        patientRepository.save(patient2);
        patientRepository.save(patient3);

        Page<Patient> gpPatients = patientRepository.findByGeneralPractitioner(doctor1, PageRequest.of(0, 1));

        assertEquals(2, gpPatients.getTotalElements());
        assertEquals(1, gpPatients.getContent().size());
    }

    @Test
    void CountPatientsByGeneralPractitioner_WithMultiplePatients_ReturnsSorted() {
        Doctor doctor1 = createDoctor("Dr. Wilson", generateUniqueIdNumber(), true);
        Doctor doctor2 = createDoctor("Dr. Adams", generateUniqueIdNumber(), true);
        doctor1 = doctorRepository.save(doctor1);
        doctor2 = doctorRepository.save(doctor2);
        Patient patient1 = createPatient("Mike Brown", generateValidEgn(), doctor1, LocalDate.now());
        Patient patient2 = createPatient("Emma White", generateValidEgn(), doctor1, LocalDate.now());
        Patient patient3 = createPatient("Olivia Green", generateValidEgn(), doctor2, LocalDate.now());
        patientRepository.save(patient1);
        patientRepository.save(patient2);
        patientRepository.save(patient3);

        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();

        assertEquals(2, result.size());
        assertEquals("Dr. Wilson", result.get(0).getDoctor().getName());
        assertEquals(2, result.get(0).getPatientCount());
        assertEquals("Dr. Adams", result.get(1).getDoctor().getName());
        assertEquals(1, result.get(1).getPatientCount());
    }

    @Test
    void SoftDelete_WithExistingPatient_SetsIsDeleted() {
        Doctor doctor = createDoctor("Dr. Clark", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", generateValidEgn(), doctor, LocalDate.now());
        Patient savedPatient = patientRepository.save(patient);

        patientRepository.delete(savedPatient);
        entityManager.flush();
        Optional<Patient> deletedPatient = patientRepository.findById(savedPatient.getId());
        if (deletedPatient.isPresent()) {
            entityManager.refresh(deletedPatient.get());
        }

        assertTrue(deletedPatient.isPresent());
        assertTrue(deletedPatient.get().getIsDeleted());
        assertNotNull(deletedPatient.get().getDeletedOn());
        assertEquals(0, patientRepository.findAllActive().size());
    }

    @Test
    void HardDelete_WithExistingPatient_RemovesPatient() {
        Doctor doctor = createDoctor("Dr. Evans", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Bob White", generateValidEgn(), doctor, LocalDate.now());
        Patient savedPatient = patientRepository.save(patient);
        entityManager.flush();

        patientRepository.hardDeleteById(savedPatient.getId());
        entityManager.flush();

        assertTrue(patientRepository.findById(savedPatient.getId()).isEmpty());
    }

    // Error Cases
    @Test
    void HardDelete_WithNonExistentId_DoesNotThrow() {
        assertDoesNotThrow(() -> patientRepository.hardDeleteById(999L));
    }

    @Test
    void FindByEgn_WithNonExistentEgn_ReturnsEmpty() {
        Optional<Patient> foundPatient = patientRepository.findByEgn("1234567890");

        assertTrue(foundPatient.isEmpty());
    }

    @Test
    void FindByGeneralPractitioner_WithNoPatients_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Lee", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);

        Page<Patient> gpPatients = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(0, gpPatients.getTotalElements());
        assertTrue(gpPatients.getContent().isEmpty());
    }

    @Test
    void CountPatientsByGeneralPractitioner_WithNoPatients_ReturnsEmpty() {
        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();

        assertTrue(result.isEmpty());
    }

    // Edge Cases
    @Test
    void FindByGeneralPractitioner_WithSoftDeletedPatient_ReturnsEmpty() {
        Doctor doctor = createDoctor("Dr. Smith", generateUniqueIdNumber(), true);
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient("Jane Doe", generateValidEgn(), doctor, LocalDate.now());
        Patient savedPatient = patientRepository.save(patient);
        patientRepository.delete(savedPatient);

        Page<Patient> gpPatients = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(0, gpPatients.getTotalElements());
        assertTrue(gpPatients.getContent().isEmpty());
    }

    @Test
    void FindAllActive_WithNoPatients_ReturnsEmpty() {
        List<Patient> activePatients = patientRepository.findAllActive();

        assertTrue(activePatients.isEmpty());
    }

    @Test
    void FindAllActivePaged_WithNoPatients_ReturnsEmpty() {
        Page<Patient> activePatients = patientRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, activePatients.getTotalElements());
        assertTrue(activePatients.getContent().isEmpty());
    }

    @Test
    void SoftDelete_WithNonExistentPatient_DoesNotThrow() {
        Patient patient = new Patient();
        patient.setId(999L);

        assertDoesNotThrow(() -> patientRepository.delete(patient));
    }
}