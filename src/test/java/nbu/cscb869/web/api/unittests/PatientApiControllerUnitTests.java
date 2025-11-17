package nbu.cscb869.web.api.unittests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.web.api.controllers.PatientApiController;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PatientApiControllerUnitTests {

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientApiController patientApiController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder standaloneMockMvcBuilder = MockMvcBuilders.standaloneSetup(patientApiController);
        standaloneMockMvcBuilder.setControllerAdvice(new ApiGlobalExceptionHandler());
        standaloneMockMvcBuilder.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver());
        mockMvc = standaloneMockMvcBuilder
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("GET Endpoints")
    class GetEndpoints {
        @Test
        void getAllPatients_ShouldReturnPageOfPatients_HappyPath() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<PatientViewDTO> patientPage = new PageImpl<>(Collections.singletonList(new PatientViewDTO()), pageable, 1);
            when(patientService.findAll(any(), any())).thenReturn(patientPage);

            mockMvc.perform(get("/api/patients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getPatientById_WithValidId_ShouldReturnPatient_HappyPath() throws Exception {
            PatientViewDTO patient = new PatientViewDTO();
            patient.setId(1L);
            when(patientService.getById(1L)).thenReturn(patient);

            mockMvc.perform(get("/api/patients/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        void getPatientById_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            when(patientService.getById(99L)).thenThrow(new EntityNotFoundException("Patient not found"));

            mockMvc.perform(get("/api/patients/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST Endpoints")
    class PostEndpoints {
        @Test
        void createPatient_WithValidData_ShouldReturnCreated_HappyPath() throws Exception {
            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("John Doe");
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(1L);
            createDTO.setKeycloakId("some-keycloak-id");

            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setId(1L);

            when(patientService.create(any(PatientCreateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(post("/api/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated());
        }

        @Test
        void createPatient_WithInvalidData_ShouldReturnBadRequest_ErrorCase() throws Exception {
            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("");

            mockMvc.perform(post("/api/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void registerPatient_WithValidData_ShouldReturnCreated_HappyPath() throws Exception {
            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("New User");
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(1L);

            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setId(1L);

            when(patientService.registerPatient(any(PatientCreateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(post("/api/patients/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated());
        }

        @Test
        void updateInsuranceStatus_WithValidId_ShouldReturnOk_HappyPath() throws Exception {
            when(patientService.updateInsuranceStatus(1L)).thenReturn(new PatientViewDTO());

            mockMvc.perform(post("/api/patients/1/update-insurance"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH Endpoints")
    class PatchEndpoints {
        @Test
        void updatePatient_WithValidData_ShouldReturnOk_HappyPath() throws Exception {
            PatientUpdateDTO updateDTO = new PatientUpdateDTO();
            PatientViewDTO viewDTO = new PatientViewDTO();

            when(patientService.update(any(PatientUpdateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(patch("/api/patients/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk());
        }

        @Test
        void updatePatient_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            PatientUpdateDTO updateDTO = new PatientUpdateDTO();
            when(patientService.update(any(PatientUpdateDTO.class))).thenThrow(new EntityNotFoundException("Patient not found"));

            mockMvc.perform(patch("/api/patients/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE Endpoints")
    class DeleteEndpoints {
        @Test
        void deletePatient_WithValidId_ShouldReturnNoContent_HappyPath() throws Exception {
            doNothing().when(patientService).delete(1L);

            mockMvc.perform(delete("/api/patients/1"))
                    .andExpect(status().isNoContent());

            verify(patientService, times(1)).delete(1L);
        }

        @Test
        void deletePatient_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Patient not found")).when(patientService).delete(99L);

            mockMvc.perform(delete("/api/patients/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
