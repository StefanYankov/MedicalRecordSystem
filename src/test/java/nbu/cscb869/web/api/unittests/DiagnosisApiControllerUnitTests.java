package nbu.cscb869.web.api.unittests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.web.api.controllers.DiagnosisApiController;
import nbu.cscb869.web.api.controllers.ApiGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DiagnosisApiControllerUnitTests {

    @Mock
    private DiagnosisService diagnosisService;

    @InjectMocks
    private DiagnosisApiController diagnosisApiController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(diagnosisApiController)
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("GET Endpoints")
    class GetEndpoints {
        @Test
        void getAllDiagnoses_ShouldReturnPageOfDiagnoses_HappyPath() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<DiagnosisViewDTO> diagnosisPage = new PageImpl<>(Collections.singletonList(new DiagnosisViewDTO()), pageable, 1);
            when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(diagnosisPage));

            mockMvc.perform(get("/api/diagnoses"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getDiagnosisById_WithValidId_ShouldReturnDiagnosis_HappyPath() throws Exception {
            DiagnosisViewDTO diagnosis = new DiagnosisViewDTO();
            diagnosis.setId(1L);
            diagnosis.setName("Flu");
            when(diagnosisService.getById(1L)).thenReturn(diagnosis);

            mockMvc.perform(get("/api/diagnoses/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Flu"));
        }

        @Test
        void getDiagnosisById_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            when(diagnosisService.getById(99L)).thenThrow(new EntityNotFoundException("Diagnosis not found"));

            mockMvc.perform(get("/api/diagnoses/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST Endpoints")
    class PostEndpoints {
        @Test
        void createDiagnosis_WithValidData_ShouldReturnCreated_HappyPath() throws Exception {
            DiagnosisCreateDTO createDTO = new DiagnosisCreateDTO();
            createDTO.setName("Common Cold");
            DiagnosisViewDTO viewDTO = new DiagnosisViewDTO();
            viewDTO.setId(1L);

            when(diagnosisService.create(any(DiagnosisCreateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(post("/api/diagnoses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        void createDiagnosis_WithInvalidData_ShouldReturnBadRequest_ErrorCase() throws Exception {
            DiagnosisCreateDTO createDTO = new DiagnosisCreateDTO();
            createDTO.setName(""); // Invalid blank name

            mockMvc.perform(post("/api/diagnoses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH Endpoints")
    class PatchEndpoints {
        @Test
        void updateDiagnosis_WithValidData_ShouldReturnOk_HappyPath() throws Exception {
            DiagnosisCreateDTO updateData = new DiagnosisCreateDTO();
            updateData.setName("Seasonal Flu");
            DiagnosisViewDTO viewDTO = new DiagnosisViewDTO();
            viewDTO.setName("Seasonal Flu");

            when(diagnosisService.update(any(DiagnosisUpdateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(patch("/api/diagnoses/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Seasonal Flu"));
        }

        @Test
        void updateDiagnosis_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            DiagnosisCreateDTO updateData = new DiagnosisCreateDTO();
            updateData.setName("Seasonal Flu");

            when(diagnosisService.update(any(DiagnosisUpdateDTO.class))).thenThrow(new EntityNotFoundException("Diagnosis not found"));

            mockMvc.perform(patch("/api/diagnoses/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE Endpoints")
    class DeleteEndpoints {
        @Test
        void deleteDiagnosis_WithValidId_ShouldReturnNoContent_HappyPath() throws Exception {
            doNothing().when(diagnosisService).delete(1L);

            mockMvc.perform(delete("/api/diagnoses/1"))
                    .andExpect(status().isNoContent());

            verify(diagnosisService, times(1)).delete(1L);
        }

        @Test
        void deleteDiagnosis_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Diagnosis not found")).when(diagnosisService).delete(99L);

            mockMvc.perform(delete("/api/diagnoses/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteDiagnosis_WhenDiagnosisIsInUse_ShouldReturnConflict_ErrorCase() throws Exception {
            doThrow(new InvalidDTOException("Diagnosis is in use")).when(diagnosisService).delete(1L);

            mockMvc.perform(delete("/api/diagnoses/1"))
                    .andExpect(status().isConflict());
        }
    }
}
