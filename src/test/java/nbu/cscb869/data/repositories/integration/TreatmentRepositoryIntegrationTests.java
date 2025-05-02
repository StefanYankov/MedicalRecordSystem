package nbu.cscb869.data.repositories.integration;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.TreatmentRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TreatmentRepositoryIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private Treatment treatment;
    private Visit visit;

    @BeforeEach
    void setUp() {
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

        visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit = visitRepository.save(visit);

        treatment = new Treatment();
        treatment.setInstructions("Rest for 3 days");
        treatment.setVisit(visit);
        treatment = treatmentRepository.save(treatment);
    }

    @Test
    void FindByVisitAndIsDeletedFalse_ValidVisit_ReturnsTreatment() {
        Optional<Treatment> result = treatmentRepository.findByVisitAndIsDeletedFalse(visit);
        assertThat(result).isPresent();
        assertThat(result.get().getInstructions()).isEqualTo("Rest for 3 days");
    }

    @Test
    void FindByVisitAndIsDeletedFalse_DeletedTreatment_ExcludesFromResult() {
        treatmentRepository.softDeleteById(treatment.getId());
        Optional<Treatment> result = treatmentRepository.findByVisitAndIsDeletedFalse(visit);
        assertThat(result).isEmpty();
    }

    @Test
    void FindAllDeleted_ValidCall_ReturnsDeletedTreatments() {
        treatmentRepository.softDeleteById(treatment.getId());
        List<Treatment> result = treatmentRepository.findAllDeleted();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getInstructions()).isEqualTo("Rest for 3 days");
    }
}