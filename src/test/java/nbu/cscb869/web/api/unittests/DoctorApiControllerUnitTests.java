package nbu.cscb869.web.api.unittests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.data.dtos.DoctorCreateDTO;
import nbu.cscb869.services.data.dtos.DoctorUpdateDTO;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.web.api.controllers.DoctorApiController;
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
class DoctorApiControllerUnitTests {

    @Mock
    private DoctorService doctorService;

    @InjectMocks
    private DoctorApiController doctorApiController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(doctorApiController)
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("GET Endpoints")
    class GetEndpoints {
        @Test
        void getAllDoctors_ShouldReturnPageOfDoctors_HappyPath() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<DoctorViewDTO> doctorPage = new PageImpl<>(Collections.singletonList(new DoctorViewDTO()), pageable, 1);
            when(doctorService.getAllAsync(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(doctorPage));

            mockMvc.perform(get("/api/doctors"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getDoctorById_WithValidId_ShouldReturnDoctor_HappyPath() throws Exception {
            DoctorViewDTO doctor = new DoctorViewDTO();
            doctor.setId(1L);
            doctor.setName("Dr. Test");
            when(doctorService.getById(1L)).thenReturn(doctor);

            mockMvc.perform(get("/api/doctors/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Dr. Test"));
        }

        @Test
        void getDoctorById_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            when(doctorService.getById(99L)).thenThrow(new EntityNotFoundException("Doctor not found"));

            mockMvc.perform(get("/api/doctors/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST Endpoints")
    class PostEndpoints {
        @Test
        void createDoctor_WithValidData_ShouldReturnCreated_HappyPath() throws Exception {
            DoctorCreateDTO createDTO = new DoctorCreateDTO();
            createDTO.setName("Dr. New");
            createDTO.setUniqueIdNumber("VALID123");
            createDTO.setKeycloakId("some-keycloak-id");

            DoctorViewDTO viewDTO = new DoctorViewDTO();
            viewDTO.setId(1L);

            when(doctorService.createDoctor(any(DoctorCreateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(post("/api/doctors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        void createDoctor_WithInvalidData_ShouldReturnBadRequest_ErrorCase() throws Exception {
            DoctorCreateDTO createDTO = new DoctorCreateDTO();
            createDTO.setName(""); // Invalid blank name

            mockMvc.perform(post("/api/doctors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH Endpoints")
    class PatchEndpoints {
        @Test
        void updateDoctorPartially_WithValidData_ShouldReturnOk_HappyPath() throws Exception {
            DoctorUpdateDTO updateDTO = new DoctorUpdateDTO();
            updateDTO.setName("Dr. Updated");
            DoctorViewDTO viewDTO = new DoctorViewDTO();
            viewDTO.setName("Dr. Updated");

            when(doctorService.updateDoctor(any(DoctorUpdateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(patch("/api/doctors/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Dr. Updated"));
        }

        @Test
        void updateDoctorPartially_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            DoctorUpdateDTO updateDTO = new DoctorUpdateDTO();
            updateDTO.setName("Dr. Updated");

            when(doctorService.updateDoctor(any(DoctorUpdateDTO.class))).thenThrow(new EntityNotFoundException("Doctor not found"));

            mockMvc.perform(patch("/api/doctors/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE Endpoints")
    class DeleteEndpoints {
        @Test
        void deleteDoctor_WithValidId_ShouldReturnNoContent_HappyPath() throws Exception {
            doNothing().when(doctorService).delete(1L);

            mockMvc.perform(delete("/api/doctors/1"))
                    .andExpect(status().isNoContent());

            verify(doctorService, times(1)).delete(1L);
        }

        @Test
        void deleteDoctor_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Doctor not found")).when(doctorService).delete(99L);

            mockMvc.perform(delete("/api/doctors/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteDoctorImage_WithValidId_ShouldReturnNoContent_HappyPath() throws Exception {
            doNothing().when(doctorService).deleteDoctorImage(1L);

            mockMvc.perform(delete("/api/doctors/1/image"))
                    .andExpect(status().isNoContent());

            verify(doctorService, times(1)).deleteDoctorImage(1L);
        }

        @Test
        void deleteDoctorImage_WithInvalidId_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Doctor not found")).when(doctorService).deleteDoctorImage(99L);

            mockMvc.perform(delete("/api/doctors/99/image"))
                    .andExpect(status().isNotFound());
        }
    }
}
