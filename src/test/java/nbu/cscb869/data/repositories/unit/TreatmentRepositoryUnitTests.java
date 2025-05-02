package nbu.cscb869.data.repositories.unit;

import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.TreatmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TreatmentRepository}, focusing on query methods and behavior.
 */
@ExtendWith(MockitoExtension.class)
class TreatmentRepositoryUnitTests {

    @Mock
    private TreatmentRepository treatmentRepository;

    private Treatment treatment;
    private Visit visit;

    @BeforeEach
    void setUp() {
        visit = new Visit();
        visit.setId(1L);

        treatment = new Treatment();
        treatment.setId(2L);
        treatment.setVisit(visit);
    }

    @Test
    void FindByVisitAndIsDeletedFalse_ValidVisit_ReturnsTreatment() {
        when(treatmentRepository.findByVisitAndIsDeletedFalse(visit)).thenReturn(Optional.of(treatment));

        Optional<Treatment> result = treatmentRepository.findByVisitAndIsDeletedFalse(visit);

        assertThat(result).isPresent();
        assertThat(result.get().getVisit()).isEqualTo(visit);
    }

    @Test
    void FindByVisitAndIsDeletedFalse_NoTreatment_ReturnsEmpty() {
        when(treatmentRepository.findByVisitAndIsDeletedFalse(visit)).thenReturn(Optional.empty());

        Optional<Treatment> result = treatmentRepository.findByVisitAndIsDeletedFalse(visit);

        assertThat(result).isEmpty();
    }

    @Test
    void FindByVisitAndIsDeletedFalse_NullVisit_ThrowsIllegalArgumentException() {
        when(treatmentRepository.findByVisitAndIsDeletedFalse(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> treatmentRepository.findByVisitAndIsDeletedFalse(null));
    }
}