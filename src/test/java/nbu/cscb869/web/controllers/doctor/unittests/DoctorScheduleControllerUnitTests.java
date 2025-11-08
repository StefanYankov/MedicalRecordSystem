package nbu.cscb869.web.controllers.doctor.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.controllers.doctor.DoctorDashboardController;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DoctorScheduleControllerUnitTests {

    @Mock
    private DoctorService doctorService;

    @Mock
    private VisitService visitService;

    @InjectMocks
    private DoctorDashboardController doctorDashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(doctorDashboardController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("Doctor Dashboard Tests")
    class DoctorDashboardTests {

        private Authentication authentication;

        @BeforeEach
        void setupAuthentication() {
            authentication = new UsernamePasswordAuthenticationToken("doctor-id", null, Collections.emptyList());
        }

        @Test
        void doctorDashboard_AsAuthenticatedDoctor_ShouldReturnDashboardView_HappyPath() throws Exception {
            DoctorViewDTO doctor = new DoctorViewDTO();
            doctor.setName("Dr. Test");
            Page<nbu.cscb869.services.data.dtos.VisitViewDTO> visitsPage = new PageImpl<>(Collections.singletonList(new nbu.cscb869.services.data.dtos.VisitViewDTO()));

            when(doctorService.getByKeycloakId("doctor-id")).thenReturn(doctor);
            when(visitService.getVisitsByDoctorAndStatusAndDateRange(any(), any(VisitStatus.class), any(), any(), anyInt(), anyInt())).thenReturn(visitsPage);

            mockMvc.perform(get("/doctor/dashboard").principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/dashboard"))
                    .andExpect(model().attributeExists("doctorName", "visits"))
                    .andExpect(model().attribute("doctorName", "Dr. Test"));
        }

        @Test
        void doctorDashboard_WhenDoctorNotFound_ShouldThrowException_ErrorCase() throws Exception {
            when(doctorService.getByKeycloakId("doctor-id")).thenThrow(new EntityNotFoundException("Doctor not found"));

            mockMvc.perform(get("/doctor/dashboard").principal(authentication))
                    .andExpect(status().isNotFound());
        }

        @Test
        void doctorDashboard_WhenDoctorHasNoVisits_ShouldReturnEmptyPage_EdgeCase() throws Exception {
            DoctorViewDTO doctor = new DoctorViewDTO();
            doctor.setName("Dr. No-Visits");
            Page<nbu.cscb869.services.data.dtos.VisitViewDTO> emptyPage = Page.empty();

            when(doctorService.getByKeycloakId("doctor-id")).thenReturn(doctor);
            when(visitService.getVisitsByDoctorAndStatusAndDateRange(any(), any(VisitStatus.class), any(), any(), anyInt(), anyInt())).thenReturn(emptyPage);

            mockMvc.perform(get("/doctor/dashboard").principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/dashboard"))
                    .andExpect(model().attribute("visits", emptyPage));
        }
    }
}
