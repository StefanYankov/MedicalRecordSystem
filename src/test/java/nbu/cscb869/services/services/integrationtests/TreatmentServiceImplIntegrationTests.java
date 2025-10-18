package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.MedicineCreateDTO;
import nbu.cscb869.services.data.dtos.MedicineUpdateDTO;
import nbu.cscb869.services.data.dtos.TreatmentCreateDTO;
import nbu.cscb869.services.data.dtos.TreatmentUpdateDTO;
import nbu.cscb869.services.data.dtos.TreatmentViewDTO;
import nbu.cscb869.services.services.contracts.TreatmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
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
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TreatmentServiceImplIntegrationTests.AsyncTestConfig.class)
class TreatmentServiceImplIntegrationTests {

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private TreatmentService treatmentService;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    private Visit testVisit;
    private Patient patientOwner;

    @BeforeEach
    void setUp() {
        treatmentRepository.deleteAll();
        visitRepository.deleteAll();
        diagnosisRepository.deleteAll();

        Doctor doctor = doctorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Doctor newDoctor = new Doctor();
            newDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
            newDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            newDoctor.setName("Dr. Test");
            newDoctor.setGeneralPractitioner(true);
            return doctorRepository.save(newDoctor);
        });

        patientOwner = patientRepository.findByKeycloakId("patient-owner-id").orElseGet(() -> {
            Patient newPatient = new Patient();
            newPatient.setKeycloakId("patient-owner-id");
            newPatient.setEgn(TestDataUtils.generateValidEgn());
            newPatient.setName("Patient Owner");
            newPatient.setGeneralPractitioner(doctor);
            return patientRepository.save(newPatient);
        });

        Diagnosis diagnosis = diagnosisRepository.findByName("Flu").orElseGet(() -> {
            Diagnosis newDiagnosis = new Diagnosis();
            newDiagnosis.setName("Flu");
            return diagnosisRepository.save(newDiagnosis);
        });

        testVisit = new Visit();
        testVisit.setDoctor(doctor);
        testVisit.setPatient(patientOwner);
        testVisit.setDiagnosis(diagnosis);
        testVisit.setVisitDate(LocalDate.now());
        testVisit.setVisitTime(LocalTime.now());
        visitRepository.save(testVisit);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void create_AsDoctorWithValidData_ShouldPersistAggregate_HappyPath() {
        // ARRANGE
        MedicineCreateDTO medDto = new MedicineCreateDTO("Aspirin", "500mg", "Once a day");
        TreatmentCreateDTO createDTO = new TreatmentCreateDTO("Take with food", testVisit.getId(), List.of(medDto));

        // ACT
        TreatmentViewDTO result = treatmentService.create(createDTO);

        // ASSERT
        assertNotNull(result);
        assertNotNull(result.getId());

        Treatment savedTreatment = treatmentRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedTreatment);
        assertEquals("Take with food", savedTreatment.getDescription());
        assertEquals(1, savedTreatment.getMedicines().size());
        assertEquals("Aspirin", savedTreatment.getMedicines().get(0).getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void update_AsDoctorWithValidData_ShouldUpdateAggregate_HappyPath() {
        // ARRANGE
        Treatment treatment = new Treatment();
        treatment.setVisit(testVisit);
        treatment.setDescription("Old description");
        treatment.getMedicines().add(new Medicine("Old Med", "100mg", "Old Freq", treatment));
        treatment = treatmentRepository.save(treatment);

        MedicineUpdateDTO medDto = new MedicineUpdateDTO(null, "New Med", "200mg", "New Freq");
        TreatmentUpdateDTO updateDTO = new TreatmentUpdateDTO(treatment.getId(), "New description", testVisit.getId(), List.of(medDto));

        // ACT
        treatmentService.update(updateDTO);

        // ASSERT
        Treatment updatedTreatment = treatmentRepository.findById(treatment.getId()).get();
        assertEquals("New description", updatedTreatment.getDescription());
        assertEquals(1, updatedTreatment.getMedicines().size());
        assertEquals("New Med", updatedTreatment.getMedicines().get(0).getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void delete_AsDoctor_ShouldDeleteAggregate_HappyPath() {
        // ARRANGE
        Treatment treatment = new Treatment();
        treatment.setVisit(testVisit);
        treatment = treatmentRepository.save(treatment);
        long treatmentId = treatment.getId();

        // ACT
        treatmentService.delete(treatmentId);

        // ASSERT
        assertFalse(treatmentRepository.findById(treatmentId).isPresent());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
    void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
        // ARRANGE
        Treatment treatment = new Treatment();
        treatment.setVisit(testVisit);
        treatment = treatmentRepository.save(treatment);

        // ACT
        TreatmentViewDTO result = treatmentService.getById(treatment.getId());

        // ASSERT
        assertNotNull(result);
        assertEquals(treatment.getId(), result.getId());
    }

    @Test
    @WithMockKeycloakUser(keycloakId = "other-patient-id", authorities = "ROLE_PATIENT")
    void getById_AsOtherPatient_ShouldBeDenied_ErrorCase() {
        // ARRANGE
        Treatment treatment = new Treatment();
        treatment.setVisit(testVisit);
        treatment = treatmentRepository.save(treatment);
        long treatmentId = treatment.getId();

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> treatmentService.getById(treatmentId));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getAll_AsAdmin_ShouldReturnPage_HappyPath() throws ExecutionException, InterruptedException {
        // ARRANGE
        Treatment treatment = new Treatment();
        treatment.setVisit(testVisit);
        treatmentRepository.save(treatment);

        // Commit transaction to make data visible to the async thread
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // ACT
        CompletableFuture<Page<TreatmentViewDTO>> future = treatmentService.getAll(0, 10, "description", true);
        Page<TreatmentViewDTO> result = future.get();

        // ASSERT
        assertEquals(1, result.getTotalElements());
    }
}