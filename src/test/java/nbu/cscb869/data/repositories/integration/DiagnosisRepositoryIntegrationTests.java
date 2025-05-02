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
class DiagnosisRepositoryIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private VisitRepository visitRepository;

    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        diagnosis.setDescription("Influenza");
        diagnosis = diagnosisRepository.save(diagnosis);
    }

    @Test
    void FindByName_ValidName_ReturnsDiagnosis() {
        Optional<Diagnosis> result = diagnosisRepository.findByName("Flu");
        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Influenza");
    }

    @Test
    void FindByName_NonExistentName_ReturnsEmpty() {
        Optional<Diagnosis> result = diagnosisRepository.findByName("Cancer");
        assertThat(result).isEmpty();
    }

    @Test
    void FindByName_DeletedDiagnosis_ExcludesFromResult() {
        diagnosisRepository.softDeleteById(diagnosis.getId());
        Optional<Diagnosis> result = diagnosisRepository.findByName("Flu");
        assertThat(result).isEmpty();
    }

    @Test
    void CountPatientsByDiagnosis_ValidDiagnosis_ReturnsCount() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. GP");
        doctor.setUniqueIdNumber("67890");
        doctor.setGeneralPractitioner(true);
        doctor = doctorRepository.save(doctor);

        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn("1234567890");
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(doctor);
        patient = patientRepository.save(patient);

        Visit visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visitRepository.save(visit);

        long result = diagnosisRepository.countPatientsByDiagnosis(diagnosis);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void CountPatientsByDiagnosis_NoPatients_ReturnsZero() {
        long result = diagnosisRepository.countPatientsByDiagnosis(diagnosis);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void FindMostFrequentDiagnoses_ValidPageable_ReturnsPagedResults() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. GP");
        doctor.setUniqueIdNumber("67890");
        doctor.setGeneralPractitioner(true);
        doctor = doctorRepository.save(doctor);

        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setEgn("1234567890");
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(doctor);
        patient = patientRepository.save(patient);

        Visit visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visitRepository.save(visit);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> result = diagnosisRepository.findMostFrequentDiagnoses(pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()[0]).isInstanceOf(Diagnosis.class);
        assertThat(result.getContent().getFirst()[1]).isEqualTo(1L);
    }

    @Test
    void FindMostFrequentDiagnoses_NoVisits_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> result = diagnosisRepository.findMostFrequentDiagnoses(pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void FindAllDeleted_ValidCall_ReturnsDeletedDiagnoses() {
        diagnosisRepository.softDeleteById(diagnosis.getId());
        List<Diagnosis> result = diagnosisRepository.findAllDeleted();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Flu");
    }
}