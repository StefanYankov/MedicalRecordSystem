package nbu.cscb869.web.api.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.services.data.dtos.SpecialtyCreateDTO;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(SpecialtyApiControllerIntegrationTests.TestConfig.class)
@DisplayName("SpecialtyApiController Integration Tests")
class SpecialtyApiControllerIntegrationTests {

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @MockBean
    private Keycloak keycloak;

    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setUp() {
        specialtyRepository.deleteAll();
    }

    @AfterEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void tearDown() {
        specialtyRepository.deleteAll();
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {
        @Test
        @DisplayName("createSpecialty_WithValidDataAsAdmin_ShouldCreateSpecialtyInDb")
        void createSpecialty_WithValidDataAsAdmin_ShouldCreateSpecialtyInDb() throws Exception {
            // ARRANGE
            SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
            createDTO.setName("Integration Test Specialty");
            createDTO.setDescription("A test specialty.");

            long specialtiesBefore = specialtyRepository.count();

            // ACT
            mockMvc.perform(post("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isCreated());

            // ASSERT
            long specialtiesAfter = specialtyRepository.count();
            assertThat(specialtiesAfter).isEqualTo(specialtiesBefore + 1);
            Specialty createdSpecialty = specialtyRepository.findAll().get(0);
            assertThat(createdSpecialty.getName()).isEqualTo("Integration Test Specialty");
        }

        @Test
        @DisplayName("getAllSpecialties_WhenSomeExist_ShouldReturnPageWithCorrectData")
        void getAllSpecialties_WhenSomeExist_ShouldReturnPageWithCorrectData() throws Exception {
            // ARRANGE
            specialtyRepository.save(new Specialty("Cardiology", "Heart-related issues", null));

            // ACT & ASSERT
            mockMvc.perform(get("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Cardiology"));
        }

        @Test
        @DisplayName("deleteSpecialty_WithExistingId_ShouldRemoveFromDb")
        void deleteSpecialty_WithExistingId_ShouldRemoveFromDb() throws Exception {
            // ARRANGE
            Specialty specialty = specialtyRepository.save(new Specialty("To be Deleted", "...", null));
            long specialtiesBefore = specialtyRepository.count();

            // ACT
            mockMvc.perform(delete("/api/specialties/" + specialty.getId())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            // ASSERT
            long specialtiesAfter = specialtyRepository.count();
            assertThat(specialtiesAfter).isEqualTo(specialtiesBefore - 1);
            assertThat(specialtyRepository.findById(specialty.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Error Case Tests")
    class ErrorCaseTests {
        @Test
        @DisplayName("createSpecialty_AsDoctor_ShouldReturnForbidden")
        void createSpecialty_AsDoctor_ShouldReturnForbidden() throws Exception {
            // ARRANGE
            SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
            createDTO.setName("Forbidden Specialty");

            // ACT & ASSERT
            mockMvc.perform(post("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("createSpecialty_WithDuplicateName_ShouldReturnConflict")
        void createSpecialty_WithDuplicateName_ShouldReturnConflict() throws Exception {
            // ARRANGE
            specialtyRepository.save(new Specialty("Cardiology", "Heart-related issues", null));
            SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
            createDTO.setName("Cardiology");

            // ACT & ASSERT
            mockMvc.perform(post("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deleteSpecialty_WithNonExistentId_ShouldReturnNotFound")
        void deleteSpecialty_WithNonExistentId_ShouldReturnNotFound() throws Exception {
            // ACT & ASSERT
            mockMvc.perform(delete("/api/specialties/999")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
