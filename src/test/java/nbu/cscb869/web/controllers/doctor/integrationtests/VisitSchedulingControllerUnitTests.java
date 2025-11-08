package nbu.cscb869.web.controllers.doctor.integrationtests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitCreateDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.controllers.GlobalExceptionHandler;
import nbu.cscb869.web.controllers.VisitSchedulingController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class VisitSchedulingControllerUnitTests {

    @Mock
    private VisitService visitService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private PatientService patientService;

    @InjectMocks
    private VisitSchedulingController visitSchedulingController;

    private MockMvc mockMvc;

    @Mock
    private OidcUser mockOidcUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(visitSchedulingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("Schedule Visit")
    class ScheduleVisitTests {
        @Test
        void showScheduleVisitForm_ShouldReturnScheduleView_HappyPath() throws Exception {
            when(doctorService.getById(anyLong())).thenReturn(new DoctorViewDTO());

            mockMvc.perform(get("/visits/schedule/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("visits/schedule"))
                    .andExpect(model().attributeExists("doctor", "visitData"));
        }

        @Test
        void showScheduleVisitForm_WithDate_ShouldReturnTimeSlots_HappyPath() throws Exception {
            when(doctorService.getById(anyLong())).thenReturn(new DoctorViewDTO());
            when(visitService.getVisitsByDoctorAndStatusAndDateRange(anyLong(), any(VisitStatus.class), any(LocalDate.class), any(LocalDate.class), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(new VisitViewDTO())));

            mockMvc.perform(get("/visits/schedule/1").param("date", LocalDate.now().toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("visits/schedule"))
                    .andExpect(model().attributeExists("doctor", "visitData", "timeSlots", "selectedDate"));
        }

        @Test
        void processScheduleVisit_ShouldCreateVisitAndRedirect_HappyPath() throws Exception {
            when(patientService.getByKeycloakId(anyString())).thenReturn(new PatientViewDTO());
            when(visitService.scheduleNewVisitByPatient(any(VisitCreateDTO.class))).thenReturn(new VisitViewDTO());
            when(mockOidcUser.getSubject()).thenReturn("test-user");

            mockMvc.perform(post("/visits/schedule")
                            .with(oidcLogin().oidcUser(mockOidcUser))
                            .flashAttr("visitData", new VisitCreateDTO()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/visits/*"));

            verify(visitService).scheduleNewVisitByPatient(any(VisitCreateDTO.class));
        }
    }

    @Nested
    @DisplayName("Visit Details")
    class VisitDetailsTests {
        @Test
        void getVisitDetails_ShouldReturnDetailsView_HappyPath() throws Exception {
            when(visitService.getById(anyLong())).thenReturn(new VisitViewDTO());

            mockMvc.perform(get("/visits/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("visits/details"))
                    .andExpect(model().attributeExists("visit"));
        }

        @Test
        void getVisitDetails_ForNonExistentVisit_ShouldReturnNotFound_ErrorCase() throws Exception {
            when(visitService.getById(anyLong())).thenThrow(new EntityNotFoundException("Visit not found"));

            mockMvc.perform(get("/visits/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Cancel Visit")
    class CancelVisitTests {
        @Test
        void cancelVisit_ShouldCancelAndRedirect_HappyPath() throws Exception {
            doNothing().when(visitService).cancelVisit(anyLong());

            mockMvc.perform(post("/visits/1/cancel"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile/history?visitCancelled=true"));

            verify(visitService).cancelVisit(1L);
        }
    }
}
