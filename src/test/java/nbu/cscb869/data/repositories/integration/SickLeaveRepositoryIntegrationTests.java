package nbu.cscb869.data.repositories.integration;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.SickLeaveRepository;
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
class SickLeaveRepositoryIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private SickLeaveRepository sickLeaveRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private SickLeave sickLeave;
    private Visit visit;
    private Patient patient;
    private Doctor doctor;

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

        visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setSickLeaveIssued(true);
        visit = visitRepository.save(visit);

        sickLeave = new SickLeave();
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave.setVisit(visit);
        sickLeave = sickLeaveRepository.save(sickLeave);
    }

    @Test
    void FindByPatient_ValidPatient_ReturnsPagedSickLeaves() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SickLeave> result = sickLeaveRepository.findByPatient(patient, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getDurationDays()).isEqualTo(5);
    }

    @Test
    void FindByPatient_NoSickLeaves_ReturnsEmptyPage() {
        sickLeaveRepository.deleteAll();
        Pageable pageable = PageRequest.of(0, 10);
        Page<SickLeave> result = sickLeaveRepository.findByPatient(patient, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void FindByPatient_DeletedSickLeave_ExcludesFromResult() {
        sickLeaveRepository.softDeleteById(sickLeave.getId());
        Pageable pageable = PageRequest.of(0, 10);
        Page<SickLeave> result = sickLeaveRepository.findByPatient(patient, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void FindByDoctor_ValidDoctor_ReturnsPagedSickLeaves() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SickLeave> result = sickLeaveRepository.findByDoctor(doctor, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getDurationDays()).isEqualTo(5);
    }

    @Test
    void FindMonthWithMostSickLeaves_ValidYear_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> result = sickLeaveRepository.findMonthWithMostSickLeaves(LocalDate.now().getYear(), pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()[0]).isEqualTo(LocalDate.now().getMonthValue());
        assertThat(result.getContent().getFirst()[1]).isEqualTo(1L);
    }

    @Test
    void FindAllDeleted_ValidCall_ReturnsDeletedSickLeaves() {
        sickLeaveRepository.softDeleteById(sickLeave.getId());
        List<SickLeave> result = sickLeaveRepository.findAllDeleted();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDurationDays()).isEqualTo(5);
    }
}