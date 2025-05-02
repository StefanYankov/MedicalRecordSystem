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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PatientRepositoryIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private VisitRepository visitRepository;

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
    }

    @Test
    void FindByEgn_ValidEgn_ReturnsPatient() {
        Optional<Patient> result = patientRepository.findByEgn("1234567890");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John Doe");
    }

    @Test
    void FindByEgn_NonExistentEgn_ReturnsEmpty() {
        Optional<Patient> result = patientRepository.findByEgn("9999999999");
        assertThat(result).isEmpty();
    }

    @Test
    void FindByEgn_DeletedPatient_ExcludesFromResult() {
        patientRepository.softDeleteById(patient.getId());
        Optional<Patient> result = patientRepository.findByEgn("1234567890");
        assertThat(result).isEmpty();
    }

    @Test
    void FindByGeneralPractitionerAndIsDeletedFalse_ValidGP_ReturnsPagedPatients() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> result = patientRepository.findByGeneralPractitionerAndIsDeletedFalse(doctor, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getEgn()).isEqualTo("1234567890");
    }

    @Test
    void FindByGeneralPractitionerAndIsDeletedFalse_NoPatients_ReturnsEmptyPage() {
        patientRepository.deleteAll();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> result = patientRepository.findByGeneralPractitionerAndIsDeletedFalse(doctor, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void FindByDiagnosis_ValidDiagnosis_ReturnsPagedPatients() {
        Visit visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visitRepository.save(visit);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> result = patientRepository.findByDiagnosis(diagnosis, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getEgn()).isEqualTo("1234567890");
    }

    @Test
    void FindByDiagnosis_NoPatients_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> result = patientRepository.findByDiagnosis(diagnosis, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void FindByEgnWithValidInsurance_ValidEgn_ReturnsPatient() {
        Optional<Patient> result = patientRepository.findByEgnWithValidInsurance("1234567890");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John Doe");
    }

    @Test
    void FindByEgnWithValidInsurance_ExpiredInsurance_ReturnsEmpty() {
        patient.setLastInsurancePaymentDate(LocalDate.now().minusDays(1));
        patientRepository.save(patient);
        Optional<Patient> result = patientRepository.findByEgnWithValidInsurance("1234567890");
        assertThat(result).isEmpty();
    }

    @Test
    void FindAllDeleted_ValidCall_ReturnsDeletedPatients() {
        patientRepository.softDeleteById(patient.getId());
        List<Patient> result = patientRepository.findAllDeleted();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEgn()).isEqualTo("1234567890");
    }
}