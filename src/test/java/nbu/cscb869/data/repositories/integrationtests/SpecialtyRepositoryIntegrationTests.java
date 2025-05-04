package nbu.cscb869.data.repositories.integrationtests;

import jakarta.persistence.EntityManager;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SpecialtyRepositoryIntegrationTests {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        specialtyRepository.deleteAll();
    }

    private Specialty createSpecialty(String name, String description) {
        Specialty specialty = new Specialty();
        specialty.setName(name);
        specialty.setDescription(description);
        return specialty;
    }

    // Happy Path
    @Test
    void Save_WithValidSpecialty_SavesSuccessfully() {
        Specialty specialty = createSpecialty("Cardiology", "Heart-related conditions");
        Specialty savedSpecialty = specialtyRepository.save(specialty);
        Optional<Specialty> foundSpecialty = specialtyRepository.findById(savedSpecialty.getId());

        assertTrue(foundSpecialty.isPresent());
        assertEquals("Cardiology", foundSpecialty.get().getName());
        assertEquals("Heart-related conditions", foundSpecialty.get().getDescription());
        assertFalse(foundSpecialty.get().getIsDeleted());
        assertNotNull(foundSpecialty.get().getCreatedOn());
    }

    @Test
    void FindByName_WithExistingName_ReturnsSpecialty() {
        Specialty specialty = createSpecialty("Neurology", "Nervous system disorders");
        specialtyRepository.save(specialty);

        Optional<Specialty> foundSpecialty = specialtyRepository.findByName("Neurology");

        assertTrue(foundSpecialty.isPresent());
        assertEquals("Neurology", foundSpecialty.get().getName());
    }

    @Test
    void FindAllActive_WithMultipleSpecialties_ReturnsAll() {
        Specialty specialty1 = createSpecialty("Pediatrics", "Child healthcare");
        Specialty specialty2 = createSpecialty("Oncology", "Cancer treatment");
        specialtyRepository.save(specialty1);
        specialtyRepository.save(specialty2);

        List<Specialty> activeSpecialties = specialtyRepository.findAllActive();

        assertEquals(2, activeSpecialties.size());
        assertTrue(activeSpecialties.stream().anyMatch(s -> s.getName().equals("Pediatrics")));
        assertTrue(activeSpecialties.stream().anyMatch(s -> s.getName().equals("Oncology")));
    }

    @Test
    void FindAllActivePaged_WithMultipleSpecialties_ReturnsPaged() {
        Specialty specialty1 = createSpecialty("Dermatology", "Skin conditions");
        Specialty specialty2 = createSpecialty("Orthopedics", "Musculoskeletal disorders");
        specialtyRepository.save(specialty1);
        specialtyRepository.save(specialty2);

        Page<Specialty> activeSpecialties = specialtyRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(2, activeSpecialties.getTotalElements());
        assertEquals(1, activeSpecialties.getContent().size());
    }

    @Test
    void SoftDelete_WithExistingSpecialty_SetsIsDeleted() {
        Specialty specialty = createSpecialty("Endocrinology", "Hormonal disorders");
        Specialty savedSpecialty = specialtyRepository.save(specialty);

        specialtyRepository.delete(savedSpecialty);
        entityManager.flush();
        Optional<Specialty> deletedSpecialty = specialtyRepository.findById(savedSpecialty.getId());
        if (deletedSpecialty.isPresent()) {
            entityManager.refresh(deletedSpecialty.get());
        }

        assertTrue(deletedSpecialty.isPresent());
        assertTrue(deletedSpecialty.get().getIsDeleted());
        assertNotNull(deletedSpecialty.get().getDeletedOn());
        assertEquals(0, specialtyRepository.findAllActive().size());
    }

    @Test
    void HardDelete_WithExistingSpecialty_RemovesSpecialty() {
        Specialty specialty = createSpecialty("Gastroenterology", "Digestive system disorders");
        Specialty savedSpecialty = specialtyRepository.save(specialty);
        entityManager.flush();

        specialtyRepository.hardDeleteById(savedSpecialty.getId());
        entityManager.flush();

        assertTrue(specialtyRepository.findById(savedSpecialty.getId()).isEmpty());
    }

    // Error Cases
    @Test
    void Save_WithDuplicateName_ThrowsException() {
        Specialty specialty1 = createSpecialty("Urology", "Urinary system disorders");
        Specialty specialty2 = createSpecialty("Urology", "Another description");
        specialtyRepository.save(specialty1);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> specialtyRepository.saveAndFlush(specialty2));
    }

    @Test
    void FindByName_WithNonExistentName_ReturnsEmpty() {
        Optional<Specialty> foundSpecialty = specialtyRepository.findByName("Nonexistent");

        assertFalse(foundSpecialty.isPresent());
    }

    @Test
    void HardDelete_WithNonExistentId_DoesNotThrow() {
        assertDoesNotThrow(() -> specialtyRepository.hardDeleteById(999L));
    }

    // Edge Cases
    @Test
    void Save_WithMaximumNameLength_SavesSuccessfully() {
        String maxName = "A".repeat(50);
        Specialty specialty = createSpecialty(maxName, "Description");

        Specialty savedSpecialty = specialtyRepository.save(specialty);

        assertEquals(maxName, savedSpecialty.getName());
    }

    @Test
    void Save_WithMaximumDescriptionLength_SavesSuccessfully() {
        String maxDescription = "D".repeat(500);
        Specialty specialty = createSpecialty("Psychiatry", maxDescription);

        Specialty savedSpecialty = specialtyRepository.save(specialty);

        assertEquals(maxDescription, savedSpecialty.getDescription());
    }

    @Test
    void FindAllActive_WithNoSpecialties_ReturnsEmpty() {
        List<Specialty> activeSpecialties = specialtyRepository.findAllActive();

        assertTrue(activeSpecialties.isEmpty());
    }

    @Test
    void FindAllActivePaged_WithNoSpecialties_ReturnsEmpty() {
        Page<Specialty> activeSpecialties = specialtyRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, activeSpecialties.getTotalElements());
        assertTrue(activeSpecialties.getContent().isEmpty());
    }

    @Test
    void SoftDelete_WithNonExistentSpecialty_DoesNotThrow() {
        Specialty specialty = new Specialty();
        specialty.setId(999L);

        assertDoesNotThrow(() -> specialtyRepository.delete(specialty));
    }

    @Test
    void FindByName_WithSoftDeletedSpecialty_ReturnsEmpty() {
        Specialty specialty = createSpecialty("Ophthalmology", "Eye disorders");
        Specialty savedSpecialty = specialtyRepository.save(specialty);
        specialtyRepository.delete(savedSpecialty);

        Optional<Specialty> foundSpecialty = specialtyRepository.findByName("Ophthalmology");

        assertFalse(foundSpecialty.isPresent());
    }
}