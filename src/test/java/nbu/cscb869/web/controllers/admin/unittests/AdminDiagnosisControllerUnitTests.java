package nbu.cscb869.web.controllers.admin.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.web.controllers.GlobalExceptionHandler;
import nbu.cscb869.web.controllers.admin.AdminDiagnosisController;
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

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AdminDiagnosisControllerUnitTests {

    @Mock
    private DiagnosisService diagnosisService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AdminDiagnosisController adminDiagnosisController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminDiagnosisController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("List Diagnoses")
    class ListDiagnosesTests {
        @Test
        void listDiagnoses_ShouldReturnListViewWithDiagnoses_HappyPath() throws Exception {
            Page<DiagnosisViewDTO> diagnosisPage = new PageImpl<>(Collections.singletonList(new DiagnosisViewDTO()));
            when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), any()))
                    .thenReturn(CompletableFuture.completedFuture(diagnosisPage));

            mockMvc.perform(get("/admin/diagnoses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/list"))
                    .andExpect(model().attributeExists("diagnoses"));

            verify(diagnosisService).getAll(0, 10, "name", true, null);
        }

        @Test
        void listDiagnoses_WithFilter_ShouldReturnFilteredListView_HappyPath() throws Exception {
            Page<DiagnosisViewDTO> diagnosisPage = new PageImpl<>(Collections.singletonList(new DiagnosisViewDTO()));
            when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), eq("testFilter")))
                    .thenReturn(CompletableFuture.completedFuture(diagnosisPage));

            mockMvc.perform(get("/admin/diagnoses").param("filter", "testFilter"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/list"))
                    .andExpect(model().attributeExists("diagnoses", "filter"));

            verify(diagnosisService).getAll(0, 10, "name", true, "testFilter");
        }

        @Test
        void listDiagnoses_WhenServiceReturnsEmptyPage_ShouldDisplayNoDiagnosesMessage_EdgeCase() throws Exception {
            Page<DiagnosisViewDTO> emptyPage = Page.empty();
            when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), any()))
                    .thenReturn(CompletableFuture.completedFuture(emptyPage));

            mockMvc.perform(get("/admin/diagnoses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/list"))
                    .andExpect(model().attribute("diagnoses", emptyPage));
        }

        @Test
        void listDiagnoses_WhenServiceFails_ShouldReturnErrorView_ErrorCase() throws Exception {
            when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), any()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            mockMvc.perform(get("/admin/diagnoses"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Create Diagnosis")
    class CreateDiagnosisTests {
        @Test
        void showCreateDiagnosisForm_ShouldReturnCreateView_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/diagnoses/create"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/create"))
                    .andExpect(model().attributeExists("diagnosis"));
        }

        @Test
        void createDiagnosis_WithValidData_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            when(diagnosisService.create(any(DiagnosisCreateDTO.class))).thenReturn(new DiagnosisViewDTO());

            mockMvc.perform(post("/admin/diagnoses/create")
                            .param("name", "Test Diagnosis")
                            .param("description", "Test Description"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(diagnosisService).create(any(DiagnosisCreateDTO.class));
        }

        @Test
        void createDiagnosis_WithInvalidData_ShouldRedirectBackToFormAndShowErrors_ErrorCase() throws Exception {
            mockMvc.perform(post("/admin/diagnoses/create").param("name", "")) // Empty name is invalid
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses/create"))
                    .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.diagnosis"));

            verify(diagnosisService, never()).create(any(DiagnosisCreateDTO.class));
        }

        @Test
        void createDiagnosis_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            when(diagnosisService.create(any(DiagnosisCreateDTO.class))).thenThrow(new InvalidDTOException("Diagnosis already exists"));

            mockMvc.perform(post("/admin/diagnoses/create")
                            .param("name", "Existing Diagnosis")
                            .param("description", "Test Description"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses/create"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Edit Diagnosis")
    class EditDiagnosisTests {
        @Test
        void showEditDiagnosisForm_ShouldReturnEditView_HappyPath() throws Exception {
            when(diagnosisService.getById(anyLong())).thenReturn(new DiagnosisViewDTO());
            when(modelMapper.map(any(DiagnosisViewDTO.class), eq(DiagnosisUpdateDTO.class))).thenReturn(new DiagnosisUpdateDTO());

            mockMvc.perform(get("/admin/diagnoses/edit/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/edit"))
                    .andExpect(model().attributeExists("diagnosis"));
        }

        @Test
        void showEditDiagnosisForm_ForNonExistentDiagnosis_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Diagnosis not found")).when(diagnosisService).getById(anyLong());

            mockMvc.perform(get("/admin/diagnoses/edit/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void editDiagnosis_WithValidData_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            when(diagnosisService.update(any(DiagnosisUpdateDTO.class))).thenReturn(new DiagnosisViewDTO());

            mockMvc.perform(post("/admin/diagnoses/edit/1")
                            .param("id", "1")
                            .param("name", "Updated Diagnosis")
                            .param("description", "Updated Description"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(diagnosisService).update(any(DiagnosisUpdateDTO.class));
        }

        @Test
        void editDiagnosis_WithInvalidData_ShouldRedirectBackToFormAndShowErrors_ErrorCase() throws Exception {
            mockMvc.perform(post("/admin/diagnoses/edit/1").param("name", "")) // Empty name is invalid
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses/edit/1"))
                    .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.diagnosis"));

            verify(diagnosisService, never()).update(any(DiagnosisUpdateDTO.class));
        }
    }

    @Nested
    @DisplayName("Delete Diagnosis")
    class DeleteDiagnosisTests {
        @Test
        void showDeleteConfirmation_ShouldReturnConfirmationView_HappyPath() throws Exception {
            when(diagnosisService.getById(anyLong())).thenReturn(new DiagnosisViewDTO());

            mockMvc.perform(get("/admin/diagnoses/delete/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/diagnoses/delete-confirm"))
                    .andExpect(model().attributeExists("diagnosis"));
        }

        @Test
        void showDeleteConfirmation_ForNonExistentDiagnosis_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Diagnosis not found")).when(diagnosisService).getById(anyLong());

            mockMvc.perform(get("/admin/diagnoses/delete/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteDiagnosisConfirmed_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            doNothing().when(diagnosisService).delete(anyLong());

            mockMvc.perform(post("/admin/diagnoses/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(diagnosisService).delete(1L);
        }

        @Test
        void deleteDiagnosisConfirmed_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            doThrow(new InvalidDTOException("Diagnosis in use")).when(diagnosisService).delete(anyLong());

            mockMvc.perform(post("/admin/diagnoses/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/diagnoses"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }
}
