package nbu.cscb869.data.repositories.integrationtests;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DoctorRepositoryIntegrationTests {

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
    private SpecialtyRepository specialtyRepository;

    @BeforeEach
    void setUp() {
        sickLeaveRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
        specialtyRepository.deleteAll();
    }

    private Doctor createDoctor(String uniqueIdNumber, boolean isGeneralPractitioner, String name) {
        Doctor doctor = new Doctor();
        doctor.setUniqueIdNumber(uniqueIdNumber);
        doctor.setGeneralPractitioner(isGeneralPractitioner);
        doctor.setName(name);
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        return doctor;
    }

    private Patient createPatient(String egn, String name, Doctor generalPractitioner, LocalDate lastInsurancePaymentDate) {
        Patient patient = new Patient();
        patient.setEgn(egn);
        patient.setName(name);
        patient.setGeneralPractitioner(generalPractitioner);
        patient.setLastInsurancePaymentDate(lastInsurancePaymentDate);
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        return patient;
    }

    private Diagnosis createDiagnosis(String name, String description) {
        return Diagnosis.builder()
                .name(name)
                .description(description)
                .build();
    }

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate, LocalTime visitTime, SickLeave sickLeave) {
        Visit visit = Visit.builder()
                .patient(patient)
                .doctor(doctor)
                .diagnosis(diagnosis)
                .visitDate(visitDate)
                .visitTime(visitTime)
                .build();
        if (sickLeave != null) {
            visit.setSickLeave(sickLeave);
            sickLeave.setVisit(visit);
        }
        return visit;
    }

    private SickLeave createSickLeave(Visit visit, LocalDate startDate, int durationDays) {
        return SickLeave.builder()
                .visit(visit)
                .startDate(startDate)
                .durationDays(durationDays)
                .build();
    }

    @Test
    void save_WithValidDoctor_SavesSuccessfully_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Doctor saved = doctorRepository.save(doctor);

        Optional<Doctor> found = doctorRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getUniqueIdNumber(), found.get().getUniqueIdNumber());
    }

    @Test
    void findByUniqueIdNumber_WithExistingId_ReturnsDoctor_HappyPath() {
        String uniqueId = TestDataUtils.generateUniqueIdNumber();
        Doctor doctor = createDoctor(uniqueId, false, "Dr. Jane Smith");
        doctorRepository.save(doctor);

        Optional<Doctor> found = doctorRepository.findByUniqueIdNumber(uniqueId);

        assertTrue(found.isPresent());
        assertEquals(uniqueId, found.get().getUniqueIdNumber());
    }

    @Test
    void findByKeycloakId_WithExistingId_ReturnsDoctor_HappyPath() {
        // ARRANGE
        String keycloakId = TestDataUtils.generateKeycloakId();
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Keycloak Test");
        doctor.setKeycloakId(keycloakId);
        doctorRepository.save(doctor);

        // ACT
        Optional<Doctor> found = doctorRepository.findByKeycloakId(keycloakId);

        // ASSERT
        assertTrue(found.isPresent());
        assertEquals(keycloakId, found.get().getKeycloakId());
    }

    @Test
    void findByUniqueIdNumberContaining_WithMatch_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor("DOC123", true, "Dr. Bob White");
        doctorRepository.save(doctor);

        // Add % wildcards for LIKE matching
        Page<Doctor> result = doctorRepository.findByUniqueIdNumberContaining("%123%", PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("DOC123", result.getContent().getFirst().getUniqueIdNumber());
    }

    @Test
    void findAll_WithSpecification_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        doctorRepository.save(doctor);

        Page<Doctor> result = doctorRepository.findAll((root, query, cb) -> cb.equal(root.get("uniqueIdNumber"), doctor.getUniqueIdNumber()), PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(doctor.getUniqueIdNumber(), result.getContent().getFirst().getUniqueIdNumber());
    }

    @Test
    void findPatientsByGeneralPractitioner_WithPatients_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), "Test Patient", doctor, LocalDate.now());
        patientRepository.save(patient);

        Page<Patient> result = doctorRepository.findPatientsByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(patient.getEgn(), result.getContent().getFirst().getEgn());
    }

    @Test
    void findPatientCountByGeneralPractitioner_WithPatients_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), "Test Patient", doctor, LocalDate.now());
        patientRepository.save(patient);

        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getPatientCount());
    }

    @Test
    void findVisitCountByDoctor_WithVisits_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Green");
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), "Test Patient", doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visitRepository.save(visit);

        List<DoctorVisitCountDTO> result = doctorRepository.findVisitCountByDoctor();

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getVisitCount());
    }

    @Test
    void findDoctorsWithMostSickLeaves_WithSickLeaves_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Lee");
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), "Test Patient", doctor, LocalDate.now());
        patient = patientRepository.save(patient);
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visit = visitRepository.save(visit);
        SickLeave sickLeave = createSickLeave(visit, LocalDate.now(), 5);
        sickLeaveRepository.save(sickLeave);

        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getSickLeaveCount());
    }

    @Test
    void existsBySpecialtiesContains_WhenSpecialtyInUse_ReturnsTrue_HappyPath() {
        // ARRANGE
        Specialty specialty = new Specialty();
        specialty.setName("Cardiology");
        specialty = specialtyRepository.save(specialty);

        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), false, "Dr. Specialist");
        doctor.setSpecialties(Set.of(specialty));
        doctorRepository.save(doctor);

        // ACT
        boolean result = doctorRepository.existsBySpecialtiesContains(specialty);

        // ASSERT
        assertTrue(result);
    }

    @Test
    void existsBySpecialtiesContains_WhenSpecialtyNotInUse_ReturnsFalse_ErrorCase() {
        // ARRANGE
        Specialty specialty = new Specialty();
        specialty.setName("Cardiology");
        specialty = specialtyRepository.save(specialty);

        Specialty unusedSpecialty = new Specialty();
        unusedSpecialty.setName("Unused");
        unusedSpecialty = specialtyRepository.save(unusedSpecialty);

        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), false, "Dr. Specialist");
        doctor.setSpecialties(Set.of(specialty));
        doctorRepository.save(doctor);

        // ACT
        boolean result = doctorRepository.existsBySpecialtiesContains(unusedSpecialty);

        // ASSERT
        assertFalse(result);
    }

    @Test
    void save_WithDuplicateUniqueIdNumber_ThrowsException_ErrorCase() {
        String uniqueId = TestDataUtils.generateUniqueIdNumber();
        Doctor doctor1 = createDoctor(uniqueId, true, "Dr. John Doe");
        Doctor doctor2 = createDoctor(uniqueId, false, "Dr. Jane Smith");
        doctorRepository.save(doctor1);

        assertThrows(DataIntegrityViolationException.class, () -> doctorRepository.save(doctor2));
    }

    @Test
    void findByUniqueIdNumber_WithNonExistentId_ReturnsEmpty_ErrorCase() {
        Optional<Doctor> found = doctorRepository.findByUniqueIdNumber("NONEXISTENT");

        assertFalse(found.isPresent());
    }

    @Test
    void findByKeycloakId_WithNonExistentId_ReturnsEmpty_ErrorCase() {
        // ARRANGE
        String nonExistentKeycloakId = "non-existent-keycloak-id";

        // ACT
        Optional<Doctor> found = doctorRepository.findByKeycloakId(nonExistentKeycloakId);

        // ASSERT
        assertFalse(found.isPresent());
    }

    @Test
    void findByUniqueIdNumberContaining_WithNoMatch_ReturnsEmptyPage_ErrorCase() {
        Page<Doctor> result = doctorRepository.findByUniqueIdNumberContaining("XYZ", PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findAll_WithNoMatch_ReturnsEmptyPage_ErrorCase() {
        Page<Doctor> result = doctorRepository.findAll((root, query, cb) -> cb.equal(root.get("uniqueIdNumber"), "NONEXISTENT"), PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findPatientsByGeneralPractitioner_WithNoPatients_ReturnsEmptyPage_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        doctor = doctorRepository.save(doctor);

        Page<Patient> result = doctorRepository.findPatientsByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findPatientCountByGeneralPractitioner_WithNoPatients_ReturnsZeroCount_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        doctorRepository.save(doctor);

        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();

        assertEquals(1, result.size());
        assertEquals(0L, result.getFirst().getPatientCount());
    }

    @Test
    void findVisitCountByDoctor_WithNoVisits_ReturnsZeroCount_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        doctorRepository.save(doctor);

        List<DoctorVisitCountDTO> result = doctorRepository.findVisitCountByDoctor();

        assertEquals(1, result.size());
        assertEquals(0L, result.getFirst().getVisitCount());
    }

    @Test
    void findDoctorsWithMostSickLeaves_WithNoSickLeaves_ReturnsEmpty_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        doctorRepository.save(doctor);

        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUniqueIdNumberContaining_WithEmptyFilter_ReturnsAllDoctors_EdgeCase() {
        Doctor doctor1 = createDoctor("DOC123", true, "Dr. Alice Brown");
        Doctor doctor2 = createDoctor("DOC456", false, "Dr. Bob White");
        doctorRepository.saveAll(List.of(doctor1, doctor2));

        // Use % wildcard to match any string
        Page<Doctor> result = doctorRepository.findByUniqueIdNumberContaining("%%", PageRequest.of(0, 2));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void findPatientsByGeneralPractitioner_WithLargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        doctor = doctorRepository.save(doctor);
        List<Patient> patients = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            patients.add(createPatient(TestDataUtils.generateValidEgn(), "Patient " + i, doctor, LocalDate.now()));
        }
        patientRepository.saveAll(patients);

        Page<Patient> result = doctorRepository.findPatientsByGeneralPractitioner(doctor, PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
    }

}
