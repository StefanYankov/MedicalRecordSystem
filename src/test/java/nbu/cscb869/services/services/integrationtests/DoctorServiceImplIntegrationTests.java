package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDoctorException;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.*;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(DoctorServiceImplIntegrationTests.AsyncTestConfig.class)
class DoctorServiceImplIntegrationTests {

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private SickLeaveRepository sickLeaveRepository;

    private Specialty specialtyCardiology;
    private Specialty specialtySurgery;

    @BeforeEach
    void setUp() {
        sickLeaveRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();
        diagnosisRepository.deleteAll();

        specialtyCardiology = specialtyRepository.findByName("Cardiology")
                .orElseGet(() -> specialtyRepository.save(new Specialty("Cardiology", "Heart related issues", Collections.emptySet())));

        specialtySurgery = specialtyRepository.findByName("Surgery")
                .orElseGet(() -> specialtyRepository.save(new Specialty("Surgery", "Surgical operations", Collections.emptySet())));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Create Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void create_AsAdminWithValidData_ShouldSucceed_HappyPath() {
        DoctorCreateDTO createDTO = new DoctorCreateDTO();
        createDTO.setName("Dr. Gregory House");
        createDTO.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        createDTO.setSpecialties(Set.of("Cardiology"));
        createDTO.setKeycloakId(TestDataUtils.generateKeycloakId());

        DoctorViewDTO result = doctorService.create(createDTO, null);

        assertNotNull(result);
        assertNotNull(result.getId());
        Doctor savedDoctor = doctorRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedDoctor);
        assertEquals("Cardiology", savedDoctor.getSpecialties().iterator().next().getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void create_AsDoctor_ShouldBeDenied_ErrorCase() {
        DoctorCreateDTO createDTO = new DoctorCreateDTO();
        createDTO.setName("Dr. Unauthorized");
        createDTO.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        createDTO.setKeycloakId(TestDataUtils.generateKeycloakId());

        assertThrows(AccessDeniedException.class, () -> doctorService.create(createDTO, null));
    }

    // --- Update Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void update_AsAdminWithValidData_ShouldSucceed_HappyPath() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Old Name");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctor.getSpecialties().add(specialtyCardiology);
        doctor = doctorRepository.save(doctor);

        DoctorUpdateDTO updateDTO = new DoctorUpdateDTO();
        updateDTO.setId(doctor.getId());
        updateDTO.setName("Dr. New Name");
        updateDTO.setUniqueIdNumber(doctor.getUniqueIdNumber());
        updateDTO.setSpecialties(Set.of("Surgery"));

        DoctorViewDTO result = doctorService.update(updateDTO, null);

        assertNotNull(result);
        Doctor updatedDoctor = doctorRepository.findById(doctor.getId()).get();
        assertEquals("Dr. New Name", updatedDoctor.getName());
        assertEquals("Surgery", updatedDoctor.getSpecialties().iterator().next().getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void update_AsPatient_ShouldBeDenied_ErrorCase() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. To Update");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctor = doctorRepository.save(doctor);

        DoctorUpdateDTO updateDTO = new DoctorUpdateDTO();
        updateDTO.setId(doctor.getId());
        updateDTO.setName("Dr. Should Fail");
        updateDTO.setUniqueIdNumber(doctor.getUniqueIdNumber());

        assertThrows(AccessDeniedException.class, () -> doctorService.update(updateDTO, null));
    }

    // --- Delete Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void delete_AsAdmin_ShouldSucceed_HappyPath() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. To Delete");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctor.setGeneralPractitioner(false);
        doctor = doctorRepository.save(doctor);
        long doctorId = doctor.getId();

        doctorService.delete(doctorId);

        assertFalse(doctorRepository.findById(doctorId).isPresent());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void delete_AsAdminWhenDoctorIsGpWithPatients_ShouldThrowException_ErrorCase() {
        Doctor gp = new Doctor();
        gp.setName("Dr. GP");
        gp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        gp.setKeycloakId(TestDataUtils.generateKeycloakId());
        gp.setGeneralPractitioner(true);
        gp = doctorRepository.save(gp);

        Patient patient = new Patient();
        patient.setName("Test Patient");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);

        long gpId = gp.getId();

        assertThrows(InvalidDoctorException.class, () -> doctorService.delete(gpId));
    }

    // --- Get & Find Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void getById_AsPatient_ShouldSucceed_HappyPath() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Visible");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctor = doctorRepository.save(doctor);

        DoctorViewDTO result = doctorService.getById(doctor.getId());

        assertNotNull(result);
        assertEquals(doctor.getName(), result.getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getById_WithNonExistentId_ShouldThrowException_ErrorCase() {
        assertThrows(EntityNotFoundException.class, () -> doctorService.getById(999L));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
    void getByUniqueIdNumber_AsPatient_ShouldSucceed_HappyPath() {
        String uniqueId = TestDataUtils.generateUniqueIdNumber();
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Findable");
        doctor.setUniqueIdNumber(uniqueId);
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctorRepository.save(doctor);

        DoctorViewDTO result = doctorService.getByUniqueIdNumber(uniqueId);

        assertNotNull(result);
        assertEquals("Dr. Findable", result.getName());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getByUniqueIdNumber_WithNonExistentId_ShouldThrowException_ErrorCase() {
        assertThrows(EntityNotFoundException.class, () -> doctorService.getByUniqueIdNumber("non-existent-id"));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void getPatientsByGeneralPractitioner_AsDoctor_ShouldReturnCorrectPatients_HappyPath() {
        Doctor gp = new Doctor();
        gp.setName("Dr. GP");
        gp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        gp.setKeycloakId(TestDataUtils.generateKeycloakId());
        gp.setGeneralPractitioner(true);
        doctorRepository.save(gp);

        Patient patient = new Patient();
        patient.setName("Test Patient");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);

        Page<PatientViewDTO> result = doctorService.getPatientsByGeneralPractitioner(gp.getId(), 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Patient", result.getContent().get(0).getName());
    }

    // --- Async GetAll Test ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void getAllAsync_AsDoctorWithFilter_ShouldReturnFilteredPage_HappyPath() throws ExecutionException, InterruptedException {
        String uniqueIdToFind = TestDataUtils.generateUniqueIdNumber();

        Doctor doctor1 = new Doctor();
        doctor1.setName("Dr. One");
        doctor1.setUniqueIdNumber(uniqueIdToFind);
        doctor1.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctorRepository.save(doctor1);

        Doctor doctor2 = new Doctor();
        doctor2.setName("Dr. Two");
        doctor2.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor2.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctorRepository.save(doctor2);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        CompletableFuture<Page<DoctorViewDTO>> resultFuture = doctorService.getAllAsync(0, 10, "name", true, uniqueIdToFind);
        Page<DoctorViewDTO> result = resultFuture.get();

        assertEquals(1, result.getTotalElements());
        assertEquals("Dr. One", result.getContent().get(0).getName());
    }

    // --- Reporting Tests ---

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getPatientCountByGeneralPractitioner_AsAdmin_ShouldReturnCorrectCounts_HappyPath() {
        Doctor gp1 = new Doctor();
        gp1.setName("Dr. GP One");
        gp1.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        gp1.setKeycloakId(TestDataUtils.generateKeycloakId());
        gp1.setGeneralPractitioner(true);
        doctorRepository.save(gp1);

        Patient patient1 = new Patient();
        patient1.setName("Patient One");
        patient1.setEgn(TestDataUtils.generateValidEgn());
        patient1.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient1.setGeneralPractitioner(gp1);
        patientRepository.save(patient1);

        var result = doctorService.getPatientCountByGeneralPractitioner();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Dr. GP One", result.get(0).getDoctor().getName());
        assertEquals(1, result.get(0).getPatientCount());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void getPatientCountByGeneralPractitioner_AsDoctor_ShouldBeDenied_ErrorCase() {
        assertThrows(AccessDeniedException.class, () -> doctorService.getPatientCountByGeneralPractitioner());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getVisitCount_AsAdmin_ShouldReturnCorrectCounts_HappyPath() {
        Doctor gp = new Doctor();
        gp.setName("Dr. Visited GP");
        gp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        gp.setKeycloakId(TestDataUtils.generateKeycloakId());
        gp.setGeneralPractitioner(true);
        doctorRepository.save(gp);

        Patient patient = new Patient();
        patient.setName("Test Patient");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        diagnosis.setDescription("Test Diagnosis");
        diagnosisRepository.save(diagnosis);

        Visit visit = new Visit();
        visit.setDoctor(gp);
        visit.setPatient(patient);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(LocalTime.now());
        visitRepository.save(visit);

        var result = doctorService.getVisitCount();

        assertEquals(1, result.size());
        assertEquals("Dr. Visited GP", result.get(0).getDoctor().getName());
        assertEquals(1, result.get(0).getVisitCount());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void getVisitsByPeriod_AsDoctor_ShouldReturnCorrectVisits_HappyPath() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Visited");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctorRepository.save(doctor);

        createTestVisit(doctor);

        var result = doctorService.getVisitsByPeriod(doctor.getId(), LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 0, 10);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void getDoctorsWithMostSickLeaves_AsAdmin_ShouldReturnCorrectDoctors_HappyPath() {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Sick Leave Issuer");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctorRepository.save(doctor);

        Visit visit = createTestVisit(doctor);
        SickLeave sickLeave = new SickLeave();
        sickLeave.setVisit(visit);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeaveRepository.save(sickLeave);

        var result = doctorService.getDoctorsWithMostSickLeaves();

        assertEquals(1, result.size());
        assertEquals("Dr. Sick Leave Issuer", result.get(0).getDoctor().getName());
        assertEquals(1, result.get(0).getSickLeaveCount());
    }

    private Visit createTestVisit(Doctor doctor) {
        Doctor gp = new Doctor();
        gp.setName("Dr. Helper GP");
        gp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        gp.setKeycloakId(TestDataUtils.generateKeycloakId());
        gp.setGeneralPractitioner(true);
        doctorRepository.save(gp);

        Patient patient = new Patient();
        patient.setName("Test Patient");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setKeycloakId(TestDataUtils.generateKeycloakId());
        patient.setGeneralPractitioner(gp);
        patientRepository.save(patient);

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        diagnosis.setDescription("Test Diagnosis");
        diagnosisRepository.save(diagnosis);

        Visit visit = new Visit();
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(LocalTime.now());
        return visitRepository.save(visit);
    }
}
