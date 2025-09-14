// nbu.cscb869.data.repositories.unittests/SpecialtyRepositoryUnitTests.java
package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyRepositoryUnitTests {

    @Mock
    private SpecialtyRepository specialtyRepository;

    private Specialty createSpecialty(String name, String description) {
        return Specialty.builder()
                .name(name)
                .description(description)
                .build();
    }

    @Test
    void findByName_ExistingName_ReturnsSpecialty_HappyPath() {
        Specialty specialty = createSpecialty("Cardiology", "Heart-related specialties");
        when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(specialty));

        Optional<Specialty> found = specialtyRepository.findByName("Cardiology");

        assertTrue(found.isPresent());
        assertEquals("Cardiology", found.get().getName());
        verify(specialtyRepository).findByName("Cardiology");
    }

    @Test
    void findAll_WithData_ReturnsPaged_HappyPath() {
        Specialty specialty = createSpecialty("Cardiology", "Heart-related specialties");
        Page<Specialty> page = new PageImpl<>(List.of(specialty));
        when(specialtyRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Specialty> result = specialtyRepository.findAll(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Cardiology", result.getContent().getFirst().getName());
        verify(specialtyRepository).findAll(any(Pageable.class));
    }

    @Test
    void findByName_NonExistentName_ReturnsEmpty_ErrorCase() {
        when(specialtyRepository.findByName("Nonexistent")).thenReturn(Optional.empty());

        Optional<Specialty> found = specialtyRepository.findByName("Nonexistent");

        assertFalse(found.isPresent());
        verify(specialtyRepository).findByName("Nonexistent");
    }

    @Test
    void findAll_NoData_ReturnsEmptyPage_ErrorCase() {
        Page<Specialty> emptyPage = new PageImpl<>(List.of());
        when(specialtyRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<Specialty> result = specialtyRepository.findAll(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(specialtyRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAll_WithLargePageSize_EdgeCase() {
        List<Specialty> specialties = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            specialties.add(createSpecialty("Specialty" + i, "Description" + i));
        }
        Page<Specialty> page = new PageImpl<>(specialties);
        when(specialtyRepository.findAll(eq(PageRequest.of(0, 10)))).thenReturn(page);

        Page<Specialty> result = specialtyRepository.findAll(PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
        verify(specialtyRepository).findAll(eq(PageRequest.of(0, 10)));
    }
}