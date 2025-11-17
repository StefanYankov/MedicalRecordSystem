package nbu.cscb869.web.api.unittests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.config.SecurityConfig;
import nbu.cscb869.services.data.dtos.SickLeaveCreateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveUpdateDTO;
import nbu.cscb869.services.data.dtos.SickLeaveViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.SickLeaveService;
import nbu.cscb869.web.api.controllers.ApiGlobalExceptionHandler;
import nbu.cscb869.web.api.controllers.SickLeaveApiController;
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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SickLeaveApiController.class)
@Import({ApiGlobalExceptionHandler.class, SecurityConfig.class})
class SickLeaveApiControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SickLeaveService sickLeaveService;

    @MockBean
    private PatientService patientService;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Nested
    @DisplayName("GET /api/sick-leaves")
    class GetAllSickLeavesTests {
        @Test
        void getAllSickLeaves_AsAdmin_ShouldReturnPage_HappyPath() throws Exception {
            Page<SickLeaveViewDTO> page = new PageImpl<>(Collections.singletonList(new SickLeaveViewDTO()));
            when(sickLeaveService.getAll(anyInt(), anyInt(), anyString(), any(Boolean.class))).thenReturn(CompletableFuture.completedFuture(page));

            mockMvc.perform(get("/api/sick-leaves")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getAllSickLeaves_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/sick-leaves")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/sick-leaves")
    class CreateSickLeaveTests {
        @Test
        void createSickLeave_WithValidDataAsAdmin_ShouldReturnCreated_HappyPath() throws Exception {
            SickLeaveCreateDTO createDTO = new SickLeaveCreateDTO();
            createDTO.setVisitId(1L);
            createDTO.setStartDate(LocalDate.now());
            createDTO.setDurationDays(5);

            SickLeaveViewDTO viewDTO = new SickLeaveViewDTO();
            viewDTO.setId(1L);

            when(sickLeaveService.create(any(SickLeaveCreateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(post("/api/sick-leaves")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }

        @Test
        void createSickLeave_WithInvalidData_ShouldReturnBadRequest_ErrorCase() throws Exception {
            SickLeaveCreateDTO createDTO = new SickLeaveCreateDTO(); // Missing required fields
            createDTO.setDurationDays(0); // Invalid duration

            mockMvc.perform(post("/api/sick-leaves")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/sick-leaves/{id}")
    class UpdateSickLeaveTests {
        @Test
        void updateSickLeave_WithValidDataAsAdmin_ShouldReturnOk_HappyPath() throws Exception {
            SickLeaveUpdateDTO updateDTO = new SickLeaveUpdateDTO();
            updateDTO.setVisitId(1L);
            updateDTO.setStartDate(LocalDate.now());
            updateDTO.setDurationDays(10);

            SickLeaveViewDTO viewDTO = new SickLeaveViewDTO();
            viewDTO.setId(1L);

            when(sickLeaveService.update(any(SickLeaveUpdateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(patch("/api/sick-leaves/1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        void updateSickLeave_ForNonExistent_ShouldReturnNotFound_ErrorCase() throws Exception {
            SickLeaveUpdateDTO updateDTO = new SickLeaveUpdateDTO();
            updateDTO.setVisitId(1L);
            updateDTO.setStartDate(LocalDate.now());
            updateDTO.setDurationDays(10);

            when(sickLeaveService.update(any(SickLeaveUpdateDTO.class))).thenThrow(new EntityNotFoundException("Sick leave not found"));

            mockMvc.perform(patch("/api/sick-leaves/999")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/sick-leaves/{id}")
    class DeleteSickLeaveTests {
        @Test
        void deleteSickLeave_AsAdmin_ShouldReturnNoContent_HappyPath() throws Exception {
            mockMvc.perform(delete("/api/sick-leaves/1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        void deleteSickLeave_ForNonExistentId_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Sick Leave not found")).when(sickLeaveService).delete(anyLong());

            mockMvc.perform(delete("/api/sick-leaves/999")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
