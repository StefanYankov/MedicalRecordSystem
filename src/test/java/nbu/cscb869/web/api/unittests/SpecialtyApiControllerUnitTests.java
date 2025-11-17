package nbu.cscb869.web.api.unittests;

import com.fasterxml.jackson.databind.ObjectMapper;
import nbu.cscb869.common.exceptions.EntityInUseException;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.config.SecurityConfig;
import nbu.cscb869.services.data.dtos.SpecialtyCreateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyUpdateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.SpecialtyService;
import nbu.cscb869.web.api.controllers.ApiGlobalExceptionHandler;
import nbu.cscb869.web.api.controllers.SpecialtyApiController;
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

@WebMvcTest(SpecialtyApiController.class)
@Import({ApiGlobalExceptionHandler.class, SecurityConfig.class})
class SpecialtyApiControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SpecialtyService specialtyService;

    @MockBean
    private PatientService patientService;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Nested
    @DisplayName("GET /api/specialties")
    class GetAllSpecialtiesTests {
        @Test
        void getAllSpecialties_AsAdmin_ShouldReturnPage_HappyPath() throws Exception {
            Page<SpecialtyViewDTO> page = new PageImpl<>(Collections.singletonList(new SpecialtyViewDTO()));
            when(specialtyService.getAll(anyInt(), anyInt(), anyString(), any(Boolean.class))).thenReturn(CompletableFuture.completedFuture(page));

            mockMvc.perform(get("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getAllSpecialties_AsDoctor_ShouldReturnForbidden_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getAllSpecialties_WhenNoneExist_ShouldReturnEmptyPage_EdgeCase() throws Exception {
            Page<SpecialtyViewDTO> emptyPage = new PageImpl<>(Collections.emptyList());
            when(specialtyService.getAll(anyInt(), anyInt(), anyString(), any(Boolean.class))).thenReturn(CompletableFuture.completedFuture(emptyPage));

            mockMvc.perform(get("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/specialties")
    class CreateSpecialtyTests {
        @Test
        void createSpecialty_WithValidDataAsAdmin_ShouldReturnCreated_HappyPath() throws Exception {
            SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
            createDTO.setName("Cardiology");

            SpecialtyViewDTO viewDTO = new SpecialtyViewDTO();
            viewDTO.setId(1L);
            viewDTO.setName("Cardiology");

            when(specialtyService.create(any(SpecialtyCreateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(post("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }

        @Test
        void createSpecialty_WithInvalidData_ShouldReturnBadRequest_ErrorCase() throws Exception {
            SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO(); // Name is null
            createDTO.setName(""); // or blank

            mockMvc.perform(post("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createSpecialty_WithDuplicateName_ShouldReturnConflict_ErrorCase() throws Exception {
            SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
            createDTO.setName("Cardiology");

            when(specialtyService.create(any(SpecialtyCreateDTO.class))).thenThrow(new InvalidDTOException("Specialty with this name already exists."));

            mockMvc.perform(post("/api/specialties")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PATCH /api/specialties/{id}")
    class UpdateSpecialtyTests {
        @Test
        void updateSpecialty_WithValidDataAsAdmin_ShouldReturnOk_HappyPath() throws Exception {
            SpecialtyUpdateDTO updateDTO = new SpecialtyUpdateDTO();
            updateDTO.setName("Updated Cardiology");

            SpecialtyViewDTO viewDTO = new SpecialtyViewDTO();
            viewDTO.setId(1L);
            viewDTO.setName("Updated Cardiology");

            when(specialtyService.update(any(SpecialtyUpdateDTO.class))).thenReturn(viewDTO);

            mockMvc.perform(patch("/api/specialties/1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        void updateSpecialty_ForNonExistent_ShouldReturnNotFound_ErrorCase() throws Exception {
            SpecialtyUpdateDTO updateDTO = new SpecialtyUpdateDTO();
            updateDTO.setName("Non-existent");

            when(specialtyService.update(any(SpecialtyUpdateDTO.class))).thenThrow(new EntityNotFoundException("Specialty not found"));

            mockMvc.perform(patch("/api/specialties/999")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/specialties/{id}")
    class DeleteSpecialtyTests {
        @Test
        void deleteSpecialty_AsAdmin_ShouldReturnNoContent_HappyPath() throws Exception {
            mockMvc.perform(delete("/api/specialties/1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        void deleteSpecialty_ForNonExistentId_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Specialty not found")).when(specialtyService).delete(anyLong());

            mockMvc.perform(delete("/api/specialties/999")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteSpecialty_WhenSpecialtyIsInUse_ShouldReturnConflict_ErrorCase() throws Exception {
            doThrow(new EntityInUseException("Specialty is in use by doctors")).when(specialtyService).delete(anyLong());

            mockMvc.perform(delete("/api/specialties/1")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }
}
