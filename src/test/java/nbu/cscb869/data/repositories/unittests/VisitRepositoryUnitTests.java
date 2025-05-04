package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitRepositoryUnitTests {

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DiagnosisRepository diagnosisRepository;

    private Visit visit;
    private Doctor doctor;
    private Patient patient;
    private Diagnosis diagnosis;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

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
        patient.setLastInsurancePaymentDate(today);

        visit = new Visit();
        visit.setId(1L);
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(today);
        visit.setSickLeaveIssued(false);
    }

    // Happy Path
    @Test
    void FindByPatient_WithValidPatient_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByPatient(eq(patient), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByPatient(patient, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByPatient(eq(patient), any(Pageable.class));
    }

    @Test
    void FindByDoctor_WithValidDoctor_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByDoctor(eq(doctor), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctor(doctor, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByDoctor(eq(doctor), any(Pageable.class));
    }

    @Test
    void FindByDateRange_WithValidRange_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByDateRange(eq(today), eq(today), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDateRange(today, today, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByDateRange(eq(today), eq(today), any(Pageable.class));
    }

    @Test
    void FindByDoctorAndDateRange_WithValidDoctorAndRange_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, today, today, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), any(Pageable.class));
    }

    @Test
    void FindByDiagnosis_WithValidDiagnosis_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByDiagnosis(eq(diagnosis), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByDiagnosis(eq(diagnosis), any(Pageable.class));
    }

    @Test
    void FindMostFrequentDiagnoses_WithData_ReturnsList() {
        DiagnosisVisitCountDTO dto = DiagnosisVisitCountDTO.builder()
                .diagnosis(diagnosis)
                .visitCount(1L)
                .build();
        when(visitRepository.findMostFrequentDiagnoses()).thenReturn(Collections.singletonList(dto));

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertEquals(1, result.size());
        assertEquals("Flu", result.getFirst().getDiagnosis().getName());
        assertEquals(1L, result.getFirst().getVisitCount());
        verify(visitRepository).findMostFrequentDiagnoses();
    }

    @Test
    void CountVisitsByDoctor_WithData_ReturnsList() {
        DoctorVisitCountDTO dto = DoctorVisitCountDTO.builder()
                .doctor(doctor)
                .visitCount(1L)
                .build();
        when(visitRepository.countVisitsByDoctor()).thenReturn(Collections.singletonList(dto));

        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.getFirst().getDoctor().getName());
        assertEquals(1L, result.getFirst().getVisitCount());
        verify(visitRepository).countVisitsByDoctor();
    }

    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(visitRepository.findAllActive()).thenReturn(Collections.singletonList(visit));

        List<Visit> result = visitRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals(today, result.getFirst().getVisitDate());
        verify(visitRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findAllActive(any(Pageable.class));
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(visitRepository).hardDeleteById(1L);

        visitRepository.hardDeleteById(1L);

        verify(visitRepository).hardDeleteById(1L);
    }

    // Error Cases
    @Test
    void FindByPatient_WithNoVisits_ReturnsEmpty() {
        Patient otherPatient = new Patient();
        otherPatient.setId(2L);
        otherPatient.setName("John Smith");
        otherPatient.setEgn(TestDataUtils.generateValidEgn());
        otherPatient.setGeneralPractitioner(doctor);
        otherPatient.setLastInsurancePaymentDate(today);

        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByPatient(eq(otherPatient), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByPatient(otherPatient, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByPatient(eq(otherPatient), any(Pageable.class));
    }

    @Test
    void FindByDoctor_WithNoVisits_ReturnsEmpty() {
        Doctor otherDoctor = new Doctor();
        otherDoctor.setId(2L);
        otherDoctor.setName("Dr. Jones");
        otherDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        otherDoctor.setGeneralPractitioner(true);

        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDoctor(eq(otherDoctor), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctor(otherDoctor, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDoctor(eq(otherDoctor), any(Pageable.class));
    }

    @Test
    void FindByDateRange_WithNoVisits_ReturnsEmpty() {
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDateRange(eq(today.minusDays(1)), eq(today.minusDays(1)), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDateRange(today.minusDays(1), today.minusDays(1), PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDateRange(eq(today.minusDays(1)), eq(today.minusDays(1)), any(Pageable.class));
    }

    @Test
    void FindByDoctorAndDateRange_WithNoVisits_ReturnsEmpty() {
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(today.minusDays(1)), eq(today.minusDays(1)), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, today.minusDays(1), today.minusDays(1), PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(today.minusDays(1)), eq(today.minusDays(1)), any(Pageable.class));
    }

    @Test
    void FindByDiagnosis_WithNoVisits_ReturnsEmpty() {
        Diagnosis otherDiagnosis = new Diagnosis();
        otherDiagnosis.setId(2L);
        otherDiagnosis.setName("Cold");
        otherDiagnosis.setDescription("Common cold");

        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDiagnosis(eq(otherDiagnosis), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDiagnosis(otherDiagnosis, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDiagnosis(eq(otherDiagnosis), any(Pageable.class));
    }

    @Test
    void FindMostFrequentDiagnoses_WithNoData_ReturnsEmpty() {
        when(visitRepository.findMostFrequentDiagnoses()).thenReturn(Collections.emptyList());

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertTrue(result.isEmpty());
        verify(visitRepository).findMostFrequentDiagnoses();
    }

    @Test
    void CountVisitsByDoctor_WithNoData_ReturnsEmpty() {
        when(visitRepository.countVisitsByDoctor()).thenReturn(Collections.emptyList());

        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertTrue(result.isEmpty());
        verify(visitRepository).countVisitsByDoctor();
    }

    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(visitRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Visit> result = visitRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(visitRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findAllActive(any(Pageable.class));
    }

    // Edge Cases
    @Test
    void FindByPatient_WithSoftDeletedVisit_ReturnsEmpty() {
        Visit deletedVisit = new Visit();
        deletedVisit.setId(2L);
        deletedVisit.setPatient(patient);
        deletedVisit.setDoctor(doctor);
        deletedVisit.setDiagnosis(diagnosis);
        deletedVisit.setVisitDate(today);
        deletedVisit.setSickLeaveIssued(false);
        deletedVisit.setIsDeleted(true);

        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByPatient(eq(patient), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByPatient(patient, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertFalse(result.getContent().contains(deletedVisit));
        verify(visitRepository).findByPatient(eq(patient), any(Pageable.class));
    }

    @Test
    void FindByDoctorAndDateRange_WithLastPageFewerElements_ReturnsCorrectPage() {
        Visit visit2 = new Visit();
        visit2.setId(2L);
        visit2.setPatient(patient);
        visit2.setDoctor(doctor);
        visit2.setDiagnosis(diagnosis);
        visit2.setVisitDate(today);
        visit2.setSickLeaveIssued(false);

        Visit visit3 = new Visit();
        visit3.setId(3L);
        visit3.setPatient(patient);
        visit3.setDoctor(doctor);
        visit3.setDiagnosis(diagnosis);
        visit3.setVisitDate(today);
        visit3.setSickLeaveIssued(false);

        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit3), PageRequest.of(1, 2), 3);
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), eq(PageRequest.of(1, 2)))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, today, today, PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(2, result.getTotalPages());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), eq(PageRequest.of(1, 2)));
    }
}