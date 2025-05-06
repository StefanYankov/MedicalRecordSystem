package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.repositories.TreatmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentRepositoryUnitTests {

    @Mock
    private TreatmentRepository treatmentRepository;

    private Treatment treatment;

    @BeforeEach
    void setUp() {
        treatment = new Treatment();
        treatment.setId(1L);
        treatment.setDescription("Antibiotic therapy");
    }

    // Happy Path
    @Test
    void Save_WithValidTreatment_ReturnsSaved() {
        when(treatmentRepository.save(treatment)).thenReturn(treatment);

        Treatment savedTreatment = treatmentRepository.save(treatment);

        assertEquals("Antibiotic therapy", savedTreatment.getDescription());
        verify(treatmentRepository).save(treatment);
    }

    @Test
    void FindById_WithValidId_ReturnsTreatment() {
        when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));

        Optional<Treatment> foundTreatment = treatmentRepository.findById(1L);

        assertTrue(foundTreatment.isPresent());
        assertEquals("Antibiotic therapy", foundTreatment.get().getDescription());
        verify(treatmentRepository).findById(1L);
    }

    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(treatmentRepository.findAllActive()).thenReturn(Collections.singletonList(treatment));

        List<Treatment> result = treatmentRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Antibiotic therapy", result.getFirst().getDescription());
        verify(treatmentRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Treatment> page = new PageImpl<>(Collections.singletonList(treatment));
        when(treatmentRepository.findAllActive(any(PageRequest.class))).thenReturn(page);

        Page<Treatment> result = treatmentRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Antibiotic therapy", result.getContent().getFirst().getDescription());
        verify(treatmentRepository).findAllActive(any(PageRequest.class));
    }

    @Test
    void FindAllActivePaged_WithLastPageFewerElements_ReturnsCorrectPage() {
        Treatment treatment2 = new Treatment();
        treatment2.setId(2L);
        treatment2.setDescription("Pain relief therapy");

        Treatment treatment3 = new Treatment();
        treatment3.setId(3L);
        treatment3.setDescription("Antiviral therapy");

        Page<Treatment> page = new PageImpl<>(Collections.singletonList(treatment3), PageRequest.of(1, 2), 3);
        when(treatmentRepository.findAllActive(PageRequest.of(1, 2))).thenReturn(page);

        Page<Treatment> result = treatmentRepository.findAllActive(PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Antiviral therapy", result.getContent().getFirst().getDescription());
        assertEquals(2, result.getTotalPages());
        verify(treatmentRepository).findAllActive(PageRequest.of(1, 2));
    }

    @Test
    void SoftDelete_WithValidTreatment_SetsIsDeleted() {
        doNothing().when(treatmentRepository).delete(treatment);

        treatmentRepository.delete(treatment);

        verify(treatmentRepository).delete(treatment);
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(treatmentRepository).hardDeleteById(1L);

        treatmentRepository.hardDeleteById(1L);

        verify(treatmentRepository).hardDeleteById(1L);
    }

    // Error Cases
    @Test
    void Save_WithNullVisit_ThrowsException() {
        Treatment invalidTreatment = new Treatment();
        invalidTreatment.setDescription("Invalid therapy");

        when(treatmentRepository.save(invalidTreatment)).thenThrow(org.springframework.dao.DataIntegrityViolationException.class);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> treatmentRepository.save(invalidTreatment));
        verify(treatmentRepository).save(invalidTreatment);
    }

    @Test
    void FindById_WithNonExistentId_ReturnsEmpty() {
        when(treatmentRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Treatment> foundTreatment = treatmentRepository.findById(999L);

        assertFalse(foundTreatment.isPresent());
        verify(treatmentRepository).findById(999L);
    }

    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(treatmentRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Treatment> result = treatmentRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(treatmentRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Treatment> emptyPage = new PageImpl<>(Collections.emptyList());
        when(treatmentRepository.findAllActive(any(PageRequest.class))).thenReturn(emptyPage);

        Page<Treatment> result = treatmentRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(treatmentRepository).findAllActive(any(PageRequest.class));
    }

    // Edge Cases
    @Test
    void FindAllActive_WithSoftDeletedTreatment_ExcludesDeleted() {
        Treatment deletedTreatment = new Treatment();
        deletedTreatment.setId(2L);
        deletedTreatment.setDescription("Deleted therapy");
        deletedTreatment.setIsDeleted(true);

        when(treatmentRepository.findAllActive()).thenReturn(Collections.singletonList(treatment));

        List<Treatment> result = treatmentRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Antibiotic therapy", result.getFirst().getDescription());
        assertFalse(result.contains(deletedTreatment));
        verify(treatmentRepository).findAllActive();
    }
}