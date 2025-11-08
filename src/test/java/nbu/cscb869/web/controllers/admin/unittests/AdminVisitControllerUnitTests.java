package nbu.cscb869.web.controllers.admin.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitUpdateDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.controllers.GlobalExceptionHandler;
import nbu.cscb869.web.controllers.admin.AdminVisitController;
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
import org.springframework.validation.BindingResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AdminVisitControllerUnitTests {

    @Mock
    private VisitService visitService;

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private DiagnosisService diagnosisService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AdminVisitController adminVisitController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminVisitController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("List Visits")
    class ListVisitsTests {
        @Test
        void listVisits_ShouldReturnListViewWithVisits_HappyPath() throws Exception {
            Page<VisitViewDTO> visitPage = new PageImpl<>(Collections.singletonList(new VisitViewDTO()));
            when(visitService.getAll(eq(0), eq(10), eq("visitDate"), eq(false), eq("")))
                    .thenReturn(CompletableFuture.completedFuture(visitPage));

            mockMvc.perform(get("/admin/visits").param("filter", "")) // Pass empty string for filter
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/visits/list"))
                    .andExpect(model().attributeExists("visits", "filter"));

            verify(visitService).getAll(0, 10, "visitDate", false, "");
        }

        @Test
        void listVisits_WithFilter_ShouldReturnFilteredListView_HappyPath() throws Exception {
            Page<VisitViewDTO> visitPage = new PageImpl<>(Collections.singletonList(new VisitViewDTO()));
            when(visitService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), eq("testFilter")))
                    .thenReturn(CompletableFuture.completedFuture(visitPage));

            mockMvc.perform(get("/admin/visits").param("filter", "testFilter"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/visits/list"))
                    .andExpect(model().attributeExists("visits", "filter"));

            verify(visitService).getAll(0, 10, "visitDate", false, "testFilter");
        }
    }

    @Nested
    @DisplayName("Edit Visit")
    class EditVisitTests {
        @Test
        void showEditVisitForm_ShouldReturnEditView_HappyPath() throws Exception {
            VisitViewDTO visitViewDTO = new VisitViewDTO();
            visitViewDTO.setId(1L);
            visitViewDTO.setVisitDate(LocalDate.now());
            visitViewDTO.setVisitTime(LocalTime.of(10, 0)); // Valid time
            visitViewDTO.setStatus(VisitStatus.SCHEDULED);
            visitViewDTO.setPatient(new PatientViewDTO());
            visitViewDTO.setDoctor(new DoctorViewDTO());
            visitViewDTO.setDiagnosis(new DiagnosisViewDTO());

            when(visitService.getById(anyLong())).thenReturn(visitViewDTO);
            when(modelMapper.map(any(VisitViewDTO.class), eq(VisitUpdateDTO.class))).thenReturn(new VisitUpdateDTO());
            when(patientService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(Page.empty()));
            when(doctorService.getAllAsync(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(Page.empty()));
            when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(Page.empty()));

            mockMvc.perform(get("/admin/visits/edit/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/visits/edit"))
                    .andExpect(model().attributeExists("visit", "patients", "doctors", "diagnoses", "statuses"));
        }

        @Test
        void showEditVisitForm_ForNonExistentVisit_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Visit not found")).when(visitService).getById(anyLong());

            mockMvc.perform(get("/admin/visits/edit/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void editVisit_WithValidData_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            when(visitService.update(any(VisitUpdateDTO.class))).thenReturn(new VisitViewDTO());

            mockMvc.perform(post("/admin/visits/edit/1")
                            .param("id", "1")
                            .param("visitDate", LocalDate.now().toString())
                            .param("visitTime", "10:00") // Valid time
                            .param("patientId", "1")
                            .param("doctorId", "1")
                            .param("diagnosisId", "1")
                            .param("status", VisitStatus.SCHEDULED.name()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/visits"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(visitService).update(any(VisitUpdateDTO.class));
        }

        @Test
        void editVisit_WithInvalidData_ShouldRedirectBackToFormAndShowErrors_ErrorCase() throws Exception {
            mockMvc.perform(post("/admin/visits/edit/1")
                            .param("id", "1")
                            .param("visitDate", "invalid-date") // Invalid date format
                            .param("visitTime", "10:00") // Valid time to avoid multiple errors
                            .param("patientId", "1")
                            .param("doctorId", "1")
                            .param("diagnosisId", "1")
                            .param("status", VisitStatus.SCHEDULED.name()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/visits/edit/1"))
                    .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.visit"));

            verify(visitService, never()).update(any(VisitUpdateDTO.class));
        }

        @Test
        void editVisit_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            doThrow(new RuntimeException("Update error")).when(visitService).update(any(VisitUpdateDTO.class));

            mockMvc.perform(post("/admin/visits/edit/1")
                            .param("id", "1")
                            .param("visitDate", LocalDate.now().toString())
                            .param("visitTime", "10:00") // Valid time
                            .param("patientId", "1")
                            .param("doctorId", "1")
                            .param("diagnosisId", "1")
                            .param("status", VisitStatus.SCHEDULED.name()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/visits"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Delete Visit")
    class DeleteVisitTests {
        @Test
        void showDeleteConfirmation_ShouldReturnConfirmationView_HappyPath() throws Exception {
            VisitViewDTO visitViewDTO = new VisitViewDTO();
            visitViewDTO.setId(1L);
            visitViewDTO.setPatient(new PatientViewDTO());
            visitViewDTO.setDoctor(new DoctorViewDTO());
            visitViewDTO.setVisitDate(LocalDate.now());
            visitViewDTO.setVisitTime(LocalTime.now());

            when(visitService.getById(anyLong())).thenReturn(visitViewDTO);

            mockMvc.perform(get("/admin/visits/delete/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/visits/delete-confirm"))
                    .andExpect(model().attributeExists("visit"));
        }

        @Test
        void showDeleteConfirmation_ForNonExistentVisit_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Visit not found")).when(visitService).getById(anyLong());

            mockMvc.perform(get("/admin/visits/delete/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteVisitConfirmed_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            doNothing().when(visitService).delete(anyLong());

            mockMvc.perform(post("/admin/visits/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/visits"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(visitService).delete(1L);
        }

        @Test
        void deleteVisitConfirmed_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            doThrow(new RuntimeException("Delete error")).when(visitService).delete(anyLong());

            mockMvc.perform(post("/admin/visits/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/visits"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }
}
