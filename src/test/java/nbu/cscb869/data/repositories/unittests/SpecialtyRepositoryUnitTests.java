package nbu.cscb869.data.repositories.unittests;

import jakarta.validation.ConstraintViolationException;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.SpecialtyRepository;
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
class SpecialtyRepositoryUnitTests {

    @Mock
    private SpecialtyRepository specialtyRepository;

    private Specialty specialty;

    @BeforeEach
    void setUp() {
        specialty = new Specialty();
        specialty.setId(1L);
        specialty.setName("Cardiology");
        specialty.setDescription("Heart-related specialties");
    }

    // Happy Path
    @Test
    void Save_WithValidSpecialty_ReturnsSaved() {
        when(specialtyRepository.save(specialty)).thenReturn(specialty);

        Specialty savedSpecialty = specialtyRepository.save(specialty);

        assertEquals("Cardiology", savedSpecialty.getName());
        assertEquals("Heart-related specialties", savedSpecialty.getDescription());
        verify(specialtyRepository).save(specialty);
    }

    @Test
    void FindById_WithValidId_ReturnsSpecialty() {
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialty));

        Optional<Specialty> foundSpecialty = specialtyRepository.findById(1L);

        assertTrue(foundSpecialty.isPresent());
        assertEquals("Cardiology", foundSpecialty.get().getName());
        verify(specialtyRepository).findById(1L);
    }

    @Test
    void FindByName_WithValidName_ReturnsSpecialty() {
        when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(specialty));

        Optional<Specialty> foundSpecialty = specialtyRepository.findByName("Cardiology");

        assertTrue(foundSpecialty.isPresent());
        assertEquals("Cardiology", foundSpecialty.get().getName());
        verify(specialtyRepository).findByName("Cardiology");
    }

    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(specialtyRepository.findAllActive()).thenReturn(Collections.singletonList(specialty));

        List<Specialty> result = specialtyRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Cardiology", result.getFirst().getName());
        verify(specialtyRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Specialty> page = new PageImpl<>(Collections.singletonList(specialty));
        when(specialtyRepository.findAllActive(any(PageRequest.class))).thenReturn(page);

        Page<Specialty> result = specialtyRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Cardiology", result.getContent().getFirst().getName());
        verify(specialtyRepository).findAllActive(any(PageRequest.class));
    }

    @Test
    void FindAllActivePaged_WithLastPageFewerElements_ReturnsCorrectPage() {
        Specialty specialty2 = new Specialty();
        specialty2.setId(2L);
        specialty2.setName("Neurology");
        specialty2.setDescription("Brain-related specialties");

        Specialty specialty3 = new Specialty();
        specialty3.setId(3L);
        specialty3.setName("Pediatrics");
        specialty3.setDescription("Child-related specialties");

        Page<Specialty> page = new PageImpl<>(Collections.singletonList(specialty3), PageRequest.of(1, 2), 3);
        when(specialtyRepository.findAllActive(PageRequest.of(1, 2))).thenReturn(page);

        Page<Specialty> result = specialtyRepository.findAllActive(PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Pediatrics", result.getContent().getFirst().getName());
        assertEquals(2, result.getTotalPages());
        verify(specialtyRepository).findAllActive(PageRequest.of(1, 2));
    }

    @Test
    void SoftDelete_WithValidSpecialty_SetsIsDeleted() {
        doNothing().when(specialtyRepository).delete(specialty);

        specialtyRepository.delete(specialty);

        verify(specialtyRepository).delete(specialty);
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(specialtyRepository).hardDeleteById(1L);

        specialtyRepository.hardDeleteById(1L);

        verify(specialtyRepository).hardDeleteById(1L);
    }

    // Error Cases
    @Test
    void Save_WithNullName_ThrowsException() {
        Specialty invalidSpecialty = new Specialty();
        invalidSpecialty.setName(null);
        invalidSpecialty.setDescription("Description");

        when(specialtyRepository.save(invalidSpecialty)).thenThrow(ConstraintViolationException.class);

        assertThrows(ConstraintViolationException.class, () -> specialtyRepository.save(invalidSpecialty));
        verify(specialtyRepository).save(invalidSpecialty);
    }

    @Test
    void FindById_WithNonExistentId_ReturnsEmpty() {
        when(specialtyRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Specialty> foundSpecialty = specialtyRepository.findById(999L);

        assertFalse(foundSpecialty.isPresent());
        verify(specialtyRepository).findById(999L);
    }

    @Test
    void FindByName_WithNonExistentName_ReturnsEmpty() {
        when(specialtyRepository.findByName("Neurology")).thenReturn(Optional.empty());

        Optional<Specialty> foundSpecialty = specialtyRepository.findByName("Neurology");

        assertFalse(foundSpecialty.isPresent());
        verify(specialtyRepository).findByName("Neurology");
    }

    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(specialtyRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Specialty> result = specialtyRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(specialtyRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Specialty> emptyPage = new PageImpl<>(Collections.emptyList());
        when(specialtyRepository.findAllActive(any(PageRequest.class))).thenReturn(emptyPage);

        Page<Specialty> result = specialtyRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(specialtyRepository).findAllActive(any(PageRequest.class));
    }

    // Edge Cases
    @Test
    void FindByName_WithSoftDeletedSpecialty_ReturnsEmpty() {
        Specialty deletedSpecialty = new Specialty();
        deletedSpecialty.setId(2L);
        deletedSpecialty.setName("Deleted");
        deletedSpecialty.setDescription("Deleted specialty");
        deletedSpecialty.setIsDeleted(true);

        when(specialtyRepository.findByName("Deleted")).thenReturn(Optional.empty());

        Optional<Specialty> foundSpecialty = specialtyRepository.findByName("Deleted");

        assertFalse(foundSpecialty.isPresent());
        verify(specialtyRepository).findByName("Deleted");
    }
}