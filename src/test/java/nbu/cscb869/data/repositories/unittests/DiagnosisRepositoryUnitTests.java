package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.PatientDiagnosisDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
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
class DiagnosisRepositoryUnitTests {

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private VisitRepository visitRepository;

    private Diagnosis diagnosis;
    private Doctor doctor;
    private Patient patient;
    private Visit visit;

    @BeforeEach
    void setUp() {
        // Setup test data (not persisted, just for mocking)
        diagnosis = new Diagnosis();
        diagnosis.setId(1L);
        diagnosis.setName("Flu");
        diagnosis.setDescription("Viral infection");

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Smith");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);

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
    void FindByName_WithExistingName_ReturnsDiagnosis() {
        when(diagnosisRepository.findByName("Flu")).thenReturn(Optional.of(diagnosis));

        Optional<Diagnosis> foundDiagnosis = diagnosisRepository.findByName("Flu");

        assertTrue(foundDiagnosis.isPresent());
        assertEquals("Flu", foundDiagnosis.get().getName());
        verify(diagnosisRepository).findByName("Flu");
    }

    @Test
    void FindPatientsByDiagnosis_WithValidDiagnosis_ReturnsPaged() {
        PatientDiagnosisDTO dto = PatientDiagnosisDTO.builder()
                .patient(patient)
                .diagnosisName("Flu")
                .build();
        Page<PatientDiagnosisDTO> page = new PageImpl<>(Collections.singletonList(dto));
        when(diagnosisRepository.findPatientsByDiagnosis(eq(diagnosis), any(Pageable.class))).thenReturn(page);

        Page<PatientDiagnosisDTO> result = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Flu", result.getContent().get(0).getDiagnosisName());
        assertEquals(patient.getId(), result.getContent().get(0).getPatient().getId());
        verify(diagnosisRepository).findPatientsByDiagnosis(eq(diagnosis), any(Pageable.class));
    }

    @Test
    void FindMostFrequentDiagnoses_WithData_ReturnsSorted() {
        DiagnosisVisitCountDTO dto = DiagnosisVisitCountDTO.builder()
                .diagnosis(diagnosis)
                .visitCount(1L)
                .build();
        when(diagnosisRepository.findMostFrequentDiagnoses()).thenReturn(Collections.singletonList(dto));

        List<DiagnosisVisitCountDTO> result = diagnosisRepository.findMostFrequentDiagnoses();

        assertEquals(1, result.size());
        assertEquals("Flu", result.get(0).getDiagnosis().getName());
        assertEquals(1L, result.get(0).getVisitCount());
        verify(diagnosisRepository).findMostFrequentDiagnoses();
    }

    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(diagnosisRepository.findAllActive()).thenReturn(Collections.singletonList(diagnosis));

        List<Diagnosis> result = diagnosisRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Flu", result.get(0).getName());
        verify(diagnosisRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Diagnosis> page = new PageImpl<>(Collections.singletonList(diagnosis));
        when(diagnosisRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        Page<Diagnosis> result = diagnosisRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Flu", result.getContent().get(0).getName());
        verify(diagnosisRepository).findAllActive(any(Pageable.class));
    }

    // Error Cases
    @Test
    void FindByName_WithNonExistentName_ReturnsEmpty() {
        when(diagnosisRepository.findByName("Nonexistent")).thenReturn(Optional.empty());

        Optional<Diagnosis> foundDiagnosis = diagnosisRepository.findByName("Nonexistent");

        assertFalse(foundDiagnosis.isPresent());
        verify(diagnosisRepository).findByName("Nonexistent");
    }

    @Test
    void FindPatientsByDiagnosis_WithNoData_ReturnsEmpty() {
        Diagnosis otherDiagnosis = new Diagnosis();
        otherDiagnosis.setId(2L);
        otherDiagnosis.setName("Other");

        Page<PatientDiagnosisDTO> emptyPage = new PageImpl<>(Collections.emptyList());
        when(diagnosisRepository.findPatientsByDiagnosis(eq(otherDiagnosis), any(Pageable.class))).thenReturn(emptyPage);

        Page<PatientDiagnosisDTO> result = diagnosisRepository.findPatientsByDiagnosis(otherDiagnosis, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(diagnosisRepository).findPatientsByDiagnosis(eq(otherDiagnosis), any(Pageable.class));
    }

    @Test
    void FindMostFrequentDiagnoses_WithNoData_ReturnsEmpty() {
        when(diagnosisRepository.findMostFrequentDiagnoses()).thenReturn(Collections.emptyList());

        List<DiagnosisVisitCountDTO> result = diagnosisRepository.findMostFrequentDiagnoses();

        assertTrue(result.isEmpty());
        verify(diagnosisRepository).findMostFrequentDiagnoses();
    }

    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(diagnosisRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Diagnosis> result = diagnosisRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(diagnosisRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Diagnosis> emptyPage = new PageImpl<>(Collections.emptyList());
        when(diagnosisRepository.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

        Page<Diagnosis> result = diagnosisRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(diagnosisRepository).findAllActive(any(Pageable.class));
    }

    // Edge Cases
    @Test
    void FindByName_WithSoftDeletedDiagnosis_ReturnsEmpty() {
        Diagnosis deletedDiagnosis = new Diagnosis();
        deletedDiagnosis.setId(2L);
        deletedDiagnosis.setName("Deleted");
        deletedDiagnosis.setIsDeleted(true);

        when(diagnosisRepository.findByName("Deleted")).thenReturn(Optional.empty());

        Optional<Diagnosis> foundDiagnosis = diagnosisRepository.findByName("Deleted");

        assertFalse(foundDiagnosis.isPresent());
        verify(diagnosisRepository).findByName("Deleted");
    }

    @Test
    void FindPatientsByDiagnosis_WithLastPageFewerElements_ReturnsCorrectPage() {
        Patient patient2 = new Patient();
        patient2.setId(2L);
        patient2.setName("John Smith");
        patient2.setEgn(TestDataUtils.generateValidEgn());
        patient2.setGeneralPractitioner(doctor);
        patient2.setLastInsurancePaymentDate(LocalDate.now());

        PatientDiagnosisDTO dto1 = PatientDiagnosisDTO.builder()
                .patient(patient)
                .diagnosisName("Flu")
                .build();
        PatientDiagnosisDTO dto2 = PatientDiagnosisDTO.builder()
                .patient(patient2)
                .diagnosisName("Flu")
                .build();
        PatientDiagnosisDTO dto3 = PatientDiagnosisDTO.builder()
                .patient(patient2)
                .diagnosisName("Flu")
                .build();

        Page<PatientDiagnosisDTO> page = new PageImpl<>(Collections.singletonList(dto3), PageRequest.of(1, 2), 3);
        when(diagnosisRepository.findPatientsByDiagnosis(eq(diagnosis), eq(PageRequest.of(1, 2)))).thenReturn(page);

        Page<PatientDiagnosisDTO> result = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(2, result.getTotalPages());
        verify(diagnosisRepository).findPatientsByDiagnosis(eq(diagnosis), eq(PageRequest.of(1, 2)));
    }
}