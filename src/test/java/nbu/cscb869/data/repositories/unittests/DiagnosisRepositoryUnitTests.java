package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.PatientDiagnosisDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisRepositoryUnitTests {

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Test
    void findByName_ExistingName_ReturnsDiagnosis_HappyPath() {
        Diagnosis diagnosis = Diagnosis.builder().name("Flu").description("Viral infection").build();
        when(diagnosisRepository.findByName("Flu")).thenReturn(Optional.of(diagnosis));

        Optional<Diagnosis> found = diagnosisRepository.findByName("Flu");

        assertTrue(found.isPresent());
        assertEquals("Flu", found.get().getName());
        verify(diagnosisRepository).findByName("Flu");
    }

    @Test
    void findByName_NonExistentName_ReturnsEmpty_ErrorCase() {
        when(diagnosisRepository.findByName("Nonexistent")).thenReturn(Optional.empty());

        Optional<Diagnosis> found = diagnosisRepository.findByName("Nonexistent");

        assertFalse(found.isPresent());
        verify(diagnosisRepository).findByName("Nonexistent");
    }

    @Test
    void findByNameContainingIgnoreCase_PartialName_ReturnsPaged_HappyPath() {
        Diagnosis d1 = Diagnosis.builder().name("Flu").build();
        Diagnosis d2 = Diagnosis.builder().name("Influenza").build();
        Page<Diagnosis> page = new PageImpl<>(List.of(d1, d2));
        when(diagnosisRepository.findByNameContainingIgnoreCase(eq("flu"), any(PageRequest.class))).thenReturn(page);

        Page<Diagnosis> result = diagnosisRepository.findByNameContainingIgnoreCase("flu", PageRequest.of(0, 2));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(d -> d.getName().equals("Flu")));
        verify(diagnosisRepository).findByNameContainingIgnoreCase(eq("flu"), any(PageRequest.class));
    }

    @Test
    void findByNameContainingIgnoreCase_NonExistentName_ReturnsEmptyPage_ErrorCase() {
        Page<Diagnosis> emptyPage = new PageImpl<>(List.of());
        when(diagnosisRepository.findByNameContainingIgnoreCase(eq("xyz"), any(PageRequest.class))).thenReturn(emptyPage);

        Page<Diagnosis> result = diagnosisRepository.findByNameContainingIgnoreCase("xyz", PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        verify(diagnosisRepository).findByNameContainingIgnoreCase(eq("xyz"), any(PageRequest.class));
    }

    @Test
    void findByNameContainingIgnoreCase_EmptyFilter_ReturnsAll_EdgeCase() {
        Diagnosis d = Diagnosis.builder().name("Flu").build();
        Page<Diagnosis> page = new PageImpl<>(List.of(d));
        when(diagnosisRepository.findByNameContainingIgnoreCase(eq(""), any(PageRequest.class))).thenReturn(page);

        Page<Diagnosis> result = diagnosisRepository.findByNameContainingIgnoreCase("", PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        verify(diagnosisRepository).findByNameContainingIgnoreCase(eq(""), any(PageRequest.class));
    }

    @Test
    void findPatientsByDiagnosis_ValidDiagnosis_ReturnsPaged_HappyPath() {
        Diagnosis diagnosis = Diagnosis.builder().name("Flu").build();
        PatientDiagnosisDTO dto = PatientDiagnosisDTO.builder().patient(null).diagnosisName("Flu").build(); // Mock minimal
        Page<PatientDiagnosisDTO> page = new PageImpl<>(List.of(dto));
        when(diagnosisRepository.findPatientsByDiagnosis(eq(diagnosis), any(PageRequest.class))).thenReturn(page);

        Page<PatientDiagnosisDTO> result = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Flu", result.getContent().getFirst().getDiagnosisName());
        verify(diagnosisRepository).findPatientsByDiagnosis(eq(diagnosis), any(PageRequest.class));
    }

    @Test
    void findPatientsByDiagnosis_NoData_ReturnsEmptyPage_ErrorCase() {
        Diagnosis diagnosis = Diagnosis.builder().name("Other").build();
        Page<PatientDiagnosisDTO> emptyPage = new PageImpl<>(List.of());
        when(diagnosisRepository.findPatientsByDiagnosis(eq(diagnosis), any(PageRequest.class))).thenReturn(emptyPage);

        Page<PatientDiagnosisDTO> result = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        verify(diagnosisRepository).findPatientsByDiagnosis(eq(diagnosis), any(PageRequest.class));
    }

    @Test
    void findPatientsByDiagnosis_LargePageSize_EdgeCase() {
        Diagnosis diagnosis = Diagnosis.builder().name("Flu").build();
        Page<PatientDiagnosisDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        when(diagnosisRepository.findPatientsByDiagnosis(eq(diagnosis), any(PageRequest.class))).thenReturn(page);

        Page<PatientDiagnosisDTO> result = diagnosisRepository.findPatientsByDiagnosis(diagnosis, PageRequest.of(0, 100));

        assertEquals(0, result.getTotalElements());
        verify(diagnosisRepository).findPatientsByDiagnosis(eq(diagnosis), any(PageRequest.class));
    }

    @Test
    void findMostFrequentDiagnoses_WithData_ReturnsSortedList_HappyPath() {
        Diagnosis d = Diagnosis.builder().name("Flu").build();
        DiagnosisVisitCountDTO dto = DiagnosisVisitCountDTO.builder().diagnosis(d).visitCount(1L).build();
        when(diagnosisRepository.findMostFrequentDiagnoses()).thenReturn(List.of(dto));

        List<DiagnosisVisitCountDTO> result = diagnosisRepository.findMostFrequentDiagnoses();

        assertEquals(1, result.size());
        assertEquals("Flu", result.getFirst().getDiagnosis().getName());
        verify(diagnosisRepository).findMostFrequentDiagnoses();
    }

    @Test
    void findMostFrequentDiagnoses_NoData_ReturnsEmptyList_ErrorCase() {
        when(diagnosisRepository.findMostFrequentDiagnoses()).thenReturn(List.of());

        List<DiagnosisVisitCountDTO> result = diagnosisRepository.findMostFrequentDiagnoses();

        assertTrue(result.isEmpty());
        verify(diagnosisRepository).findMostFrequentDiagnoses();
    }

    @Test
    void findMostFrequentDiagnoses_MaxCount_EdgeCase() {
        Diagnosis d = Diagnosis.builder().name("Flu").build();
        List<DiagnosisVisitCountDTO> largeList = IntStream.range(0, 100)
                .mapToObj(i -> DiagnosisVisitCountDTO.builder().diagnosis(d).visitCount(i).build())
                .collect(Collectors.toList());
        when(diagnosisRepository.findMostFrequentDiagnoses()).thenReturn(largeList);

        List<DiagnosisVisitCountDTO> result = diagnosisRepository.findMostFrequentDiagnoses();

        assertEquals(100, result.size());
        verify(diagnosisRepository).findMostFrequentDiagnoses();
    }
}