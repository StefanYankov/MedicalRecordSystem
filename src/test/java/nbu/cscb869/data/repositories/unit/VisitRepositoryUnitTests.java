package nbu.cscb869.data.repositories.unit;

import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.VisitRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VisitRepository}, focusing on query methods and behavior.
 */
@ExtendWith(MockitoExtension.class)
class VisitRepositoryUnitTests {

    @Mock
    private VisitRepository visitRepository;

    private Visit visit;
    private Patient patient;
    private Doctor doctor;
    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setEgn("1234567890");

        doctor = new Doctor();
        doctor.setId(2L);
        doctor.setUniqueIdNumber("12345");

        diagnosis = new Diagnosis();
        diagnosis.setId(3L);
        diagnosis.setName("Flu");

        visit = new Visit();
        visit.setId(4L);
        visit.setVisitDate(LocalDate.now());
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
    }

    @Test
    void FindByPatientAndIsDeletedFalse_ValidPatient_ReturnsPagedVisits() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit), pageable, 1);
        when(visitRepository.findByPatientAndIsDeletedFalse(patient, pageable)).thenReturn(page);

        Page<Visit> result = visitRepository.findByPatientAndIsDeletedFalse(patient, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getPatient()).isEqualTo(patient);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByPatientAndIsDeletedFalse_NoVisits_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(visitRepository.findByPatientAndIsDeletedFalse(patient, pageable)).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByPatientAndIsDeletedFalse(patient, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByPatientAndIsDeletedFalse_NullPatient_ThrowsIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(visitRepository.findByPatientAndIsDeletedFalse(null, pageable)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> visitRepository.findByPatientAndIsDeletedFalse(null, pageable));
    }

    @Test
    void FindByDoctorAndIsDeletedFalse_ValidDoctor_ReturnsPagedVisits() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit), pageable, 1);
        when(visitRepository.findByDoctorAndIsDeletedFalse(doctor, pageable)).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctorAndIsDeletedFalse(doctor, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getDoctor()).isEqualTo(doctor);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByDoctorAndIsDeletedFalse_NoVisits_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(visitRepository.findByDoctorAndIsDeletedFalse(doctor, pageable)).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctorAndIsDeletedFalse(doctor, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByDoctorAndIsDeletedFalse_NullDoctor_ThrowsIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(visitRepository.findByDoctorAndIsDeletedFalse(null, pageable)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> visitRepository.findByDoctorAndIsDeletedFalse(null, pageable));
    }

    @Test
    void FindByDoctorAndVisitDateBetweenAndIsDeletedFalse_ValidDoctorAndDates_ReturnsPagedVisits() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit), pageable, 1);
        when(visitRepository.findByDoctorAndVisitDateBetweenAndIsDeletedFalse(doctor, startDate, endDate, pageable)).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctorAndVisitDateBetweenAndIsDeletedFalse(doctor, startDate, endDate, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getDoctor()).isEqualTo(doctor);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByDoctorAndVisitDateBetweenAndIsDeletedFalse_NoVisits_ReturnsEmptyPage() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(visitRepository.findByDoctorAndVisitDateBetweenAndIsDeletedFalse(doctor, startDate, endDate, pageable)).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctorAndVisitDateBetweenAndIsDeletedFalse(doctor, startDate, endDate, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByDoctorAndVisitDateBetweenAndIsDeletedFalse_NullDoctor_ThrowsIllegalArgumentException() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        when(visitRepository.findByDoctorAndVisitDateBetweenAndIsDeletedFalse(null, startDate, endDate, pageable))
                .thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class,
                () -> visitRepository.findByDoctorAndVisitDateBetweenAndIsDeletedFalse(null, startDate, endDate, pageable));
    }

    @Test
    void FindByDoctorAndVisitDateBetweenAndIsDeletedFalse_NullStartDate_ThrowsIllegalArgumentException() {
        LocalDate endDate = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        when(visitRepository.findByDoctorAndVisitDateBetweenAndIsDeletedFalse(doctor, null, endDate, pageable))
                .thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class,
                () -> visitRepository.findByDoctorAndVisitDateBetweenAndIsDeletedFalse(doctor, null, endDate, pageable));
    }

    @Test
    void FindByDiagnosisAndIsDeletedFalse_ValidDiagnosis_ReturnsPagedVisits() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit), pageable, 1);
        when(visitRepository.findByDiagnosisAndIsDeletedFalse(diagnosis, pageable)).thenReturn(page);

        Page<Visit> result = visitRepository.findByDiagnosisAndIsDeletedFalse(diagnosis, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getDiagnosis()).isEqualTo(diagnosis);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByDiagnosisAndIsDeletedFalse_NoVisits_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(visitRepository.findByDiagnosisAndIsDeletedFalse(diagnosis, pageable)).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDiagnosisAndIsDeletedFalse(diagnosis, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByDiagnosisAndIsDeletedFalse_NullDiagnosis_ThrowsIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(visitRepository.findByDiagnosisAndIsDeletedFalse(null, pageable)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> visitRepository.findByDiagnosisAndIsDeletedFalse(null, pageable));
    }

    @Test
    void FindByVisitDateBetweenAndIsDeletedFalse_ValidDates_ReturnsPagedVisits() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit), pageable, 1);
        when(visitRepository.findByVisitDateBetweenAndIsDeletedFalse(startDate, endDate, pageable)).thenReturn(page);

        Page<Visit> result = visitRepository.findByVisitDateBetweenAndIsDeletedFalse(startDate, endDate, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByVisitDateBetweenAndIsDeletedFalse_NoVisits_ReturnsEmptyPage() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(visitRepository.findByVisitDateBetweenAndIsDeletedFalse(startDate, endDate, pageable)).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByVisitDateBetweenAndIsDeletedFalse(startDate, endDate, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByVisitDateBetweenAndIsDeletedFalse_NullStartDate_ThrowsIllegalArgumentException() {
        LocalDate endDate = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        when(visitRepository.findByVisitDateBetweenAndIsDeletedFalse(null, endDate, pageable))
                .thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class,
                () -> visitRepository.findByVisitDateBetweenAndIsDeletedFalse(null, endDate, pageable));
    }
}