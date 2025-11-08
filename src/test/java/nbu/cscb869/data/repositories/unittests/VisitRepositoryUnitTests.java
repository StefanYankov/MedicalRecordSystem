package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitRepositoryUnitTests {

    @Mock
    private VisitRepository visitRepository;

    private Diagnosis createDiagnosis(String name, String description) {
        return Diagnosis.builder()
                .name(name)
                .description(description)
                .build();
    }

    private Doctor createDoctor(String uniqueIdNumber, boolean isGeneralPractitioner, String name) {
        return Doctor.builder()
                .uniqueIdNumber(uniqueIdNumber)
                .isGeneralPractitioner(isGeneralPractitioner)
                .name(name)
                .build();
    }

    private Patient createPatient(String egn, Doctor generalPractitioner, LocalDate lastInsurancePaymentDate) {
        return Patient.builder()
                .egn(egn)
                .generalPractitioner(generalPractitioner)
                .lastInsurancePaymentDate(lastInsurancePaymentDate)
                .build();
    }

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate, LocalTime visitTime, SickLeave sickLeave) {
        Visit visit = Visit.builder()
                .patient(patient)
                .doctor(doctor)
                .diagnosis(diagnosis)
                .visitDate(visitDate)
                .visitTime(visitTime)
                .build();
        if (sickLeave != null) {
            visit.setSickLeave(sickLeave);
            sickLeave.setVisit(visit);
        }
        return visit;
    }

    @Test
    void FindByPatientOrderByVisitDateDescVisitTimeDesc_WhenCalled_ShouldReturnPagedResults() {
        // Arrange
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), null, LocalDate.now());
        Visit visit1 = createVisit(patient, null, null, LocalDate.now().minusDays(1), LocalTime.of(10, 0), null);
        Visit visit2 = createVisit(patient, null, null, LocalDate.now(), LocalTime.of(9, 0), null);
        Page<Visit> page = new PageImpl<>(List.of(visit2, visit1));
        when(visitRepository.findByPatientOrderByVisitDateDescVisitTimeDesc(eq(patient), any(Pageable.class))).thenReturn(page);

        // Act
        Page<Visit> result = visitRepository.findByPatientOrderByVisitDateDescVisitTimeDesc(patient, PageRequest.of(0, 5));

        // Assert
        assertEquals(2, result.getTotalElements());
        verify(visitRepository).findByPatientOrderByVisitDateDescVisitTimeDesc(eq(patient), any(Pageable.class));
    }

    @Test
    void FindByDoctorAndStatusAndVisitDateBetween_WhenCalled_ShouldReturnFilteredResults() {
        // Arrange
        Doctor doctor = createDoctor("DOC123", false, "Dr. Who");
        Visit visit = createVisit(null, doctor, null, LocalDate.now().plusDays(1), LocalTime.of(14, 0), null);
        visit.setStatus(VisitStatus.SCHEDULED);
        Page<Visit> page = new PageImpl<>(List.of(visit));
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(5);
        when(visitRepository.findByDoctorAndStatusAndVisitDateBetweenOrderByVisitDateAscVisitTimeAsc(eq(doctor), eq(VisitStatus.SCHEDULED), eq(startDate), eq(endDate), any(Pageable.class))).thenReturn(page);

        // Act
        Page<Visit> result = visitRepository.findByDoctorAndStatusAndVisitDateBetweenOrderByVisitDateAscVisitTimeAsc(doctor, VisitStatus.SCHEDULED, startDate, endDate, PageRequest.of(0, 5));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(VisitStatus.SCHEDULED, result.getContent().get(0).getStatus());
        verify(visitRepository).findByDoctorAndStatusAndVisitDateBetweenOrderByVisitDateAscVisitTimeAsc(eq(doctor), eq(VisitStatus.SCHEDULED), eq(startDate), eq(endDate), any(Pageable.class));
    }

    @Test
    void findByPatient_ValidPatient_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Page<Visit> page = new PageImpl<>(List.of(visit));
        when(visitRepository.findByPatient(eq(patient), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByPatient(patient, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(LocalDate.now(), result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByPatient(eq(patient), any(Pageable.class));
    }

    @Test
    void findByDoctor_ValidDoctor_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Page<Visit> page = new PageImpl<>(List.of(visit));
        when(visitRepository.findByDoctor(eq(doctor), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctor(doctor, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(LocalDate.now(), result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByDoctor(eq(doctor), any(Pageable.class));
    }

    @Test
    void findByDateRange_ValidRange_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Page<Visit> page = new PageImpl<>(List.of(visit));
        LocalDate today = LocalDate.now();
        when(visitRepository.findByDateRange(eq(today), eq(today), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDateRange(today, today, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByDateRange(eq(today), eq(today), any(Pageable.class));
    }

    @Test
    void findByDoctorAndDateRange_ValidDoctorAndRange_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Page<Visit> page = new PageImpl<>(List.of(visit));
        LocalDate today = LocalDate.now();
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, today, today, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), any(Pageable.class));
    }

    @Test
    void findByDiagnosis_ValidDiagnosis_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Charlie Green");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Page<Visit> page = new PageImpl<>(List.of(visit));
        when(visitRepository.findByDiagnosis(eq(diagnosis), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(LocalDate.now(), result.getContent().getFirst().getVisitDate());
        verify(visitRepository).findByDiagnosis(eq(diagnosis), any(Pageable.class));
    }

    @Test
    void findMostFrequentDiagnoses_WithData_ReturnsList_HappyPath() {
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        DiagnosisVisitCountDTO dto = DiagnosisVisitCountDTO.builder()
                .diagnosis(diagnosis)
                .visitCount(1L)
                .build();
        when(visitRepository.findMostFrequentDiagnoses()).thenReturn(List.of(dto));

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertEquals(1, result.size());
        assertEquals("Flu", result.getFirst().getDiagnosis().getName());
        verify(visitRepository).findMostFrequentDiagnoses();
    }

    @Test
    void countVisitsByDoctor_WithData_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. David Black");
        DoctorVisitCountDTO dto = DoctorVisitCountDTO.builder()
                .doctor(doctor)
                .visitCount(1L)
                .build();
        when(visitRepository.countVisitsByDoctor()).thenReturn(List.of(dto));

        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertEquals(1, result.size());
        assertEquals(doctor.getUniqueIdNumber(), result.getFirst().getDoctor().getUniqueIdNumber());
        verify(visitRepository).countVisitsByDoctor();
    }

    @Test
    void findByPatientOrDoctorFilter_ValidFilter_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor("DOC123", true, "Dr. Eve White");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Page<Visit> page = new PageImpl<>(List.of(visit));
        when(visitRepository.findByPatientOrDoctorFilter(eq("%DOC123%"), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter("%DOC123%", PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("DOC123", result.getContent().getFirst().getDoctor().getUniqueIdNumber());
        verify(visitRepository).findByPatientOrDoctorFilter(eq("%DOC123%"), any(Pageable.class));
    }

    @Test
    void findByDoctorAndDateTime_ValidParams_ReturnsVisit_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Frank Gray");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        when(visitRepository.findByDoctorAndDateTime(eq(doctor), eq(LocalDate.now()), eq(LocalTime.of(10, 30)))).thenReturn(Optional.of(visit));

        Optional<Visit> result = visitRepository.findByDoctorAndDateTime(doctor, LocalDate.now(), LocalTime.of(10, 30));

        assertTrue(result.isPresent());
        assertEquals(LocalDate.now(), result.get().getVisitDate());
        verify(visitRepository).findByDoctorAndDateTime(eq(doctor), eq(LocalDate.now()), eq(LocalTime.of(10, 30)));
    }

    @Test
    void findByPatient_NoVisits_ReturnsEmptyPage_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Grace Blue");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Page<Visit> emptyPage = new PageImpl<>(List.of());
        when(visitRepository.findByPatient(eq(patient), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByPatient(patient, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByPatient(eq(patient), any(Pageable.class));
    }

    @Test
    void findByDoctor_NoVisits_ReturnsEmptyPage_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Henry Red");
        Page<Visit> emptyPage = new PageImpl<>(List.of());
        when(visitRepository.findByDoctor(eq(doctor), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctor(doctor, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDoctor(eq(doctor), any(Pageable.class));
    }

    @Test
    void findByDateRange_NoVisits_ReturnsEmptyPage_ErrorCase() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Page<Visit> emptyPage = new PageImpl<>(List.of());
        when(visitRepository.findByDateRange(eq(yesterday), eq(yesterday), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDateRange(yesterday, yesterday, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDateRange(eq(yesterday), eq(yesterday), any(Pageable.class));
    }

    @Test
    void findByDateRange_InvalidRange_ReturnsEmptyPage_ErrorCase() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        Page<Visit> emptyPage = new PageImpl<>(List.of());
        when(visitRepository.findByDateRange(eq(today), eq(yesterday), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDateRange(today, yesterday, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDateRange(eq(today), eq(yesterday), any(Pageable.class));
    }

    @Test
    void findByDoctorAndDateRange_NoVisits_ReturnsEmptyPage_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Ivy Purple");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Page<Visit> emptyPage = new PageImpl<>(List.of());
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(yesterday), eq(yesterday), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, yesterday, yesterday, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(yesterday), eq(yesterday), any(Pageable.class));
    }

    @Test
    void findByDoctorAndDateRange_InvalidRange_ReturnsEmptyPage_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        Page<Visit> emptyPage = new PageImpl<>(List.of());
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(today), eq(yesterday), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, today, yesterday, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(today), eq(yesterday), any(Pageable.class));
    }

    @Test
    void findByDiagnosis_NoVisits_ReturnsEmptyPage_ErrorCase() {
        Diagnosis diagnosis = createDiagnosis("Cold", "Common cold");
        Page<Visit> emptyPage = new PageImpl<>(List.of());
        when(visitRepository.findByDiagnosis(eq(diagnosis), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDiagnosis(eq(diagnosis), any(Pageable.class));
    }

    @Test
    void findMostFrequentDiagnoses_NoData_ReturnsEmptyList_ErrorCase() {
        when(visitRepository.findMostFrequentDiagnoses()).thenReturn(List.of());

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertTrue(result.isEmpty());
        verify(visitRepository).findMostFrequentDiagnoses();
    }

    @Test
    void countVisitsByDoctor_NoData_ReturnsEmptyList_ErrorCase() {
        when(visitRepository.countVisitsByDoctor()).thenReturn(List.of());

        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertTrue(result.isEmpty());
        verify(visitRepository).countVisitsByDoctor();
    }

    @Test
    void findByPatientOrDoctorFilter_NoMatches_ReturnsEmptyPage_ErrorCase() {
        Page<Visit> emptyPage = new PageImpl<>(List.of());
        when(visitRepository.findByPatientOrDoctorFilter(eq("%NONEXISTENT%"), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter("%NONEXISTENT%", PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByPatientOrDoctorFilter(eq("%NONEXISTENT%"), any(Pageable.class));
    }

    @Test
    void findByDoctorAndDateTime_NoMatch_ReturnsEmpty_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        when(visitRepository.findByDoctorAndDateTime(eq(doctor), eq(LocalDate.now()), eq(LocalTime.of(11, 0)))).thenReturn(Optional.empty());

        Optional<Visit> result = visitRepository.findByDoctorAndDateTime(doctor, LocalDate.now(), LocalTime.of(11, 0));

        assertFalse(result.isPresent());
        verify(visitRepository).findByDoctorAndDateTime(eq(doctor), eq(LocalDate.now()), eq(LocalTime.of(11, 0)));
    }

    @Test
    void findByDoctorAndDateTime_MultipleVisitsSameTime_ReturnsFirst_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Patient patient1 = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Patient patient2 = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit1 = createVisit(patient1, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Visit visit2 = createVisit(patient2, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        when(visitRepository.findByDoctorAndDateTime(eq(doctor), eq(LocalDate.now()), eq(LocalTime.of(10, 30)))).thenReturn(Optional.of(visit1));

        Optional<Visit> result = visitRepository.findByDoctorAndDateTime(doctor, LocalDate.now(), LocalTime.of(10, 30));

        assertTrue(result.isPresent());
        assertEquals(patient1.getEgn(), result.get().getPatient().getEgn());
        verify(visitRepository).findByDoctorAndDateTime(eq(doctor), eq(LocalDate.now()), eq(LocalTime.of(10, 30)));
    }

}