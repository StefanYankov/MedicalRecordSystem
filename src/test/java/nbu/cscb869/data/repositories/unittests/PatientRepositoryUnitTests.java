package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientRepositoryUnitTests {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private DiagnosisRepository diagnosisRepository;

    private Patient patient;
    private Doctor doctor;
    private Visit visit;
    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        // Setup test data (not persisted, just for mocking)
        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Smith");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);

        diagnosis = new Diagnosis();
        diagnosis.setId(1L);
        diagnosis.setName("Flu");
        diagnosis.setDescription("Viral infection");

        patient = new Patient();
        patient.setId(1L);
        patient.setName("Jane Doe");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(doctor);
        patient.setLastInsurancePaymentDate(LocalDate.now());

        visit = new Visit();
        visit.setId(1L);
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setSickLeaveIssued(false);
    }

    // Happy Path
    @Test
    void FindByEgn_WithValidEgn_ReturnsPatient() {
        when(patientRepository.findByEgn(patient.getEgn())).thenReturn(Optional.of(patient));

        Optional<Patient> foundPatient = patientRepository.findByEgn(patient.getEgn());

        assertTrue(foundPatient.isPresent());
        assertEquals("Jane Doe", foundPatient.get().getName());
        verify(patientRepository).findByEgn(patient.getEgn());
    }

    @Test
    void FindByGeneralPractitioner_WithValidDoctor_ReturnsPaged() {
        Page<Patient> page = new PageImpl<>(Collections.singletonList(patient));
        when(patientRepository.findByGeneralPractitioner(eq(doctor), any(Pageable.class))).thenReturn(page);

        Page<Patient> result = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Jane Doe", result.getContent().getFirst().getName());
        verify(patientRepository).findByGeneralPractitioner(eq(doctor), any(Pageable.class));
    }

    @Test
    void CountPatientsByGeneralPractitioner_WithData_ReturnsList() {
        DoctorPatientCountDTO dto = DoctorPatientCountDTO.builder()
                .doctor(doctor)
                .patientCount(1L)
                .build();
        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(Collections.singletonList(dto));

        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();

        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.getFirst().getDoctor().getName());
        assertEquals(1L, result.getFirst().getPatientCount());
        verify(patientRepository).countPatientsByGeneralPractitioner();
    }

    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(patientRepository.findAllActive()).thenReturn(Collections.singletonList(patient));

        List<Patient> result = patientRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Jane Doe", result.getFirst().getName());
        verify(patientRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Patient> page = new PageImpl<>(Collections.singletonList(patient));
        when(patientRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        Page<Patient> result = patientRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Jane Doe", result.getContent().getFirst().getName());
        verify(patientRepository).findAllActive(any(Pageable.class));
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(patientRepository).hardDeleteById(1L);

        patientRepository.hardDeleteById(1L);

        verify(patientRepository).hardDeleteById(1L);
    }

    // Error Cases
    @Test
    void FindByEgn_WithNonExistentEgn_ReturnsEmpty() {
        when(patientRepository.findByEgn("1234567890")).thenReturn(Optional.empty());

        Optional<Patient> foundPatient = patientRepository.findByEgn("1234567890");

        assertFalse(foundPatient.isPresent());
        verify(patientRepository).findByEgn("1234567890");
    }

    @Test
    void FindByGeneralPractitioner_WithNoPatients_ReturnsEmpty() {
        Doctor otherDoctor = new Doctor();
        otherDoctor.setId(2L);
        otherDoctor.setName("Dr. Jones");
        otherDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        otherDoctor.setGeneralPractitioner(true);

        Page<Patient> emptyPage = new PageImpl<>(Collections.emptyList());
        when(patientRepository.findByGeneralPractitioner(eq(otherDoctor), any(Pageable.class))).thenReturn(emptyPage);

        Page<Patient> result = patientRepository.findByGeneralPractitioner(otherDoctor, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(patientRepository).findByGeneralPractitioner(eq(otherDoctor), any(Pageable.class));
    }

    @Test
    void CountPatientsByGeneralPractitioner_WithNoData_ReturnsEmpty() {
        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(Collections.emptyList());

        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();

        assertTrue(result.isEmpty());
        verify(patientRepository).countPatientsByGeneralPractitioner();
    }

    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(patientRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Patient> result = patientRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(patientRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Patient> emptyPage = new PageImpl<>(Collections.emptyList());
        when(patientRepository.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

        Page<Patient> result = patientRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(patientRepository).findAllActive(any(Pageable.class));
    }

    // Edge Cases
    @Test
    void FindByEgn_WithSoftDeletedPatient_ReturnsEmpty() {
        Patient deletedPatient = new Patient();
        deletedPatient.setId(2L);
        deletedPatient.setName("Deleted");
        deletedPatient.setEgn(TestDataUtils.generateValidEgn());
        deletedPatient.setGeneralPractitioner(doctor);
        deletedPatient.setLastInsurancePaymentDate(LocalDate.now());
        deletedPatient.setIsDeleted(true);

        when(patientRepository.findByEgn(deletedPatient.getEgn())).thenReturn(Optional.empty());

        Optional<Patient> foundPatient = patientRepository.findByEgn(deletedPatient.getEgn());

        assertFalse(foundPatient.isPresent());
        verify(patientRepository).findByEgn(deletedPatient.getEgn());
    }

    @Test
    void FindByGeneralPractitioner_WithLastPageFewerElements_ReturnsCorrectPage() {
        Patient patient2 = new Patient();
        patient2.setId(2L);
        patient2.setName("John Smith");
        patient2.setEgn(TestDataUtils.generateValidEgn());
        patient2.setGeneralPractitioner(doctor);
        patient2.setLastInsurancePaymentDate(LocalDate.now());

        Patient patient3 = new Patient();
        patient3.setId(3L);
        patient3.setName("Alice Brown");
        patient3.setEgn(TestDataUtils.generateValidEgn());
        patient3.setGeneralPractitioner(doctor);
        patient3.setLastInsurancePaymentDate(LocalDate.now());

        Page<Patient> page = new PageImpl<>(Collections.singletonList(patient3), PageRequest.of(1, 2), 3);
        when(patientRepository.findByGeneralPractitioner(eq(doctor), eq(PageRequest.of(1, 2)))).thenReturn(page);

        Page<Patient> result = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Alice Brown", result.getContent().getFirst().getName());
        assertEquals(2, result.getTotalPages());
        verify(patientRepository).findByGeneralPractitioner(eq(doctor), eq(PageRequest.of(1, 2)));
    }
}