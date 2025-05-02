package nbu.cscb869.data.repositories.integration;

import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VisitRepositoryIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    private Visit visit;
    private Patient patient;
    private Doctor doctor;
    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        doctor = new Doctor();
        doctor.setName("Dr. GP");
        doctor.setUniqueIdNumber("67890");
        doctor.setGeneralPractitioner(true);
        doctor = doctorRepository.save(doctor);

        patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn("1234567890");
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(doctor);
        patient = patientRepository.save(patient);

        diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        diagnosis.setDescription("Influenza");
        diagnosis = diagnosisRepository.save(diagnosis);

        visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit = visitRepository.save(visit);
    }

    @Test
    void FindByPatientAndIsDeletedFalse_ValidPatient_ReturnsPagedVisits() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> result = visitRepository.findByPatientAndIsDeletedFalse(patient, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getVisitDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void FindByPatientAndIsDeletedFalse_NoVisits_ReturnsEmptyPage() {
        visitRepository.deleteAll();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> result = visitRepository.findByPatientAndIsDeletedFalse(patient, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void FindByPatientAndIsDeletedFalse_DeletedVisit_ExcludesFromResult() {
        visitRepository.softDeleteById(visit.getId());
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> result = visitRepository.findByPatientAndIsDeletedFalse(patient, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void FindByDoctorAndIsDeletedFalse_ValidDoctor_ReturnsPagedVisits() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> result = visitRepository.findByDoctorAndIsDeletedFalse(doctor, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getVisitDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void FindByDoctorAndVisitDateBetweenAndIsDeletedFalse_ValidDates_ReturnsPagedVisits() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> result = visitRepository.findByDoctorAndVisitDateBetweenAndIsDeletedFalse(doctor, startDate, endDate, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getVisitDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void FindByDiagnosisAndIsDeletedFalse_ValidDiagnosis_ReturnsPagedVisits() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> result = visitRepository.findByDiagnosisAndIsDeletedFalse(diagnosis, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getVisitDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void FindByVisitDateBetweenAndIsDeletedFalse_ValidDates_ReturnsPagedVisits() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> result = visitRepository.findByVisitDateBetweenAndIsDeletedFalse(startDate, endDate, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getVisitDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void FindAllDeleted_ValidCall_ReturnsDeletedVisits() {
        visitRepository.softDeleteById(visit.getId());
        List<Visit> result = visitRepository.findAllDeleted();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getVisitDate()).isEqualTo(LocalDate.now());
    }
}