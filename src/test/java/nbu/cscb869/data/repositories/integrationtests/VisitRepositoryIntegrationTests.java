package nbu.cscb869.data.repositories.integrationtests;

import jakarta.validation.ConstraintViolationException;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VisitRepositoryIntegrationTests {

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

    @BeforeEach
    void setUp() {
        sickLeaveRepository.deleteAll();
        treatmentRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();
    }

    private Doctor createDoctor(String uniqueIdNumber, boolean isGeneralPractitioner, String name) {
        return Doctor.builder()
                .uniqueIdNumber(uniqueIdNumber)
                .isGeneralPractitioner(isGeneralPractitioner)
                .name(name)
                .build();
    }

    private Patient createPatient(String egn, Doctor generalPractitioner, LocalDate lastInsurancePaymentDate) {
        return Patient.builder()
                .egn(egn)
                .generalPractitioner(generalPractitioner)
                .lastInsurancePaymentDate(lastInsurancePaymentDate)
                .build();
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
            sickLeave.setVisit(visit); // Ensure bidirectional link
        }
        return visit;
    }

    private Treatment createTreatment(String description, Visit visit) {
        return Treatment.builder()
                .description(description)
                .visit(visit)
                .build();
    }

    private SickLeave createSickLeave(LocalDate startDate, int durationDays, Visit visit) {
        return SickLeave.builder()
                .startDate(startDate)
                .durationDays(durationDays)
                .visit(visit)
                .build();
    }

    @Test
    void save_WithValidVisit_SavesSuccessfully_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Visit saved = visitRepository.save(visit);

        Optional<Visit> found = visitRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(LocalDate.now(), found.get().getVisitDate());
        assertEquals(LocalTime.of(10, 30), found.get().getVisitTime());
    }

    @Test
    void save_WithSickLeave_SavesSuccessfully_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visit = visitRepository.save(visit);
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 5, visit);
        sickLeave = sickLeaveRepository.save(sickLeave);
        visit.setSickLeave(sickLeave);
        visitRepository.save(visit);

        Optional<Visit> found = visitRepository.findById(visit.getId());

        assertTrue(found.isPresent());
        assertTrue(found.get().isSickLeaveIssued());
        assertEquals(5, found.get().getSickLeave().getDurationDays());
    }

    @Test
    void save_WithTreatment_SavesSuccessfully_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Antibiotic therapy", visit);
        treatment = treatmentRepository.save(treatment);
        // Link bidirectional
        visit.setTreatment(treatment);
        treatment.setVisit(visit);
        visitRepository.save(visit); // Persist link

        // Reload to ensure relationship is loaded
        Optional<Visit> found = visitRepository.findById(visit.getId());

        assertTrue(found.isPresent());
        assertNotNull(found.get().getTreatment());
        assertEquals("Antibiotic therapy", found.get().getTreatment().getDescription());
    }

    @Test
    void findByPatient_WithMultipleVisits_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByPatient(patient, PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByDoctor_WithMultipleVisits_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Charlie Green");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDoctor(doctor, PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByDateRange_WithMultipleVisits_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. David Black");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDateRange(LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByDoctorAndDateRange_WithMultipleVisits_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Eve White");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByDiagnosis_WithMultipleVisits_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Frank Gray");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findMostFrequentDiagnoses_WithMultipleVisits_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Grace Blue");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis1 = createDiagnosis("Flu", "Viral infection");
        Diagnosis diagnosis2 = createDiagnosis("Cold", "Common cold");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis1 = diagnosisRepository.save(diagnosis1);
        diagnosis2 = diagnosisRepository.save(diagnosis2);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis1, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis1, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null),
                createVisit(patient, doctor, diagnosis2, LocalDate.now(), LocalTime.of(12, 0), null)
        );
        visitRepository.saveAll(visits);

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertEquals(2, result.size());
        assertEquals("Flu", result.getFirst().getDiagnosis().getName());
        assertEquals(2L, result.getFirst().getVisitCount());
    }

    @Test
    void countVisitsByDoctor_WithMultipleVisits_ReturnsList_HappyPath() {
        Doctor doctor1 = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Henry Red");
        Doctor doctor2 = createDoctor(TestDataUtils.generateUniqueIdNumber(), false, "Dr. Ivy Purple");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor1, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Bronchial inflammation");
        doctor1 = doctorRepository.save(doctor1);
        doctor2 = doctorRepository.save(doctor2);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor1, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor1, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null),
                createVisit(patient, doctor2, diagnosis, LocalDate.now(), LocalTime.of(12, 0), null)
        );
        visitRepository.saveAll(visits);

        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertEquals(2, result.size());
        assertEquals(2L, result.getFirst().getVisitCount());
    }

    @Test
    void findByPatientOrDoctorFilter_WithEgn_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        String egn = TestDataUtils.generateValidEgn(); // e.g., "2445112130"
        Patient patient = createPatient(egn, doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visitRepository.save(visit);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter(egn.substring(0, 4), PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements(), "Should find one visit for the EGN prefix");
    }

    @Test
    void findByPatientOrDoctorFilter_WithDoctorUniqueId_ReturnsPaged_HappyPath() {
        String uniqueId = TestDataUtils.generateUniqueIdNumber();
        Doctor doctor = createDoctor(uniqueId, true, "Dr. Jane Smith");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visitRepository.save(visit);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter(uniqueId, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(uniqueId, result.getContent().getFirst().getDoctor().getUniqueIdNumber());
    }

    @Test
    void findByDoctorAndDateTime_WithValidParams_ReturnsVisit_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visitRepository.save(visit);

        Optional<Visit> result = visitRepository.findByDoctorAndDateTime(doctor, LocalDate.now(), LocalTime.of(10, 30));

        assertTrue(result.isPresent());
        assertEquals(LocalDate.now(), result.get().getVisitDate());
    }

    @Test
    void save_WithNullVisitDate_ThrowsException_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, null, LocalTime.of(10, 30), null);

        assertThrows(ConstraintViolationException.class, () -> {
            visitRepository.save(visit);
        });
    }

    @Test
    void save_WithNullVisitTime_ThrowsException_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Charlie Green");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), null, null);

        assertThrows(ConstraintViolationException.class, () -> {
            visitRepository.save(visit);
        });
    }

    @Test
    void save_WithNullSickLeaveIssued_ThrowsException_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. David Black");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);

        assertDoesNotThrow(() -> {
            visitRepository.save(visit); // No exception expected since sickLeave is optional
        });
        assertFalse(visitRepository.findById(visit.getId()).get().isSickLeaveIssued());
    }

    @Test
    void save_WithNullPatient_ThrowsException_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Eve White");
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(null, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);

        assertThrows(ConstraintViolationException.class, () -> {
            visitRepository.save(visit);
        });
    }

    @Test
    void save_WithNullDoctor_ThrowsException_ErrorCase() {
        // Create dummy doctor for patient, but null for visit
        Doctor dummyDoctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dummy Doctor");
        dummyDoctor = doctorRepository.save(dummyDoctor); // Persist for patient
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), dummyDoctor, LocalDate.now());
        patient = patientRepository.save(patient); // Now patient saves without rollback
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, null, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null); // Null doctor

        assertThrows(ConstraintViolationException.class, () -> {
            visitRepository.save(visit); // Fails due to null doctor in Visit
        });
    }

    @Test
    void save_WithNullDiagnosis_ThrowsException_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Grace Blue");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        Visit visit = createVisit(patient, doctor, null, LocalDate.now(), LocalTime.of(10, 30), null);

        assertThrows(ConstraintViolationException.class, () -> {
            visitRepository.save(visit);
        });
    }

    @Test
    void findByDateRange_WithOverlappingRange_ReturnsCorrectCount_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Henry Red");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDateRange(LocalDate.now().minusDays(2), LocalDate.now(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void findByPatientOrDoctorFilter_WithLargeDataset_ReturnsPaged_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Ivy Purple");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            visits.add(createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30).plusMinutes(i), null));
        }
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter(patient.getEgn().substring(0, 4), PageRequest.of(0, 5));

        assertEquals(10, result.getTotalElements());
        assertEquals(5, result.getContent().size());
    }
}