package nbu.cscb869.web.controllers.admin.unittests;

import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import nbu.cscb869.web.controllers.admin.AdminDoctorController;
import nbu.cscb869.web.viewmodels.DoctorEditViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminDoctorControllerUnitTests {

    @Mock
    private DoctorService doctorService;

    @Mock
    private SpecialtyService specialtyService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AdminDoctorController adminDoctorController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminDoctorController).build();
    }

    @Nested
    @DisplayName("List Doctors")
    class ListDoctorsTests {
        @Test
        void listDoctors_ShouldReturnListViewWithDoctors_HappyPath() throws Exception {
            Page<DoctorViewDTO> doctorPage = new PageImpl<>(Collections.singletonList(new DoctorViewDTO()));
            when(doctorService.getAllAsync(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(doctorPage));

            mockMvc.perform(get("/admin/doctors"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/doctors/list"))
                    .andExpect(model().attributeExists("doctors"));
        }
    }

    @Nested
    @DisplayName("Approval Workflow")
    class ApprovalWorkflowTests {
        @Test
        void listUnapprovedDoctors_WhenCalled_ShouldReturnUnapprovedListView_HappyPath() throws Exception {
            Page<DoctorViewDTO> unapprovedPage = new PageImpl<>(Collections.singletonList(new DoctorViewDTO()));
            when(doctorService.getUnapprovedDoctors(anyInt(), anyInt())).thenReturn(unapprovedPage);

            mockMvc.perform(get("/admin/doctors/unapproved"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/doctors/unapproved-list"))
                    .andExpect(model().attributeExists("doctors"));
        }

        @Test
        void approveDoctor_WithValidId_ShouldRedirectWithSuccessMessage_HappyPath() throws Exception {
            doNothing().when(doctorService).approveDoctor(1L);

            mockMvc.perform(post("/admin/doctors/1/approve"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors/unapproved"))
                    .andExpect(flash().attribute("successMessage", "Doctor approved successfully."));

            verify(doctorService).approveDoctor(1L);
        }

        @Test
        void approveDoctor_WhenServiceThrowsException_ShouldRedirectWithErrorMessage_ErrorCase() throws Exception {
            doThrow(new RuntimeException("Approval failed")).when(doctorService).approveDoctor(1L);

            mockMvc.perform(post("/admin/doctors/1/approve"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors/unapproved"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Edit Doctor")
    class EditDoctorTests {
        @Test
        void showEditDoctorForm_ShouldReturnEditView_HappyPath() throws Exception {
            when(doctorService.getById(anyLong())).thenReturn(new DoctorViewDTO());
            when(specialtyService.getAll(anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(Page.empty()));
            when(modelMapper.map(any(DoctorViewDTO.class), eq(DoctorEditViewModel.class))).thenReturn(new DoctorEditViewModel());

            mockMvc.perform(get("/admin/doctors/edit/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/doctors/edit"))
                    .andExpect(model().attributeExists("doctor", "assignedSpecialties", "availableSpecialties"));
        }

        @Test
        void editDoctor_WithValidData_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile("imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image".getBytes());

            mockMvc.perform(multipart("/admin/doctors/edit/1").file(imageFile)
                            .flashAttr("doctor", new DoctorUpdateDTO()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(doctorService).update(any(DoctorUpdateDTO.class), any());
        }

        @Test
        void editDoctor_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            doThrow(new RuntimeException("Update error")).when(doctorService).update(any(DoctorUpdateDTO.class), any());

            mockMvc.perform(multipart("/admin/doctors/edit/1").file(new MockMultipartFile("imageFile", new byte[0]))
                            .flashAttr("doctor", new DoctorUpdateDTO()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Delete Doctor")
    class DeleteDoctorTests {
        @Test
        void showDeleteConfirmation_ShouldReturnConfirmationView_HappyPath() throws Exception {
            when(doctorService.getById(anyLong())).thenReturn(new DoctorViewDTO());

            mockMvc.perform(get("/admin/doctors/delete/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/doctors/delete-confirm"))
                    .andExpect(model().attributeExists("doctor"));
        }

        @Test
        void deleteDoctorConfirmed_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/doctors/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(doctorService).delete(1L);
        }

        @Test
        void deleteDoctorConfirmed_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            doThrow(new RuntimeException("Delete error")).when(doctorService).delete(anyLong());

            mockMvc.perform(post("/admin/doctors/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Delete Doctor Image")
    class DeleteImageTests {
        @Test
        void deleteDoctorImage_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            mockMvc.perform(post("/admin/doctors/1/delete-image"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors/edit/1"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(doctorService).deleteDoctorImage(1L);
        }

        @Test
        void deleteDoctorImage_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            doThrow(new RuntimeException("Image delete error")).when(doctorService).deleteDoctorImage(anyLong());

            mockMvc.perform(post("/admin/doctors/1/delete-image"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/doctors/edit/1"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }
}
