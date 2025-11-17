package nbu.cscb869.data.repositories.integrationtests;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(PatientRepositoryIntegrationTests.TestConfig.class)
class PatientRepositoryIntegrationTests {

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
    private PatientRepository patientRepository;
    @Autowired
    private DoctorRepository doctorRepository;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
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

    @Test
    void save_WithValidPatient_SavesSuccessfully_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Patient saved = patientRepository.save(patient);

        Optional<Patient> found = patientRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getEgn(), found.get().getEgn());
    }

    @Test
    void findByEgn_WithExistingEgn_ReturnsPatient_HappyPath() {
        String egn = TestDataUtils.generateValidEgn();
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        doctor = doctorRepository.save(doctor);
        Patient patient = createPatient(egn, doctor, LocalDate.now());
        patientRepository.save(patient);

        Optional<Patient> found = patientRepository.findByEgn(egn);

        assertTrue(found.isPresent());
        assertEquals(egn, found.get().getEgn());
    }

    @Test
    void findByGeneralPractitioner_WithMultiplePatients_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        doctor = doctorRepository.save(doctor);
        List<Patient> patients = List.of(
                createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now()),
                createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now())
        );
        patientRepository.saveAll(patients);

        Page<Patient> result = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void countPatientsByGeneralPractitioner_WithMultiplePatients_ReturnsList_HappyPath() {
        Doctor doctor1 = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Doctor doctor2 = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Charlie Green");
        doctorRepository.saveAll(List.of(doctor1, doctor2));
        List<Patient> patients = List.of(
                createPatient(TestDataUtils.generateValidEgn(), doctor1, LocalDate.now()),
                createPatient(TestDataUtils.generateValidEgn(), doctor1, LocalDate.now()),
                createPatient(TestDataUtils.generateValidEgn(), doctor2, LocalDate.now())
        );
        patientRepository.saveAll(patients);

        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getPatientCount());
        assertEquals(1, result.get(1).getPatientCount());
    }

    @Test
    void findByEgnContaining_WithPartialEgn_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. David Black");
        doctor = doctorRepository.save(doctor);
        String egn = TestDataUtils.generateValidEgn();
        Patient patient = createPatient(egn, doctor, LocalDate.now());
        patientRepository.save(patient);

        Page<Patient> result = patientRepository.findByEgnContaining(egn.substring(0, 4), PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(egn, result.getContent().getFirst().getEgn());
    }

    @Test
    void save_WithDuplicateEgn_ThrowsException_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Eve White");
        doctor = doctorRepository.save(doctor);
        String egn = TestDataUtils.generateValidEgn();
        Patient patient1 = createPatient(egn, doctor, LocalDate.now());
        Patient patient2 = createPatient(egn, doctor, LocalDate.now());
        patientRepository.save(patient1);

        assertThrows(DataIntegrityViolationException.class, () -> patientRepository.save(patient2));
    }

    @Test
    void findByEgn_WithNonExistentEgn_ReturnsEmpty_ErrorCase() {
        assertFalse(patientRepository.findByEgn("1234567890").isPresent());
    }

    @Test
    void findByGeneralPractitioner_WithNoPatients_ReturnsEmptyPage_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Frank Gray");
        doctor = doctorRepository.save(doctor);

        Page<Patient> result = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void countPatientsByGeneralPractitioner_WithNoPatients_ReturnsEmptyList_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Grace Blue");
        doctorRepository.save(doctor);

        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();

        assertEquals(0, result.size());
    }

    @Test
    void findByEgnContaining_WithNoMatch_ReturnsEmptyPage_ErrorCase() {
        Page<Patient> result = patientRepository.findByEgnContaining("NONEXISTENT", PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void findByGeneralPractitioner_WithLargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Henry Red");
        doctor = doctorRepository.save(doctor);
        List<Patient> patients = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            patients.add(createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now()));
        }
        patientRepository.saveAll(patients);

        Page<Patient> result = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
    }

    @Test
    void findByEgnContaining_WithEmptyFilter_ReturnsAllPatients_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Ivy Purple");
        doctor = doctorRepository.save(doctor);
        List<Patient> patients = List.of(
                createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now()),
                createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now())
        );
        patientRepository.saveAll(patients);

        Page<Patient> result = patientRepository.findByEgnContaining("", PageRequest.of(0, 2));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }
}
