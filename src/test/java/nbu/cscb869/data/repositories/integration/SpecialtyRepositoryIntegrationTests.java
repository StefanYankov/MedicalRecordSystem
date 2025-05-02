package nbu.cscb869.data.repositories.integration;

import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SpecialtyRepositoryIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Specialty specialty;

    @BeforeEach
    void setUp() {
        specialty = new Specialty();
        specialty.setName("Cardiology");
        specialty = specialtyRepository.save(specialty);
    }

    @Test
    void FindByName_ValidName_ReturnsSpecialty() {
        Optional<Specialty> result = specialtyRepository.findByName("Cardiology");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Cardiology");
    }

    @Test
    void FindByName_NonExistentName_ReturnsEmpty() {
        Optional<Specialty> result = specialtyRepository.findByName("Neurology");
        assertThat(result).isEmpty();
    }

    @Test
    void FindByName_DeletedSpecialty_ExcludesFromResult() {
        specialtyRepository.softDeleteById(specialty.getId());
        Optional<Specialty> result = specialtyRepository.findByName("Cardiology");
        assertThat(result).isEmpty();
    }

    @Test
    void FindByNameAndIsDeletedFalse_ValidName_ReturnsSpecialty() {
        Optional<Specialty> result = specialtyRepository.findByNameAndIsDeletedFalse("Cardiology");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Cardiology");
    }

    @Test
    void FindByNameAndIsDeletedFalse_DeletedSpecialty_ExcludesFromResult() {
        specialtyRepository.softDeleteById(specialty.getId());
        Optional<Specialty> result = specialtyRepository.findByNameAndIsDeletedFalse("Cardiology");
        assertThat(result).isEmpty();
    }

    @Test
    void FindAllDeleted_ValidCall_ReturnsDeletedSpecialties() {
        specialtyRepository.softDeleteById(specialty.getId());
        List<Specialty> result = specialtyRepository.findAllDeleted();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Cardiology");
    }
}