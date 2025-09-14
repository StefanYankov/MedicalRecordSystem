package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.YearMonthSickLeaveCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.SickLeaveRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SickLeaveRepositoryUnitTests {

    @Mock
    private SickLeaveRepository sickLeaveRepository;

    private Doctor createDoctor(String uniqueIdNumber, boolean isGeneralPractitioner, String name) {
        return Doctor.builder()
                .uniqueIdNumber(uniqueIdNumber)
                .isGeneralPractitioner(isGeneralPractitioner)
                .name(name)
                .build();
    }

    private Visit createVisit(Doctor doctor) {
        return Visit.builder()
                .doctor(doctor)
                .build();
    }

    private SickLeave createSickLeave(LocalDate startDate, int durationDays, Visit visit) {
        return SickLeave.builder()
                .startDate(startDate)
                .durationDays(durationDays)
                .visit(visit)
                .build();
    }

    @Test
    void findAll_WithData_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Visit visit = createVisit(doctor);
        SickLeave sickLeave = createSickLeave(LocalDate.now(), 5, visit);
        Page<SickLeave> page = new PageImpl<>(List.of(sickLeave));
        when(sickLeaveRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<SickLeave> result = sickLeaveRepository.findAll(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(5, result.getContent().getFirst().getDurationDays());
        verify(sickLeaveRepository).findAll(any(Pageable.class));
    }

    @Test
    void findYearMonthWithMostSickLeaves_WithData_ReturnsList_HappyPath() {
        YearMonthSickLeaveCountDTO dto = YearMonthSickLeaveCountDTO.builder()
                .year(LocalDate.now().getYear())
                .month(LocalDate.now().getMonthValue())
                .count(1L)
                .build();
        when(sickLeaveRepository.findYearMonthWithMostSickLeaves()).thenReturn(List.of(dto));

        List<YearMonthSickLeaveCountDTO> result = sickLeaveRepository.findYearMonthWithMostSickLeaves();

        assertEquals(1, result.size());
        assertEquals(LocalDate.now().getYear(), result.getFirst().getYear());
        assertEquals(LocalDate.now().getMonthValue(), result.getFirst().getMonth());
        assertEquals(1L, result.getFirst().getCount());
        verify(sickLeaveRepository).findYearMonthWithMostSickLeaves();
    }

    @Test
    void findDoctorsWithMostSickLeaves_WithData_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        DoctorSickLeaveCountDTO dto = DoctorSickLeaveCountDTO.builder()
                .doctor(doctor)
                .sickLeaveCount(1L)
                .build();
        when(sickLeaveRepository.findDoctorsWithMostSickLeaves()).thenReturn(List.of(dto));

        List<DoctorSickLeaveCountDTO> result = sickLeaveRepository.findDoctorsWithMostSickLeaves();

        assertEquals(1, result.size());
        assertEquals(doctor.getUniqueIdNumber(), result.getFirst().getDoctor().getUniqueIdNumber());
        assertEquals(1L, result.getFirst().getSickLeaveCount());
        verify(sickLeaveRepository).findDoctorsWithMostSickLeaves();
    }

    @Test
    void findAll_NoData_ReturnsEmptyPage_ErrorCase() {
        Page<SickLeave> emptyPage = new PageImpl<>(List.of());
        when(sickLeaveRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<SickLeave> result = sickLeaveRepository.findAll(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(sickLeaveRepository).findAll(any(Pageable.class));
    }

    @Test
    void findYearMonthWithMostSickLeaves_NoData_ReturnsEmptyList_ErrorCase() {
        when(sickLeaveRepository.findYearMonthWithMostSickLeaves()).thenReturn(List.of());

        List<YearMonthSickLeaveCountDTO> result = sickLeaveRepository.findYearMonthWithMostSickLeaves();

        assertTrue(result.isEmpty());
        verify(sickLeaveRepository).findYearMonthWithMostSickLeaves();
    }

    @Test
    void findDoctorsWithMostSickLeaves_NoData_ReturnsEmptyList_ErrorCase() {
        when(sickLeaveRepository.findDoctorsWithMostSickLeaves()).thenReturn(List.of());

        List<DoctorSickLeaveCountDTO> result = sickLeaveRepository.findDoctorsWithMostSickLeaves();

        assertTrue(result.isEmpty());
        verify(sickLeaveRepository).findDoctorsWithMostSickLeaves();
    }

    @Test
    void findAll_WithLargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        Visit visit = createVisit(doctor);
        List<SickLeave> sickLeaves = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            sickLeaves.add(createSickLeave(LocalDate.now(), 5, visit));
        }
        Page<SickLeave> page = new PageImpl<>(sickLeaves);
        when(sickLeaveRepository.findAll(eq(PageRequest.of(0, 10)))).thenReturn(page);

        Page<SickLeave> result = sickLeaveRepository.findAll(PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
        verify(sickLeaveRepository).findAll(eq(PageRequest.of(0, 10)));
    }
}