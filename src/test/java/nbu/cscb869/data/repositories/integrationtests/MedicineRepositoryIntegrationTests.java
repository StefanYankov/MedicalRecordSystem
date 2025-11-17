package nbu.cscb869.data.repositories.integrationtests;

import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
@Import(MedicineRepositoryIntegrationTests.TestConfig.class)
class MedicineRepositoryIntegrationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ClientRegistrationRepository clientRegistrationRepository() {
            return Mockito.mock(ClientRegistrationRepository.class);
        }

        @Bean
        @Primary
        public JwtDecoder jwtDecoder() {
            return Mockito.mock(JwtDecoder.class);
        }
    }

    @MockBean
    private Keycloak keycloak;

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
    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @BeforeEach
    void setUp() {
        medicineRepository.deleteAll();
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
                .keycloakId(TestDataUtils.generateKeycloakId())
                .build();
    }

    private Patient createPatient(String egn, Doctor generalPractitioner, LocalDate lastInsurancePaymentDate) {
        return Patient.builder()
                .egn(egn)
                .name("Test Patient")
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

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate, LocalTime visitTime, SickLeave sickLeave) {
        Visit visit = Visit.builder()
                .patient(patient)
                .doctor(doctor)
                .diagnosis(diagnosis)
                .visitDate(visitDate)
                .visitTime(visitTime)
                .status(VisitStatus.COMPLETED) // FIX: Add default status
                .build();
        if (sickLeave != null) {
            visit.setSickLeave(sickLeave);
            sickLeave.setVisit(visit);
        }
        return visit;
    }

    private Treatment createTreatment(Visit visit, String description) {
        return Treatment.builder()
                .visit(visit)
                .description(description)
                .build();
    }

    private Medicine createMedicine(String name, String dosage, String frequency, Treatment treatment) {
        return Medicine.builder()
                .name(name)
                .dosage(dosage)
                .frequency(frequency)
                .treatment(treatment)
                .build();
    }

    @Test
    void save_WithValidMedicine_SavesSuccessfully_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Influenza");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment(visit, "Rest and hydration");
        treatment = treatmentRepository.save(treatment);
        Medicine medicine = createMedicine("Paracetamol", "500mg", "Twice daily", treatment);
        Medicine saved = medicineRepository.save(medicine);

        Optional<Medicine> found = medicineRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Paracetamol", found.get().getName());
    }

    @Test
    void findAll_WithMultipleMedicines_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment(visit, "Symptomatic relief");
        treatment = treatmentRepository.save(treatment);
        List<Medicine> medicines = List.of(
                createMedicine("Ibuprofen", "200mg", "Three times daily", treatment),
                createMedicine("Aspirin", "100mg", "Once daily", treatment)
        );
        medicineRepository.saveAll(medicines);

        Page<Medicine> result = medicineRepository.findAll(PageRequest.of(0, 2));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().anyMatch(m -> m.getName().equals("Ibuprofen")));
    }

    @Test
    void findAll_WithNoMedicines_ReturnsEmptyPage_ErrorCase() {
        Page<Medicine> result = medicineRepository.findAll(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void findAll_WithLargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Allergy", "Seasonal allergy");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visit = visitRepository.save(visit);
        Treatment treatment = createTreatment(visit, "Antihistamines");
        treatment = treatmentRepository.save(treatment);
        List<Medicine> medicines = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            medicines.add(createMedicine("Medicine" + i, "10mg", "Once daily", treatment));
        }
        medicineRepository.saveAll(medicines);

        Page<Medicine> result = medicineRepository.findAll(PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
    }
}
