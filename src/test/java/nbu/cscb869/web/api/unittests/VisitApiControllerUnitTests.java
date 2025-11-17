package nbu.cscb869.web.api.unittests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.data.dtos.VisitCreateDTO;
import nbu.cscb869.services.data.dtos.VisitUpdateDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.api.controllers.ApiGlobalExceptionHandler;
import nbu.cscb869.web.api.controllers.VisitApiController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VisitApiController.class)
@Import(ApiGlobalExceptionHandler.class)
class VisitApiControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VisitService visitService;

    private VisitCreateDTO createValidVisitCreateDTO() {
        VisitCreateDTO dto = new VisitCreateDTO();
        dto.setPatientId(1L);
        dto.setDoctorId(1L);
        dto.setDiagnosisId(1L);
        dto.setVisitDate(LocalDate.now());
        dto.setVisitTime(LocalTime.of(10, 0));
        return dto;
    }

    private VisitUpdateDTO createValidVisitUpdateDTO() {
        VisitUpdateDTO dto = new VisitUpdateDTO();
        dto.setId(1L);
        dto.setPatientId(1L);
        dto.setDoctorId(1L);
        dto.setVisitDate(LocalDate.now());
        dto.setVisitTime(LocalTime.of(10, 0));
        return dto;
    }

    @Nested
    @DisplayName("GET /api/visits")
    class GetAllVisitsTests {
        @Test
        void getAllVisits_AsAdmin_ShouldReturnPage_HappyPath() throws Exception {
            Page<VisitViewDTO> page = new PageImpl<>(Collections.singletonList(new VisitViewDTO()));
            when(visitService.getAll(anyInt(), anyInt(), anyString(), any(Boolean.class), any())).thenReturn(CompletableFuture.completedFuture(page));

            mockMvc.perform(get("/api/visits")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/visits")
    class CreateVisitTests {
        @Test
        void createVisit_WithValidDataAsDoctor_ShouldReturnCreated_HappyPath() throws Exception {
            VisitCreateDTO createDTO = createValidVisitCreateDTO();
            VisitViewDTO viewDTO = new VisitViewDTO();
            viewDTO.setId(1L);

            when(visitService.create(any(VisitCreateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(post("/api/visits")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PATCH /api/visits/{id}")
    class UpdateVisitTests {
        @Test
        void updateVisit_WithValidDataAsAdmin_ShouldReturnOk_HappyPath() throws Exception {
            VisitUpdateDTO updateDTO = createValidVisitUpdateDTO();
            VisitViewDTO viewDTO = new VisitViewDTO();
            viewDTO.setId(1L);

            when(visitService.update(any(VisitUpdateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(patch("/api/visits/1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        void updateVisit_WithInvalidData_ShouldReturnBadRequest_ErrorCase() throws Exception {
            VisitUpdateDTO invalidDto = new VisitUpdateDTO(); // is missing required fields

            mockMvc.perform(patch("/api/visits/1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateVisit_ForNonExistentVisit_ShouldReturnNotFound_ErrorCase() throws Exception {
            VisitUpdateDTO updateDTO = createValidVisitUpdateDTO();
            when(visitService.update(any(VisitUpdateDTO.class))).thenThrow(new EntityNotFoundException("Visit not found"));

            mockMvc.perform(patch("/api/visits/999")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/visits/{id}")
    class DeleteVisitTests {
        @Test
        void deleteVisit_AsAdmin_ShouldReturnNoContent_HappyPath() throws Exception {
            mockMvc.perform(delete("/api/visits/1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

    }
}