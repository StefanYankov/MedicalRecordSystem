package nbu.cscb869.data.repositories.integrationtests;

import jakarta.validation.ConstraintViolationException;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
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

    private Doctor testDoctor;
    private Patient testPatient;
    private Diagnosis testDiagnosis;

    @BeforeEach
    void setUp() {
        sickLeaveRepository.deleteAll();
        treatmentRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();

        testDoctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Test");
        testPatient = createPatient(TestDataUtils.generateValidEgn(), testDoctor, LocalDate.now());
        testDiagnosis = createDiagnosis("Test Flu", "Test viral infection");
        doctorRepository.save(testDoctor);
        patientRepository.save(testPatient);
        diagnosisRepository.save(testDiagnosis);
    }

    private Doctor createDoctor(String uniqueIdNumber, boolean isGeneralPractitioner, String name) {
        return Doctor.builder()
                .uniqueIdNumber(uniqueIdNumber)
                .isGeneralPractitioner(isGeneralPractitioner)
                .name(name)
                .keycloakId(TestDataUtils.generateKeycloakId())
                .build();
    }

    private Patient createPatient(String egn, Doctor generalPractitioner, LocalDate lastInsurancePaymentDate) {
        return Patient.builder()
                .egn(egn)
                .generalPractitioner(generalPractitioner)
                .lastInsurancePaymentDate(lastInsurancePaymentDate)
                .keycloakId(TestDataUtils.generateKeycloakId())
                .build();
    }

    private Diagnosis createDiagnosis(String name, String description) {
        return Diagnosis.builder()
                .name(name)
                .description(description)
                .build();
    }

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate, LocalTime visitTime, VisitStatus status, SickLeave sickLeave) {
        Visit visit = Visit.builder()
                .patient(patient)
                .doctor(doctor)
                .diagnosis(diagnosis)
                .visitDate(visitDate)
                .visitTime(visitTime)
                .status(status)
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
        Visit visit = createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
        Visit saved = visitRepository.save(visit);

        Optional<Visit> found = visitRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(LocalDate.now(), found.get().getVisitDate());
        assertEquals(LocalTime.of(10, 30), found.get().getVisitTime());
        assertEquals(VisitStatus.COMPLETED, found.get().getStatus());
    }

    @Test
    void save_WithSickLeave_SavesSuccessfully_HappyPath() {
        Visit visit = createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
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
        Visit visit = createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment("Antibiotic therapy", visit);
        treatment = treatmentRepository.save(treatment);
        visit.setTreatment(treatment);
        treatment.setVisit(visit);
        visitRepository.save(visit);

        Optional<Visit> found = visitRepository.findById(visit.getId());

        assertTrue(found.isPresent());
        assertNotNull(found.get().getTreatment());
        assertEquals("Antibiotic therapy", found.get().getTreatment().getDescription());
    }

    @Test
    void findByPatient_WithMultipleVisits_ReturnsPaged_HappyPath() {
        List<Visit> visits = List.of(
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null),
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), VisitStatus.COMPLETED, null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByPatient(testPatient, PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByDoctor_WithMultipleVisits_ReturnsPaged_HappyPath() {
        List<Visit> visits = List.of(
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null),
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), VisitStatus.COMPLETED, null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDoctor(testDoctor, PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByDateRange_WithMultipleVisits_ReturnsPaged_HappyPath() {
        List<Visit> visits = List.of(
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null),
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), VisitStatus.COMPLETED, null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDateRange(LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByDoctorAndDateRange_WithMultipleVisits_ReturnsPaged_HappyPath() {
        List<Visit> visits = List.of(
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null),
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), VisitStatus.COMPLETED, null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(testDoctor, LocalDate.now().minusDays(5), LocalDate.now(), PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByDiagnosis_WithMultipleVisits_ReturnsPaged_HappyPath() {
        List<Visit> visits = List.of(
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null),
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), VisitStatus.COMPLETED, null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDiagnosis(testDiagnosis, PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findMostFrequentDiagnoses_WithMultipleVisits_ReturnsList_HappyPath() {
        Diagnosis diagnosis2 = createDiagnosis("Cold", "Common cold");
        diagnosisRepository.save(diagnosis2);
        List<Visit> visits = List.of(
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null),
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), VisitStatus.COMPLETED, null),
                createVisit(testPatient, testDoctor, diagnosis2, LocalDate.now(), LocalTime.of(12, 0), VisitStatus.COMPLETED, null)
        );
        visitRepository.saveAll(visits);

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertEquals(2, result.size());
        assertEquals("Test Flu", result.getFirst().getDiagnosis().getName());
        assertEquals(2L, result.getFirst().getVisitCount());
    }

    @Test
    void countVisitsByDoctor_WithMultipleVisits_ReturnsList_HappyPath() {
        Doctor doctor2 = createDoctor(TestDataUtils.generateUniqueIdNumber(), false, "Dr. Specialist");
        doctorRepository.save(doctor2);
        List<Visit> visits = List.of(
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null),
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), VisitStatus.COMPLETED, null),
                createVisit(testPatient, doctor2, testDiagnosis, LocalDate.now(), LocalTime.of(12, 0), VisitStatus.COMPLETED, null)
        );
        visitRepository.saveAll(visits);

        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertEquals(2, result.size());
        assertEquals(2L, result.getFirst().getVisitCount());
    }

    @Test
    void findByPatientOrDoctorFilter_WithEgn_ReturnsPaged_HappyPath() {
        Visit visit = createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
        visitRepository.save(visit);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter(testPatient.getEgn().substring(0, 4), PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements(), "Should find one visit for the EGN prefix");
    }

    @Test
    void findByPatientOrDoctorFilter_WithDoctorUniqueId_ReturnsPaged_HappyPath() {
        Visit visit = createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
        visitRepository.save(visit);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter(testDoctor.getUniqueIdNumber(), PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(testDoctor.getUniqueIdNumber(), result.getContent().getFirst().getDoctor().getUniqueIdNumber());
    }

    @Test
    void findByDoctorAndDateTime_WithValidParams_ReturnsVisit_HappyPath() {
        Visit visit = createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
        visitRepository.save(visit);

        Optional<Visit> result = visitRepository.findByDoctorAndDateTime(testDoctor, LocalDate.now(), LocalTime.of(10, 30));

        assertTrue(result.isPresent());
        assertEquals(LocalDate.now(), result.get().getVisitDate());
    }

    @Test
    void save_WithNullVisitDate_ThrowsException_ErrorCase() {
        Visit visit = createVisit(testPatient, testDoctor, testDiagnosis, null, LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
        assertThrows(ConstraintViolationException.class, () -> visitRepository.save(visit));
    }

    @Test
    void save_WithNullVisitTime_ThrowsException_ErrorCase() {
        Visit visit = createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), null, VisitStatus.COMPLETED, null);
        assertThrows(ConstraintViolationException.class, () -> visitRepository.save(visit));
    }

    @Test
    void save_WithNullPatient_ThrowsException_ErrorCase() {
        Visit visit = createVisit(null, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
        assertThrows(ConstraintViolationException.class, () -> visitRepository.save(visit));
    }

    @Test
    void save_WithNullDoctor_ThrowsException_ErrorCase() {
        Visit visit = createVisit(testPatient, null, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
        assertThrows(ConstraintViolationException.class, () -> visitRepository.save(visit));
    }

    @Test
    void save_WithNullDiagnosis_IsAllowed_HappyPath() {
        Visit visit = createVisit(testPatient, testDoctor, null, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null);
        assertDoesNotThrow(() -> visitRepository.save(visit));
    }

    @Test
    void findByDateRange_WithOverlappingRange_ReturnsCorrectCount_EdgeCase() {
        List<Visit> visits = List.of(
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30), VisitStatus.COMPLETED, null),
                createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), VisitStatus.COMPLETED, null)
        );
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByDateRange(LocalDate.now().minusDays(2), LocalDate.now(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void findByPatientOrDoctorFilter_WithLargeDataset_ReturnsPaged_EdgeCase() {
        List<Visit> visits = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            visits.add(createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now(), LocalTime.of(10, 30).plusMinutes(i), VisitStatus.COMPLETED, null));
        }
        visitRepository.saveAll(visits);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter(testPatient.getEgn().substring(0, 4), PageRequest.of(0, 5));

        assertEquals(10, result.getTotalElements());
        assertEquals(5, result.getContent().size());
    }

    @Test
    void findByDoctorAndStatusAndVisitDateBetween_ShouldReturnCorrectVisits_HappyPath() {
        // Arrange
        Visit scheduledVisit = createVisit(testPatient, testDoctor, null, LocalDate.now().plusDays(1), LocalTime.NOON, VisitStatus.SCHEDULED, null);
        Visit completedVisit = createVisit(testPatient, testDoctor, testDiagnosis, LocalDate.now().plusDays(2), LocalTime.NOON, VisitStatus.COMPLETED, null);
        visitRepository.saveAll(List.of(scheduledVisit, completedVisit));

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(3);

        // Act
        Page<Visit> result = visitRepository.findByDoctorAndStatusAndVisitDateBetweenOrderByVisitDateAscVisitTimeAsc(
                testDoctor, VisitStatus.SCHEDULED, startDate, endDate, PageRequest.of(0, 10));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(VisitStatus.SCHEDULED, result.getContent().get(0).getStatus());
    }
}
