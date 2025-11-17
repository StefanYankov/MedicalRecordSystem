package nbu.cscb869.data.repositories.integrationtests;

import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
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
@Import(SickLeaveRepositoryIntegrationTests.TestConfig.class)
class SickLeaveRepositoryIntegrationTests {

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
    private SickLeaveRepository sickLeaveRepository;
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
        sickLeaveRepository.deleteAll();
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
            sickLeave.setVisit(visit); // Ensure bidirectional link
        }
        return visit;
    }

    private SickLeave createSickLeave(Visit visit, LocalDate startDate, int durationDays) {
        return SickLeave.builder()
                .visit(visit)
                .startDate(startDate)
                .durationDays(durationDays)
                .build();
    }

    @Test
    void save_WithValidSickLeave_SavesSuccessfully_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Influenza");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        visit = visitRepository.save(visit);
        SickLeave sickLeave = createSickLeave(visit, LocalDate.now(), 5);
        sickLeave = sickLeaveRepository.save(sickLeave);
        visit.setSickLeave(sickLeave);
        visitRepository.save(visit);

        Optional<SickLeave> foundSickLeave = sickLeaveRepository.findById(sickLeave.getId());
        Optional<Visit> foundVisit = visitRepository.findById(visit.getId());

        assertTrue(foundSickLeave.isPresent());
        assertEquals(5, foundSickLeave.get().getDurationDays());
        assertTrue(foundVisit.isPresent());
        assertNotNull(foundVisit.get().getSickLeave()); // Should now pass
    }

    @Test
    void findAll_WithMultipleSickLeaves_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Bronchitis", "Acute bronchitis");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null)
        );
        visitRepository.saveAll(visits);
        List<SickLeave> sickLeaves = List.of(
                createSickLeave(visits.getFirst(), LocalDate.now(), 5),
                createSickLeave(visits.get(1), LocalDate.now().minusDays(1), 3)
        );
        sickLeaveRepository.saveAll(sickLeaves);

        Page<SickLeave> result = sickLeaveRepository.findAll(PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findYearMonthWithMostSickLeaves_WithMultipleSickLeaves_ReturnsSorted_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusMonths(1), LocalTime.of(11, 0), null),
                createVisit(patient, doctor, diagnosis, LocalDate.now().minusMonths(1), LocalTime.of(12, 0), null)
        );
        visitRepository.saveAll(visits);
        List<SickLeave> sickLeaves = List.of(
                createSickLeave(visits.getFirst(), LocalDate.now(), 5),
                createSickLeave(visits.get(1), LocalDate.now().minusMonths(1), 3),
                createSickLeave(visits.get(2), LocalDate.now().minusMonths(1), 4)
        );
        sickLeaveRepository.saveAll(sickLeaves);

        List<YearMonthSickLeaveCountDTO> result = sickLeaveRepository.findYearMonthWithMostSickLeaves();

        assertEquals(2, result.size());
        assertEquals(2, result.getFirst().getCount());
        assertEquals(LocalDate.now().minusMonths(1).getMonthValue(), result.getFirst().getMonth());
    }

    @Test
    void findDoctorsWithMostSickLeaves_WithMultipleSickLeaves_ReturnsSorted_HappyPath() {
        Doctor doctor1 = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        Doctor doctor2 = createDoctor(TestDataUtils.generateUniqueIdNumber(), false, "Dr. Charlie Green");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor1, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Influenza");
        doctorRepository.saveAll(List.of(doctor1, doctor2));
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = List.of(
                createVisit(patient, doctor1, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null),
                createVisit(patient, doctor1, diagnosis, LocalDate.now().minusDays(1), LocalTime.of(11, 0), null),
                createVisit(patient, doctor2, diagnosis, LocalDate.now(), LocalTime.of(12, 0), null)
        );
        visitRepository.saveAll(visits);
        List<SickLeave> sickLeaves = List.of(
                createSickLeave(visits.getFirst(), LocalDate.now(), 5),
                createSickLeave(visits.get(1), LocalDate.now().minusDays(1), 3),
                createSickLeave(visits.get(2), LocalDate.now(), 7)
        );
        sickLeaveRepository.saveAll(sickLeaves);

        List<DoctorSickLeaveCountDTO> result = sickLeaveRepository.findDoctorsWithMostSickLeaves();

        assertEquals(2, result.size());
        assertEquals(2, result.getFirst().getSickLeaveCount());
    }

    @Test
    void findAll_WithNoSickLeaves_ReturnsEmptyPage_ErrorCase() {
        Page<SickLeave> result = sickLeaveRepository.findAll(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void findYearMonthWithMostSickLeaves_WithNoSickLeaves_ReturnsEmpty_ErrorCase() {
        assertTrue(sickLeaveRepository.findYearMonthWithMostSickLeaves().isEmpty());
    }

    @Test
    void findDoctorsWithMostSickLeaves_WithNoSickLeaves_ReturnsEmpty_ErrorCase() {
        assertTrue(sickLeaveRepository.findDoctorsWithMostSickLeaves().isEmpty());
    }

    @Test
    void findAll_WithLargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. David Black");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Pneumonia", "Lung infection");
        doctor = doctorRepository.save(doctor);
        patient = patientRepository.save(patient);
        diagnosis = diagnosisRepository.save(diagnosis);
        List<Visit> visits = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            visits.add(createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null));
        }
        visitRepository.saveAll(visits);
        List<SickLeave> sickLeaves = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            sickLeaves.add(createSickLeave(visits.get(i), LocalDate.now(), 5));
        }
        sickLeaveRepository.saveAll(sickLeaves);

        Page<SickLeave> result = sickLeaveRepository.findAll(PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
    }
}
