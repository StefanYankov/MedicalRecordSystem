package nbu.cscb869.web.controllers.admin.integrationtests;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(AdminDoctorControllerIntegrationTests.TestConfig.class)
class AdminDoctorControllerIntegrationTests {

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

    private Doctor testDoctor;

    @BeforeEach
    void setUp() {
        doctorRepository.deleteAll();
        testDoctor = new Doctor();
        testDoctor.setName("Dr. House");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setGeneralPractitioner(false);
        testDoctor.setApproved(true); // Default to approved for most tests
        doctorRepository.save(testDoctor);
    }

    @Nested
    @DisplayName("List Doctors")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class ListTests {
        @Test
        void listDoctors_ShouldShowDoctorInList_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/doctors"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/doctors/list"))
                    .andExpect(content().string(containsString("Dr. House")));
        }
    }

    @Nested
    @DisplayName("Edit Doctor")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class EditTests {
        @Test
        void showEditDoctorForm_ShouldDisplayCorrectDoctor_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/doctors/edit/{id}", testDoctor.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/doctors/edit"))
                    .andExpect(model().attributeExists("doctor"))
                    .andExpect(content().string(containsString("Dr. House")));
        }

        @Test
        void editDoctor_WithValidData_ShouldUpdateDoctorAndRedirect_HappyPath() throws Exception {
            when(cloudinaryService.uploadImage(any())).thenReturn(CompletableFuture.completedFuture("http://mock.url/image.jpg"));

            MockMultipartFile imageFile = new MockMultipartFile("imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image".getBytes());

            mockMvc.perform(multipart("/admin/doctors/edit/{id}", testDoctor.getId())
                            .file(imageFile)
                            .param("name", "Dr. Gregory House")
                            .param("isGeneralPractitioner", "true")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors"))
                    .andExpect(flash().attribute("successMessage", "Doctor updated successfully."));
        }
    }

    @Nested
    @DisplayName("Delete Doctor")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class DeleteTests {
        @Test
        void showDeleteConfirmation_ShouldDisplayConfirmationPage_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/doctors/delete/{id}", testDoctor.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/doctors/delete-confirm"))
                    .andExpect(content().string(containsString("Are you sure you want to delete this doctor?")))
                    .andExpect(content().string(containsString("Dr. House")));
        }

        @Test
        void deleteDoctorConfirmed_ShouldDeleteDoctorAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/doctors/delete/{id}", testDoctor.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors"))
                    .andExpect(flash().attribute("successMessage", "Doctor deleted successfully."));

            assertFalse(doctorRepository.findById(testDoctor.getId()).isPresent());
        }

        @Test
        void showDeleteConfirmation_ForNonExistentDoctor_ShouldReturnNotFound_ErrorCase() throws Exception {
            mockMvc.perform(get("/admin/doctors/delete/9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Approval Workflow")
    @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
    class ApprovalTests {
        private Doctor unapprovedDoctor;

        @BeforeEach
        void setUp() {
            unapprovedDoctor = new Doctor();
            unapprovedDoctor.setName("Dr. Pending");
            unapprovedDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            unapprovedDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
            unapprovedDoctor.setApproved(false);
            doctorRepository.save(unapprovedDoctor);
        }

        @Test
        void listUnapprovedDoctors_ShouldReturnUnapprovedDoctors_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/doctors/unapproved"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/doctors/unapproved-list"))
                    .andExpect(model().attributeExists("doctors"))
                    .andExpect(content().string(containsString("Dr. Pending")))
                    .andExpect(content().string(not(containsString("Dr. House"))));
        }

        @Test
        void approveDoctor_ShouldSetFlagAndRedirect_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/doctors/{id}/approve", unapprovedDoctor.getId()).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors/unapproved"))
                    .andExpect(flash().attribute("successMessage", "Doctor approved successfully."));

            assertTrue(doctorRepository.findById(unapprovedDoctor.getId()).get().isApproved());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void listDoctors_AsDoctor_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/admin/doctors"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        void approveDoctor_AsDoctor_ShouldBeForbidden() throws Exception {
            mockMvc.perform(post("/admin/doctors/{id}/approve", testDoctor.getId()).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
