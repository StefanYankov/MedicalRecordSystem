package nbu.cscb869.data.repositories.integration;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.MedicineRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.TreatmentRepository;
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
class MedicineRepositoryIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private Medicine medicine;
    private Treatment treatment;

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

        Visit visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit = visitRepository.save(visit);

        treatment = new Treatment();
        treatment.setInstructions("Rest for 3 days");
        treatment.setVisit(visit);
        treatment = treatmentRepository.save(treatment);

        medicine = new Medicine();
        medicine.setName("Paracetamol");
        medicine.setDosage("500mg");
        medicine.setFrequency("As needed");
        medicine.setTreatment(treatment);
        medicine = medicineRepository.save(medicine);
    }

    @Test
    void FindByTreatmentAndIsDeletedFalse_ValidTreatment_ReturnsPagedMedicines() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Medicine> result = medicineRepository.findByTreatmentAndIsDeletedFalse(treatment, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Paracetamol");
        assertThat(result.getContent().getFirst().getFrequency()).isEqualTo("As needed");
    }

    @Test
    void FindByTreatmentAndIsDeletedFalse_DeletedMedicine_ExcludesFromResult() {
        medicineRepository.softDeleteById(medicine.getId());
        Pageable pageable = PageRequest.of(0, 10);
        Page<Medicine> result = medicineRepository.findByTreatmentAndIsDeletedFalse(treatment, pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void FindAllDeleted_ValidCall_ReturnsDeletedMedicines() {
        medicineRepository.softDeleteById(medicine.getId());
        List<Medicine> result = medicineRepository.findAllDeleted();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Paracetamol");
    }
}