package nbu.cscb869.web.controllers.doctor.unittests;

import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.data.dtos.VisitDocumentationDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.controllers.doctor.VisitDocumentationController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitDocumentationControllerUnitTests {

    @Mock
    private VisitService visitService;

    @Mock
    private DiagnosisService diagnosisService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private VisitDocumentationController visitDocumentationController;

    @Test
    void ShowDocumentVisitForm_WithValidId_ShouldPopulateModelAndReturnView_HappyPath() throws ExecutionException, InterruptedException {
        // Arrange
        Long visitId = 1L;
        VisitViewDTO visitViewDTO = new VisitViewDTO();
        Page<DiagnosisViewDTO> diagnosisPage = new PageImpl<>(Collections.singletonList(new DiagnosisViewDTO()));
        CompletableFuture<Page<DiagnosisViewDTO>> futureDiagnoses = CompletableFuture.completedFuture(diagnosisPage);

        when(visitService.getById(visitId)).thenReturn(visitViewDTO);
        when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), any())).thenReturn(futureDiagnoses);

        // Act
        String viewName = visitDocumentationController.showDocumentVisitForm(visitId, model);

        // Assert
        verify(visitService).getById(visitId);
        verify(diagnosisService).getAll(0, 100, "name", true, null);
        verify(model).addAttribute(eq("visit"), any(VisitViewDTO.class));
        verify(model).addAttribute(eq("documentation"), any(VisitDocumentationDTO.class));
        verify(model).addAttribute(eq("allDiagnoses"), any());
        assertEquals("visit/document", viewName);
    }

    @Test
    void DocumentVisit_WithValidData_ShouldCallServiceAndRedirect_HappyPath() throws ExecutionException, InterruptedException {
        // Arrange
        Long visitId = 1L;
        VisitDocumentationDTO documentationDTO = new VisitDocumentationDTO();
        when(bindingResult.hasErrors()).thenReturn(false);

        // Act
        String viewName = visitDocumentationController.documentVisit(visitId, documentationDTO, bindingResult, model);

        // Assert
        verify(visitService).documentVisit(visitId, documentationDTO);
        assertEquals("redirect:/schedule", viewName);
    }

    @Test
    void DocumentVisit_WithInvalidData_ShouldReturnFormView_ErrorCase() throws ExecutionException, InterruptedException {
        // Arrange
        Long visitId = 1L;
        VisitDocumentationDTO documentationDTO = new VisitDocumentationDTO();
        when(bindingResult.hasErrors()).thenReturn(true);

        // Mock the services that are called when repopulating the form
        when(visitService.getById(visitId)).thenReturn(new VisitViewDTO());
        Page<DiagnosisViewDTO> diagnosisPage = new PageImpl<>(Collections.emptyList());
        CompletableFuture<Page<DiagnosisViewDTO>> futureDiagnoses = CompletableFuture.completedFuture(diagnosisPage);
        when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), any())).thenReturn(futureDiagnoses);

        // Act
        String viewName = visitDocumentationController.documentVisit(visitId, documentationDTO, bindingResult, model);

        // Assert
        verify(visitService, never()).documentVisit(anyLong(), any());
        verify(model).addAttribute(eq("visit"), any(VisitViewDTO.class));
        verify(model).addAttribute(eq("allDiagnoses"), any());
        assertEquals("visit/document", viewName);
    }
}
