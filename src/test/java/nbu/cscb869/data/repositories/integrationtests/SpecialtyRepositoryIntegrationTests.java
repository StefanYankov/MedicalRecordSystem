package nbu.cscb869.data.repositories.integrationtests;

import jakarta.validation.ConstraintViolationException;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(SpecialtyRepositoryIntegrationTests.TestConfig.class)
class SpecialtyRepositoryIntegrationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ClientRegistrationRepository clientRegistrationRepository() {
            return Mockito.mock(ClientRegistrationRepository.class);
        }

        @Bean
        @Primary
        public JwtDecoder jwtDecoder() {
            return Mockito.mock(JwtDecoder.class);
        }
    }

    @MockBean
    private Keycloak keycloak;

    @Autowired
    private SpecialtyRepository specialtyRepository;
    @Autowired
    private DoctorRepository doctorRepository;

    @BeforeEach
    void setUp() {
        specialtyRepository.deleteAll();
        doctorRepository.deleteAll();
    }

    private Specialty createSpecialty(String name, String description) {
        return Specialty.builder()
                .name(name)
                .description(description)
                .build();
    }

    private Doctor createDoctor(String uniqueIdNumber, boolean isGeneralPractitioner, String name) {
        return Doctor.builder()
                .uniqueIdNumber(uniqueIdNumber)
                .isGeneralPractitioner(isGeneralPractitioner)
                .name(name)
                .keycloakId(TestDataUtils.generateKeycloakId())
                .build();
    }

    @Test
    void save_WithValidSpecialty_SavesSuccessfully_HappyPath() {
        Specialty specialty = createSpecialty("Cardiology", "Heart-related conditions");
        Specialty saved = specialtyRepository.save(specialty);

        Optional<Specialty> found = specialtyRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Cardiology", found.get().getName());
    }

    @Test
    void save_WithAssociatedDoctors_SavesSuccessfully_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        doctor = doctorRepository.save(doctor);
        Specialty specialty = createSpecialty("Cardiology", "Heart-related conditions");
        specialty.setDoctors(java.util.Set.of(doctor));
        Specialty saved = specialtyRepository.save(specialty);

        Optional<Specialty> found = specialtyRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Cardiology", found.get().getName());
        assertEquals(1, found.get().getDoctors().size());
    }

    @Test
    void findByName_WithExistingName_ReturnsSpecialty_HappyPath() {
        Specialty specialty = createSpecialty("Neurology", "Nervous system disorders");
        specialtyRepository.save(specialty);

        Optional<Specialty> found = specialtyRepository.findByName("Neurology");

        assertTrue(found.isPresent());
        assertEquals("Neurology", found.get().getName());
    }

    @Test
    void findAll_WithMultipleSpecialties_ReturnsPaged_HappyPath() {
        List<Specialty> specialties = List.of(
                createSpecialty("Pediatrics", "Child healthcare"),
                createSpecialty("Oncology", "Cancer treatment")
        );
        specialtyRepository.saveAll(specialties);

        Page<Specialty> result = specialtyRepository.findAll(PageRequest.of(0, 1));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void save_WithDuplicateName_ThrowsException_ErrorCase() {
        Specialty specialty1 = createSpecialty("Urology", "Urinary system disorders");
        Specialty specialty2 = createSpecialty("Urology", "Another description");
        specialtyRepository.save(specialty1);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            specialtyRepository.save(specialty2);
        });
    }

    @Test
    void save_WithNullName_ThrowsException_ErrorCase() {
        Specialty specialty = createSpecialty(null, "Description");

        assertThrows(ConstraintViolationException.class, () -> {
            specialtyRepository.save(specialty);
        });
    }

    @Test
    void findByName_WithNonExistentName_ReturnsEmpty_ErrorCase() {
        assertFalse(specialtyRepository.findByName("Nonexistent").isPresent());
    }

    @Test
    void findAll_WithNoSpecialties_ReturnsEmptyPage_ErrorCase() {
        Page<Specialty> result = specialtyRepository.findAll(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void findAll_WithLargePageSize_EdgeCase() {
        List<Specialty> specialties = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            specialties.add(createSpecialty("Specialty" + i, "Description" + i));
        }
        specialtyRepository.saveAll(specialties);

        Page<Specialty> result = specialtyRepository.findAll(PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
    }
}
