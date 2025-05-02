package nbu.cscb869.data.repositories.unit;

import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.repositories.MedicineRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MedicineRepository}, focusing on query methods and behavior.
 */
@ExtendWith(MockitoExtension.class)
class MedicineRepositoryUnitTests {

    @Mock
    private MedicineRepository medicineRepository;

    private Medicine medicine;
    private Treatment treatment;

    @BeforeEach
    void setUp() {
        treatment = new Treatment();
        treatment.setId(1L);

        medicine = new Medicine();
        medicine.setId(2L);
        medicine.setName("Aspirin");
        medicine.setTreatment(treatment);
    }

    @Test
    void FindByTreatmentAndIsDeletedFalse_ValidTreatment_ReturnsPagedMedicines() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Medicine> page = new PageImpl<>(Collections.singletonList(medicine), pageable, 1);
        when(medicineRepository.findByTreatmentAndIsDeletedFalse(treatment, pageable)).thenReturn(page);

        Page<Medicine> result = medicineRepository.findByTreatmentAndIsDeletedFalse(treatment, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTreatment()).isEqualTo(treatment);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByTreatmentAndIsDeletedFalse_NoMedicines_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Medicine> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(medicineRepository.findByTreatmentAndIsDeletedFalse(treatment, pageable)).thenReturn(emptyPage);

        Page<Medicine> result = medicineRepository.findByTreatmentAndIsDeletedFalse(treatment, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByTreatmentAndIsDeletedFalse_NullTreatment_ThrowsIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(medicineRepository.findByTreatmentAndIsDeletedFalse(null, pageable)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> medicineRepository.findByTreatmentAndIsDeletedFalse(null, pageable));
    }
}