package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.repositories.SickLeaveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SickLeaveRepositoryUnitTests {

    @Mock
    private SickLeaveRepository sickLeaveRepository;

    private SickLeave sickLeave;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Smith");

        sickLeave = new SickLeave();
        sickLeave.setId(1L);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
    }

    // Happy Path
    @Test
    void Save_WithValidSickLeave_ReturnsSaved() {
        when(sickLeaveRepository.save(sickLeave)).thenReturn(sickLeave);

        SickLeave savedSickLeave = sickLeaveRepository.save(sickLeave);

        assertEquals(5, savedSickLeave.getDurationDays());
        assertEquals(LocalDate.now(), savedSickLeave.getStartDate());
        verify(sickLeaveRepository).save(sickLeave);
    }

    @Test
    void FindById_WithValidId_ReturnsSickLeave() {
        when(sickLeaveRepository.findById(1L)).thenReturn(Optional.of(sickLeave));

        Optional<SickLeave> foundSickLeave = sickLeaveRepository.findById(1L);

        assertTrue(foundSickLeave.isPresent());
        assertEquals(5, foundSickLeave.get().getDurationDays());
        verify(sickLeaveRepository).findById(1L);
    }

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
        when(sickLeaveRepository.findAllActive(any(PageRequest.class))).thenReturn(page);

        Page<SickLeave> result = sickLeaveRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(5, result.getContent().getFirst().getDurationDays());
        verify(sickLeaveRepository).findAllActive(any(PageRequest.class));
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
    void SoftDelete_WithValidSickLeave_SetsIsDeleted() {
        doNothing().when(sickLeaveRepository).delete(sickLeave);

        sickLeaveRepository.delete(sickLeave);

        verify(sickLeaveRepository).delete(sickLeave);
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(sickLeaveRepository).hardDeleteById(1L);

        sickLeaveRepository.hardDeleteById(1L);

        verify(sickLeaveRepository).hardDeleteById(1L);
    }

    // Error Cases
    @Test
    void FindById_WithNonExistentId_ReturnsEmpty() {
        when(sickLeaveRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<SickLeave> foundSickLeave = sickLeaveRepository.findById(999L);

        assertFalse(foundSickLeave.isPresent());
        verify(sickLeaveRepository).findById(999L);
    }

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
        when(sickLeaveRepository.findAllActive(any(PageRequest.class))).thenReturn(emptyPage);

        Page<SickLeave> result = sickLeaveRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(sickLeaveRepository).findAllActive(any(PageRequest.class));
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
    void FindAllActive_WithSoftDeletedSickLeave_ExcludesDeleted() {
        SickLeave deletedSickLeave = new SickLeave();
        deletedSickLeave.setId(2L);
        deletedSickLeave.setStartDate(LocalDate.now());
        deletedSickLeave.setDurationDays(3);
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

        SickLeave sickLeave3 = new SickLeave();
        sickLeave3.setId(3L);
        sickLeave3.setStartDate(LocalDate.now());
        sickLeave3.setDurationDays(10);

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