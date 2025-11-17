package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.common.exceptions.EntityInUseException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.SpecialtyCreateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyUpdateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({SpecialtyServiceImplIntegrationTests.AsyncTestConfig.class, SpecialtyServiceImplIntegrationTests.TestConfig.class})
class SpecialtyServiceImplIntegrationTests {

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

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private SpecialtyService specialtyService;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @BeforeEach
    void setUp() {
        doctorRepository.deleteAll();
        specialtyRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void create_AsAdminWithValidData_ShouldPersistSpecialty_HappyPath() {
        // ARRANGE
        SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
        createDTO.setName("Cardiology");
        createDTO.setDescription("Heart-related issues");

        // ACT
        SpecialtyViewDTO result = specialtyService.create(createDTO);

        // ASSERT
        assertNotNull(result);
        assertNotNull(result.getId());
        assertTrue(specialtyRepository.findById(result.getId()).isPresent());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void create_WithDuplicateName_ShouldThrowException_ErrorCase() {
        // ARRANGE
        Specialty specialty = new Specialty();
        specialty.setName("Cardiology");
        specialtyRepository.save(specialty);

        SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
        createDTO.setName("Cardiology");

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> specialtyService.create(createDTO));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
    void create_AsDoctor_ShouldBeDenied_ErrorCase() {
        // ARRANGE
        SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
        createDTO.setName("Unauthorized Specialty");

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> specialtyService.create(createDTO));
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void update_AsAdminWithValidData_ShouldUpdateSpecialty_HappyPath() {
        // ARRANGE
        Specialty specialty = new Specialty();
        specialty.setName("Old Name");
        specialty = specialtyRepository.save(specialty);

        SpecialtyUpdateDTO updateDTO = new SpecialtyUpdateDTO(specialty.getId(), "New Name", "New Desc");

        // ACT
        specialtyService.update(updateDTO);

        // ASSERT
        Specialty updatedSpecialty = specialtyRepository.findById(specialty.getId()).get();
        assertEquals("New Name", updatedSpecialty.getName());
        assertEquals("New Desc", updatedSpecialty.getDescription());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void delete_WithUnusedSpecialty_ShouldSucceed_HappyPath() {
        // ARRANGE
        Specialty specialty = new Specialty();
        specialty.setName("To Be Deleted");
        specialty = specialtyRepository.save(specialty);
        long specialtyId = specialty.getId();

        // ACT
        specialtyService.delete(specialtyId);

        // ASSERT
        assertFalse(specialtyRepository.findById(specialtyId).isPresent());
    }

    @Test
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    void delete_WithSpecialtyInUse_ShouldThrowException_ErrorCase() {
        // ARRANGE
        Specialty specialty = new Specialty();
        specialty.setName("Cardiology");
        specialty = specialtyRepository.save(specialty);

        Doctor doctor = new Doctor();
        doctor.setName("Dr. House");
        doctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setSpecialties(Set.of(specialty));
        doctorRepository.save(doctor);

        long specialtyId = specialty.getId();

        // ACT & ASSERT
        assertThrows(EntityInUseException.class, () -> specialtyService.delete(specialtyId));
    }
}
