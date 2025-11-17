package nbu.cscb869.web.api.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.services.utility.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(DoctorApiControllerIntegrationTests.TestConfig.class)
class DoctorApiControllerIntegrationTests {

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

    @MockBean
    private CloudinaryService cloudinaryService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Doctor testDoctor;

    @BeforeEach
    void setUp() {
        doctorRepository.deleteAll();
        testDoctor = new Doctor();
        testDoctor.setName("Dr. House");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setApproved(true);
        doctorRepository.save(testDoctor);
    }

    @Nested
    @DisplayName("GET /api/doctors")
    class GetTests {
        @Test
        @WithMockKeycloakUser
        void getAllDoctors_AsAuthenticatedUser_ShouldReturnDoctors_HappyPath() throws Exception {
            mockMvc.perform(get("/api/doctors"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content[0].name").value("Dr. House"));
        }

        @Test
        void getAllDoctors_AsUnauthenticatedUser_ShouldReturnUnauthorized_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/doctors"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockKeycloakUser
        void getDoctorById_WithValidId_ShouldReturnDoctor_HappyPath() throws Exception {
            mockMvc.perform(get("/api/doctors/{id}", testDoctor.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testDoctor.getId()))
                    .andExpect(jsonPath("$.name").value("Dr. House"));
        }

        @Test
        @WithMockKeycloakUser
        void getDoctorById_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/doctors/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/doctors/unapproved")
    class ApprovalWorkflowTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void getUnapprovedDoctors_AsAdmin_ShouldReturnOnlyUnapprovedDoctors_HappyPath() throws Exception {
            Doctor unapprovedDoctor = new Doctor();
            unapprovedDoctor.setName("Dr. Pending");
            unapprovedDoctor.setApproved(false);
            unapprovedDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            unapprovedDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
            doctorRepository.save(unapprovedDoctor);

            mockMvc.perform(get("/api/doctors/unapproved"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Dr. Pending"));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void getUnapprovedDoctors_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/doctors/unapproved"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/doctors")
    class PostTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void createDoctor_AsAdminWithValidData_ShouldReturnCreated_HappyPath() throws Exception {
            DoctorCreateDTO newDoctor = new DoctorCreateDTO();
            newDoctor.setName("Dr. New");
            newDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            newDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());

            mockMvc.perform(post("/api/doctors")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newDoctor)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Dr. New"));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void createDoctor_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            DoctorCreateDTO newDoctor = new DoctorCreateDTO();
            newDoctor.setName("Dr. Unauthorized");
            newDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());

            mockMvc.perform(post("/api/doctors")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newDoctor)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void createDoctor_WithInvalidData_ShouldReturnBadRequest_ErrorCase() throws Exception {
            DoctorCreateDTO newDoctor = new DoctorCreateDTO(); // Missing required fields
            newDoctor.setName(null);

            mockMvc.perform(post("/api/doctors")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newDoctor)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/doctors/{id}")
    class PatchTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void updateDoctorPartially_AsAdminWithValidData_ShouldReturnOk_HappyPath() throws Exception {
            DoctorUpdateDTO update = new DoctorUpdateDTO();
            update.setName("Dr. Gregory House");

            mockMvc.perform(patch("/api/doctors/{id}", testDoctor.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Dr. Gregory House"));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void updateDoctorPartially_WithOnlyOneField_ShouldNotUpdateOtherFields_EdgeCase() throws Exception {
            String originalUniqueId = testDoctor.getUniqueIdNumber();
            DoctorUpdateDTO update = new DoctorUpdateDTO();
            update.setName("Dr. Name Change Only");

            mockMvc.perform(patch("/api/doctors/{id}", testDoctor.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Dr. Name Change Only"))
                    .andExpect(jsonPath("$.uniqueIdNumber").value(originalUniqueId));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
        void updateDoctorPartially_AsPatient_ShouldReturnForbidden_ErrorCase() throws Exception {
            DoctorUpdateDTO update = new DoctorUpdateDTO();
            update.setName("Attempted Update");

            mockMvc.perform(patch("/api/doctors/{id}", testDoctor.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/doctors/{id}")
    class DeleteTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void deleteDoctor_AsAdmin_ShouldReturnNoContent_HappyPath() throws Exception {
            mockMvc.perform(delete("/api/doctors/{id}", testDoctor.getId()).with(csrf()))
                    .andExpect(status().isNoContent());

            assertFalse(doctorRepository.findById(testDoctor.getId()).isPresent());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void deleteDoctor_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(delete("/api/doctors/{id}", testDoctor.getId()).with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        void deleteDoctorImage_AsAdmin_ShouldSucceed_HappyPath() throws Exception {
            doNothing().when(cloudinaryService).deleteImage(any());
            mockMvc.perform(delete("/api/doctors/{id}/image", testDoctor.getId()).with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }
}
