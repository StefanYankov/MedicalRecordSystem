package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.contracts.VisitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class VisitServiceImplIntegrationTests {

    @Autowired
    private VisitService visitService;

    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DiagnosisRepository diagnosisRepository;

    private Doctor testDoctor;
    private Patient testPatient;
    private Diagnosis testDiagnosis;

    @BeforeEach
    void setUp() {
        testDoctor = doctorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Doctor d = new Doctor();
            d.setKeycloakId(TestDataUtils.generateKeycloakId());
            d.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            d.setName("Dr. Test");
            d.setGeneralPractitioner(true);
            return doctorRepository.save(d);
        });

        testPatient = patientRepository.findByKeycloakId("patient-owner-id").orElseGet(() -> {
            Patient p = new Patient();
            p.setKeycloakId("patient-owner-id");
            p.setEgn(TestDataUtils.generateValidEgn());
            p.setName("Patient Owner");
            p.setGeneralPractitioner(testDoctor);
            p.setLastInsurancePaymentDate(LocalDate.now());
            return patientRepository.save(p);
        });

        testDiagnosis = diagnosisRepository.findByName("Flu").orElseGet(() -> {
            Diagnosis d = new Diagnosis();
            d.setName("Flu");
            return diagnosisRepository.save(d);
        });

        visitRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // The email sending test has been removed as this is no longer the responsibility of the VisitService.
    // This test will be moved to the VisitControllerIntegrationTests.

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void create_WithValidDataAndChildren_ShouldPersistAggregate_HappyPath() {
        MedicineCreateDTO medDto = new MedicineCreateDTO("Water", "8 glasses", "Daily");
        TreatmentCreateDTO treatmentDto = new TreatmentCreateDTO();
        treatmentDto.setDescription("Rest");
        treatmentDto.setMedicines(List.of(medDto));

        VisitCreateDTO createDTO = new VisitCreateDTO();
        createDTO.setPatientId(testPatient.getId());
        createDTO.setDoctorId(testDoctor.getId());
        createDTO.setDiagnosisId(testDiagnosis.getId());
        createDTO.setVisitDate(LocalDate.now());
        createDTO.setVisitTime(LocalTime.of(10, 0));
        createDTO.setTreatment(treatmentDto);

        VisitViewDTO result = visitService.create(createDTO);

        assertNotNull(result.getId());
        Visit savedVisit = visitRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedVisit);
        assertNotNull(savedVisit.getTreatment());
        assertEquals(1, savedVisit.getTreatment().getMedicines().size());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void create_AsPatient_ShouldBeDenied_ErrorCase() {
        VisitCreateDTO createDTO = new VisitCreateDTO();
        assertThrows(AccessDeniedException.class, () -> visitService.create(createDTO));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void create_WithInvalidInsurance_ShouldThrowException_ErrorCase() {
        testPatient.setLastInsurancePaymentDate(LocalDate.now().minusMonths(7));
        patientRepository.save(testPatient);

        VisitCreateDTO createDTO = new VisitCreateDTO();
        createDTO.setPatientId(testPatient.getId());
        createDTO.setDoctorId(testDoctor.getId());
        createDTO.setDiagnosisId(testDiagnosis.getId());
        createDTO.setVisitDate(LocalDate.now());
        createDTO.setVisitTime(LocalTime.of(11, 0));

        assertThrows(InvalidInputException.class, () -> visitService.create(createDTO));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void create_WithBookedTimeSlot_ShouldThrowException_ErrorCase() {
        Visit existingVisit = new Visit(LocalDate.now(), LocalTime.of(14, 0), testPatient, testDoctor, testDiagnosis, null, null);
        visitRepository.save(existingVisit);

        VisitCreateDTO createDTO = new VisitCreateDTO();
        createDTO.setPatientId(testPatient.getId());
        createDTO.setDoctorId(testDoctor.getId());
        createDTO.setDiagnosisId(testDiagnosis.getId());
        createDTO.setVisitDate(LocalDate.now());
        createDTO.setVisitTime(LocalTime.of(14, 0));

        assertThrows(InvalidInputException.class, () -> visitService.create(createDTO));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void update_WithValidData_ShouldUpdateAggregate_HappyPath() {
        Visit visit = visitRepository.save(new Visit(LocalDate.now(), LocalTime.now(), testPatient, testDoctor, testDiagnosis, null, null));
        VisitUpdateDTO updateDTO = new VisitUpdateDTO(visit.getId(), LocalDate.now(), LocalTime.of(12, 0), testPatient.getId(), testDoctor.getId(), testDiagnosis.getId(), null, null);

        visitService.update(updateDTO);

        Visit updatedVisit = visitRepository.findById(visit.getId()).get();
        assertEquals(LocalTime.of(12, 0), updatedVisit.getVisitTime());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void delete_AsDoctor_ShouldDeleteAggregate_HappyPath() {
        Visit visit = visitRepository.save(new Visit(LocalDate.now(), LocalTime.now(), testPatient, testDoctor, testDiagnosis, null, null));
        long visitId = visit.getId();

        visitService.delete(visitId);

        assertFalse(visitRepository.findById(visitId).isPresent());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
    void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
        Visit visit = visitRepository.save(new Visit(LocalDate.now(), LocalTime.now(), testPatient, testDoctor, testDiagnosis, null, null));

        VisitViewDTO result = visitService.getById(visit.getId());

        assertNotNull(result);
        assertEquals(visit.getId(), result.getId());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "other-patient-id", authorities = "ROLE_PATIENT")
    void getById_AsOtherPatient_ShouldBeDenied_ErrorCase() {
        Visit visit = visitRepository.save(new Visit(LocalDate.now(), LocalTime.now(), testPatient, testDoctor, testDiagnosis, null, null));
        long visitId = visit.getId();

        assertThrows(AccessDeniedException.class, () -> visitService.getById(visitId));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getAll_AsAdmin_ShouldReturnPage_HappyPath() throws ExecutionException, InterruptedException {
        visitRepository.save(new Visit(LocalDate.now(), LocalTime.now(), testPatient, testDoctor, testDiagnosis, null, null));

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        CompletableFuture<Page<VisitViewDTO>> future = visitService.getAll(0, 10, "visitDate", true, null);
        Page<VisitViewDTO> result = future.get();

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getVisitCountByDoctor_ShouldReturnCorrectCount_HappyPath() {
        visitRepository.save(new Visit(LocalDate.now(), LocalTime.now(), testPatient, testDoctor, testDiagnosis, null, null));

        var result = visitService.getVisitCountByDoctor();

        assertFalse(result.isEmpty());
        assertEquals(1, result.getFirst().getVisitCount());
        assertEquals(testDoctor.getName(), result.get(0).getDoctor().getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getMostFrequentDiagnoses_ShouldReturnCorrectCount_HappyPath() {
        visitRepository.save(new Visit(LocalDate.now(), LocalTime.now(), testPatient, testDoctor, testDiagnosis, null, null));

        var result = visitService.getMostFrequentDiagnoses();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testDiagnosis.getName(), result.get(0).getDiagnosis().getName());
    }
}
