package nbu.cscb869.data.repositories.unit;

import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.SpecialtyRepository;
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
 * Unit tests for {@link SpecialtyRepository}, focusing on query methods and behavior.
 */
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
    }

    @Test
    void FindByName_ValidName_ReturnsSpecialty() {
        when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(specialty));

        Optional<Specialty> result = specialtyRepository.findByName("Cardiology");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Cardiology");
    }

    @Test
    void FindByName_NonExistentName_ReturnsEmpty() {
        when(specialtyRepository.findByName("Neurology")).thenReturn(Optional.empty());

        Optional<Specialty> result = specialtyRepository.findByName("Neurology");

        assertThat(result).isEmpty();
    }

    @Test
    void FindByName_NullName_ThrowsIllegalArgumentException() {
        when(specialtyRepository.findByName(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> specialtyRepository.findByName(null));
    }
}