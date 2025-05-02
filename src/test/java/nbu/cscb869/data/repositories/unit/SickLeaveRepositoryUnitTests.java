package nbu.cscb869.data.repositories.unit;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.SickLeaveRepository;
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
 * Unit tests for {@link SickLeaveRepository}, focusing on query methods and behavior.
 */
@ExtendWith(MockitoExtension.class)
class SickLeaveRepositoryUnitTests {

    @Mock
    private SickLeaveRepository sickLeaveRepository;

    private SickLeave sickLeave;
    private Patient patient;
    private Doctor doctor;
    private Visit visit;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setEgn("1234567890");

        doctor = new Doctor();
        doctor.setId(2L);
        doctor.setUniqueIdNumber("12345");

        visit = new Visit();
        visit.setId(3L);
        visit.setPatient(patient);
        visit.setDoctor(doctor);

        sickLeave = new SickLeave();
        sickLeave.setId(4L);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave.setVisit(visit);
    }

    @Test
    void FindByPatient_ValidPatient_ReturnsPagedSickLeaves() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SickLeave> page = new PageImpl<>(Collections.singletonList(sickLeave), pageable, 1);
        when(sickLeaveRepository.findByPatient(patient, pageable)).thenReturn(page);

        Page<SickLeave> result = sickLeaveRepository.findByPatient(patient, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getVisit().getPatient()).isEqualTo(patient);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByPatient_NoSickLeaves_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SickLeave> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(sickLeaveRepository.findByPatient(patient, pageable)).thenReturn(emptyPage);

        Page<SickLeave> result = sickLeaveRepository.findByPatient(patient, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByPatient_NullPatient_ThrowsIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(sickLeaveRepository.findByPatient(null, pageable)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> sickLeaveRepository.findByPatient(null, pageable));
    }

    @Test
    void FindByDoctor_ValidDoctor_ReturnsPagedSickLeaves() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SickLeave> page = new PageImpl<>(Collections.singletonList(sickLeave), pageable, 1);
        when(sickLeaveRepository.findByDoctor(doctor, pageable)).thenReturn(page);

        Page<SickLeave> result = sickLeaveRepository.findByDoctor(doctor, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getVisit().getDoctor()).isEqualTo(doctor);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByDoctor_NoSickLeaves_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SickLeave> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(sickLeaveRepository.findByDoctor(doctor, pageable)).thenReturn(emptyPage);

        Page<SickLeave> result = sickLeaveRepository.findByDoctor(doctor, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByDoctor_NullDoctor_ThrowsIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(sickLeaveRepository.findByDoctor(null, pageable)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> sickLeaveRepository.findByDoctor(null, pageable));
    }

    @Test
    void FindMonthWithMostSickLeaves_ValidYear_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Object[] resultRow = new Object[]{1, 10L}; // January, 10 sick leaves
        Page<Object[]> page = new PageImpl<>(Collections.singletonList(resultRow), pageable, 1);
        when(sickLeaveRepository.findMonthWithMostSickLeaves(2025, pageable)).thenReturn(page);

        Page<Object[]> result = sickLeaveRepository.findMonthWithMostSickLeaves(2025, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()[0]).isEqualTo(1);
        assertThat(result.getContent().getFirst()[1]).isEqualTo(10L);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindMonthWithMostSickLeaves_NoSickLeaves_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(sickLeaveRepository.findMonthWithMostSickLeaves(2025, pageable)).thenReturn(emptyPage);

        Page<Object[]> result = sickLeaveRepository.findMonthWithMostSickLeaves(2025, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindMonthWithMostSickLeaves_NullPageable_ThrowsIllegalArgumentException() {
        when(sickLeaveRepository.findMonthWithMostSickLeaves(2025, null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> sickLeaveRepository.findMonthWithMostSickLeaves(2025, null));
    }
}