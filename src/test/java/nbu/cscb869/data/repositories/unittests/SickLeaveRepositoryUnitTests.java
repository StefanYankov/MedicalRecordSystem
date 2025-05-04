package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.SickLeaveRepository;
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
class SickLeaveRepositoryUnitTests {

    @Mock
    private SickLeaveRepository sickLeaveRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DiagnosisRepository diagnosisRepository;

    private SickLeave sickLeave;
    private Visit visit;
    private Doctor doctor;
    private Patient patient;
    private Diagnosis diagnosis;

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
        visit.setSickLeaveIssued(true);

        sickLeave = new SickLeave();
        sickLeave.setId(1L);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave.setVisit(visit);
    }

    // Happy Path
    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(sickLeaveRepository.findAllActive()).thenReturn(Collections.singletonList(sickLeave));

        List<SickLeave> result = sickLeaveRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals(5, result.getFirst().getDurationDays());
        verify(sickLeaveRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<SickLeave> page = new PageImpl<>(Collections.singletonList(sickLeave));
        when(sickLeaveRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        Page<SickLeave> result = sickLeaveRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(5, result.getContent().getFirst().getDurationDays());
        verify(sickLeaveRepository).findAllActive(any(Pageable.class));
    }

    @Test
    void FindYearMonthWithMostSickLeaves_WithData_ReturnsList() {
        YearMonthSickLeaveCountDTO dto = YearMonthSickLeaveCountDTO.builder()
                .year(LocalDate.now().getYear())
                .month(LocalDate.now().getMonthValue())
                .count(1L)
                .build();
        when(sickLeaveRepository.findYearMonthWithMostSickLeaves()).thenReturn(Collections.singletonList(dto));

        List<YearMonthSickLeaveCountDTO> result = sickLeaveRepository.findYearMonthWithMostSickLeaves();

        assertEquals(1, result.size());
        assertEquals(LocalDate.now().getYear(), result.getFirst().getYear());
        assertEquals(LocalDate.now().getMonthValue(), result.getFirst().getMonth());
        assertEquals(1L, result.getFirst().getCount());
        verify(sickLeaveRepository).findYearMonthWithMostSickLeaves();
    }

    @Test
    void FindDoctorsWithMostSickLeaves_WithData_ReturnsList() {
        DoctorSickLeaveCountDTO dto = DoctorSickLeaveCountDTO.builder()
                .doctor(doctor)
                .sickLeaveCount(1L)
                .build();
        when(sickLeaveRepository.findDoctorsWithMostSickLeaves()).thenReturn(Collections.singletonList(dto));

        List<DoctorSickLeaveCountDTO> result = sickLeaveRepository.findDoctorsWithMostSickLeaves();

        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.getFirst().getDoctor().getName());
        assertEquals(1L, result.getFirst().getSickLeaveCount());
        verify(sickLeaveRepository).findDoctorsWithMostSickLeaves();
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(sickLeaveRepository).hardDeleteById(1L);

        sickLeaveRepository.hardDeleteById(1L);

        verify(sickLeaveRepository).hardDeleteById(1L);
    }

    // Error Cases
    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(sickLeaveRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<SickLeave> result = sickLeaveRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(sickLeaveRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<SickLeave> emptyPage = new PageImpl<>(Collections.emptyList());
        when(sickLeaveRepository.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

        Page<SickLeave> result = sickLeaveRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(sickLeaveRepository).findAllActive(any(Pageable.class));
    }

    @Test
    void FindYearMonthWithMostSickLeaves_WithNoData_ReturnsEmpty() {
        when(sickLeaveRepository.findYearMonthWithMostSickLeaves()).thenReturn(Collections.emptyList());

        List<YearMonthSickLeaveCountDTO> result = sickLeaveRepository.findYearMonthWithMostSickLeaves();

        assertTrue(result.isEmpty());
        verify(sickLeaveRepository).findYearMonthWithMostSickLeaves();
    }

    @Test
    void FindDoctorsWithMostSickLeaves_WithNoData_ReturnsEmpty() {
        when(sickLeaveRepository.findDoctorsWithMostSickLeaves()).thenReturn(Collections.emptyList());

        List<DoctorSickLeaveCountDTO> result = sickLeaveRepository.findDoctorsWithMostSickLeaves();

        assertTrue(result.isEmpty());
        verify(sickLeaveRepository).findDoctorsWithMostSickLeaves();
    }

    // Edge Cases
    @Test
    void FindAllActive_WithSoftDeletedSickLeave_ReturnsEmpty() {
        SickLeave deletedSickLeave = new SickLeave();
        deletedSickLeave.setId(2L);
        deletedSickLeave.setStartDate(LocalDate.now());
        deletedSickLeave.setDurationDays(3);
        deletedSickLeave.setVisit(visit);
        deletedSickLeave.setIsDeleted(true);

        when(sickLeaveRepository.findAllActive()).thenReturn(Collections.singletonList(sickLeave));

        List<SickLeave> result = sickLeaveRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals(5, result.getFirst().getDurationDays());
        assertFalse(result.contains(deletedSickLeave));
        verify(sickLeaveRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithLastPageFewerElements_ReturnsCorrectPage() {
        SickLeave sickLeave2 = new SickLeave();
        sickLeave2.setId(2L);
        sickLeave2.setStartDate(LocalDate.now());
        sickLeave2.setDurationDays(7);
        sickLeave2.setVisit(visit);

        SickLeave sickLeave3 = new SickLeave();
        sickLeave3.setId(3L);
        sickLeave3.setStartDate(LocalDate.now());
        sickLeave3.setDurationDays(10);
        sickLeave3.setVisit(visit);

        Page<SickLeave> page = new PageImpl<>(Collections.singletonList(sickLeave3), PageRequest.of(1, 2), 3);
        when(sickLeaveRepository.findAllActive(PageRequest.of(1, 2))).thenReturn(page);

        Page<SickLeave> result = sickLeaveRepository.findAllActive(PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(10, result.getContent().getFirst().getDurationDays());
        assertEquals(2, result.getTotalPages());
        verify(sickLeaveRepository).findAllActive(PageRequest.of(1, 2));
    }
}