package nbu.cscb869.web.controllers.admin.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.SickLeaveService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.controllers.GlobalExceptionHandler;
import nbu.cscb869.web.controllers.admin.AdminSickLeaveController;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AdminSickLeaveControllerUnitTests {

    @Mock
    private SickLeaveService sickLeaveService;

    @Mock
    private VisitService visitService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AdminSickLeaveController adminSickLeaveController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminSickLeaveController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("List Sick Leaves")
    class ListSickLeavesTests {
        @Test
        void listSickLeaves_ShouldReturnListViewWithSickLeaves_HappyPath() throws Exception {
            Page<SickLeaveViewDTO> sickLeavePage = new PageImpl<>(Collections.singletonList(new SickLeaveViewDTO()));
            when(sickLeaveService.getAll(anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(CompletableFuture.completedFuture(sickLeavePage));

            mockMvc.perform(get("/admin/sick-leaves"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/sick-leaves/list"))
                    .andExpect(model().attributeExists("sickLeaves"));

            verify(sickLeaveService).getAll(0, 10, "startDate", false);
        }

        @Test
        void listSickLeaves_WhenServiceReturnsEmptyPage_ShouldDisplayNoSickLeavesMessage_EdgeCase() throws Exception {
            Page<SickLeaveViewDTO> emptyPage = Page.empty();
            when(sickLeaveService.getAll(anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenReturn(CompletableFuture.completedFuture(emptyPage));

            mockMvc.perform(get("/admin/sick-leaves"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/sick-leaves/list"))
                    .andExpect(model().attribute("sickLeaves", emptyPage));
        }

        @Test
        void listSickLeaves_WhenServiceFails_ShouldReturnErrorView_ErrorCase() throws Exception {
            when(sickLeaveService.getAll(anyInt(), anyInt(), anyString(), anyBoolean()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            mockMvc.perform(get("/admin/sick-leaves"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Create Sick Leave")
    class CreateSickLeaveTests {
        @Test
        void showCreateSickLeaveForm_ShouldReturnCreateView_HappyPath() throws Exception {
            when(visitService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull()))
                    .thenReturn(CompletableFuture.completedFuture(Page.empty()));

            mockMvc.perform(get("/admin/sick-leaves/create"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/sick-leaves/create"))
                    .andExpect(model().attributeExists("sickLeave", "visits"));
        }

        @Test
        void createSickLeave_WithValidData_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            when(sickLeaveService.create(any(SickLeaveCreateDTO.class))).thenReturn(new SickLeaveViewDTO());

            mockMvc.perform(post("/admin/sick-leaves/create")
                            .param("startDate", LocalDate.now().toString())
                            .param("durationDays", "5")
                            .param("visitId", "1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/sick-leaves"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(sickLeaveService).create(any(SickLeaveCreateDTO.class));
        }

        @Test
        void createSickLeave_WithInvalidData_ShouldRedirectBackToFormAndShowErrors_ErrorCase() throws Exception {
            mockMvc.perform(post("/admin/sick-leaves/create").param("durationDays", "0")) // Invalid duration
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/sick-leaves/create"))
                    .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.sickLeave"));

            verify(sickLeaveService, never()).create(any(SickLeaveCreateDTO.class));
        }
    }

    @Nested
    @DisplayName("Edit Sick Leave")
    class EditSickLeaveTests {
        @Test
        void showEditSickLeaveForm_ShouldReturnEditView_HappyPath() throws Exception {
            when(sickLeaveService.getById(anyLong())).thenReturn(new SickLeaveViewDTO());
            when(modelMapper.map(any(SickLeaveViewDTO.class), eq(SickLeaveUpdateDTO.class))).thenReturn(new SickLeaveUpdateDTO());
            when(visitService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull()))
                    .thenReturn(CompletableFuture.completedFuture(Page.empty()));

            mockMvc.perform(get("/admin/sick-leaves/edit/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/sick-leaves/edit"))
                    .andExpect(model().attributeExists("sickLeave", "visits"));
        }

        @Test
        void showEditSickLeaveForm_ForNonExistentSickLeave_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Sick leave not found")).when(sickLeaveService).getById(anyLong());

            mockMvc.perform(get("/admin/sick-leaves/edit/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void editSickLeave_WithValidData_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            when(sickLeaveService.update(any(SickLeaveUpdateDTO.class))).thenReturn(new SickLeaveViewDTO());

            mockMvc.perform(post("/admin/sick-leaves/edit/1")
                            .param("id", "1")
                            .param("startDate", LocalDate.now().toString())
                            .param("durationDays", "10")
                            .param("visitId", "1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/sick-leaves"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(sickLeaveService).update(any(SickLeaveUpdateDTO.class));
        }
    }

    @Nested
    @DisplayName("Delete Sick Leave")
    class DeleteSickLeaveTests {
        @Test
        void showDeleteConfirmation_ShouldReturnConfirmationView_HappyPath() throws Exception {
            when(sickLeaveService.getById(anyLong())).thenReturn(new SickLeaveViewDTO());

            mockMvc.perform(get("/admin/sick-leaves/delete/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/sick-leaves/delete-confirm"))
                    .andExpect(model().attributeExists("sickLeave"));
        }

        @Test
        void showDeleteConfirmation_ForNonExistentSickLeave_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Sick leave not found")).when(sickLeaveService).getById(anyLong());

            mockMvc.perform(get("/admin/sick-leaves/delete/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteSickLeaveConfirmed_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            doNothing().when(sickLeaveService).delete(anyLong());

            mockMvc.perform(post("/admin/sick-leaves/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/sick-leaves"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(sickLeaveService).delete(1L);
        }

        @Test
        void deleteSickLeaveConfirmed_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            doThrow(new InvalidDTOException("Cannot delete")).when(sickLeaveService).delete(anyLong());

            mockMvc.perform(post("/admin/sick-leaves/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/sick-leaves"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }
}
