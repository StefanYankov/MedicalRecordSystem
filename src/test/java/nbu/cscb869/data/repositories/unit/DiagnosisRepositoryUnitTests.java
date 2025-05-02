package nbu.cscb869.data.repositories.unit;

import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DiagnosisRepository}, focusing on query methods and behavior.
 */
@ExtendWith(MockitoExtension.class)
class DiagnosisRepositoryUnitTests {

    @Mock
    private DiagnosisRepository diagnosisRepository;

    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        diagnosis = new Diagnosis();
        diagnosis.setId(1L);
        diagnosis.setName("Flu");
    }

    @Test
    void FindByName_ValidName_ReturnsDiagnosis() {
        when(diagnosisRepository.findByName("Flu")).thenReturn(Optional.of(diagnosis));

        Optional<Diagnosis> result = diagnosisRepository.findByName("Flu");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Flu");
    }

    @Test
    void FindByName_NonExistentName_ReturnsEmpty() {
        when(diagnosisRepository.findByName("Cold")).thenReturn(Optional.empty());

        Optional<Diagnosis> result = diagnosisRepository.findByName("Cold");

        assertThat(result).isEmpty();
    }

    @Test
    void FindByName_NullName_ThrowsIllegalArgumentException() {
        when(diagnosisRepository.findByName(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> diagnosisRepository.findByName(null));
    }

    @Test
    void CountPatientsByDiagnosis_ValidDiagnosis_ReturnsPatientCount() {
        when(diagnosisRepository.countPatientsByDiagnosis(diagnosis)).thenReturn(5L);

        long result = diagnosisRepository.countPatientsByDiagnosis(diagnosis);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void CountPatientsByDiagnosis_NoPatients_ReturnsZero() {
        when(diagnosisRepository.countPatientsByDiagnosis(diagnosis)).thenReturn(0L);

        long result = diagnosisRepository.countPatientsByDiagnosis(diagnosis);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void CountPatientsByDiagnosis_NullDiagnosis_ThrowsIllegalArgumentException() {
        when(diagnosisRepository.countPatientsByDiagnosis(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> diagnosisRepository.countPatientsByDiagnosis(null));
    }

    @Test
    void FindMostFrequentDiagnoses_ValidPageable_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Object[] resultRow = new Object[]{diagnosis, 10L};
        Page<Object[]> page = new PageImpl<>(Collections.singletonList(resultRow), pageable, 1);
        when(diagnosisRepository.findMostFrequentDiagnoses(pageable)).thenReturn(page);

        Page<Object[]> result = diagnosisRepository.findMostFrequentDiagnoses(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()[0]).isEqualTo(diagnosis);
        assertThat(result.getContent().getFirst()[1]).isEqualTo(10L);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindMostFrequentDiagnoses_NoDiagnoses_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(diagnosisRepository.findMostFrequentDiagnoses(pageable)).thenReturn(emptyPage);

        Page<Object[]> result = diagnosisRepository.findMostFrequentDiagnoses(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindMostFrequentDiagnoses_NullPageable_ThrowsIllegalArgumentException() {
        when(diagnosisRepository.findMostFrequentDiagnoses(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> diagnosisRepository.findMostFrequentDiagnoses(null));
    }
}