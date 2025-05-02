package nbu.cscb869.data.repositories.integration;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.SickLeaveRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DoctorRepositoryIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private SickLeaveRepository sickLeaveRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Doctor doctor;
    private Doctor gpDoctor;
    private Specialty specialty;

    @BeforeEach
    void setUp() {
        specialty = new Specialty();
        specialty.setName("Cardiology");
        specialty = specialtyRepository.save(specialty);

        doctor = new Doctor();
        doctor.setName("Dr. Regular");
        doctor.setUniqueIdNumber("12345");
        doctor.setGeneralPractitioner(false);
        doctor.setSpecialties(Set.of(specialty));
        doctor = doctorRepository.save(doctor);

        gpDoctor = new Doctor();
        gpDoctor.setName("Dr. GP");
        gpDoctor.setUniqueIdNumber("67890");
        gpDoctor.setGeneralPractitioner(true);
        gpDoctor.setSpecialties(Set.of(specialty));
        gpDoctor = doctorRepository.save(gpDoctor);
    }

    @Test
    void FindByUniqueIdNumber_ValidId_ReturnsDoctor() {
        Optional<Doctor> result = doctorRepository.findByUniqueIdNumber("12345");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Dr. Regular");
        assertThat(result.get().getSpecialties()).hasSize(1);
        assertThat(result.get().getSpecialties().iterator().next().getName()).isEqualTo("Cardiology");
    }

    @Test
    void FindByUniqueIdNumber_NonExistentId_ReturnsEmpty() {
        Optional<Doctor> result = doctorRepository.findByUniqueIdNumber("99999");
        assertThat(result).isEmpty();
    }

    @Test
    void FindByUniqueIdNumber_DeletedDoctor_ExcludesFromResult() {
        doctorRepository.softDeleteById(doctor.getId());
        Optional<Doctor> result = doctorRepository.findByUniqueIdNumber("12345");
        assertThat(result).isEmpty();
    }

    @Test
    void FindByIsGeneralPractitionerTrue_ValidPageable_ReturnsPagedGeneralPractitioners() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Doctor> result = doctorRepository.findByIsGeneralPractitionerTrue(pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getUniqueIdNumber()).isEqualTo("67890");
    }

    @Test
    void FindByIsGeneralPractitionerTrue_NoGeneralPractitioners_ReturnsEmptyPage() {
        doctorRepository.deleteAll();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Doctor> result = doctorRepository.findByIsGeneralPractitionerTrue(pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void CountPatientsByGeneralPractitioner_ValidDoctorWithPatients_ReturnsPatientCount() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn("1234567890");
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(gpDoctor);
        patientRepository.save(patient);
        long result = doctorRepository.countPatientsByGeneralPractitioner(gpDoctor);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void CountPatientsByGeneralPractitioner_NoPatients_ReturnsZero() {
        long result = doctorRepository.countPatientsByGeneralPractitioner(gpDoctor);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void CountVisitsByDoctor_ValidDoctorWithVisits_ReturnsVisitCount() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn("1234567890");
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(gpDoctor);
        patient = patientRepository.save(patient);

        Visit visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visitRepository.save(visit);

        long result = doctorRepository.countVisitsByDoctor(doctor);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void CountVisitsByDoctor_NoVisits_ReturnsZero() {
        long result = doctorRepository.countVisitsByDoctor(doctor);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void FindDoctorsBySickLeaveCount_ValidPageable_ReturnsPagedResults() {
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn("1234567890");
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(gpDoctor);
        patient = patientRepository.save(patient);

        Visit visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setSickLeaveIssued(true);
        visit = visitRepository.save(visit);

        SickLeave sickLeave = new SickLeave();
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave.setVisit(visit);
        sickLeaveRepository.save(sickLeave);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> result = doctorRepository.findDoctorsBySickLeaveCount(pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()[0]).isInstanceOf(Doctor.class);
        assertThat(result.getContent().getFirst()[1]).isEqualTo(1L);
    }

    @Test
    void FindDoctorsBySickLeaveCount_NoSickLeaves_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> result = doctorRepository.findDoctorsBySickLeaveCount(pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void FindAllDeleted_ValidCall_ReturnsDeletedDoctors() {
        doctorRepository.softDeleteById(doctor.getId());
        List<Doctor> result = doctorRepository.findAllDeleted();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUniqueIdNumber()).isEqualTo("12345");
    }
}