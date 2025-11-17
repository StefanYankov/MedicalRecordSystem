package nbu.cscb869.web.controllers.doctor.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidInputException;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisitSchedulingController.class)
@Import(GlobalExceptionHandler.class)
class VisitSchedulingControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitService visitService;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private PatientService patientService;

    @Nested
    @DisplayName("Schedule Visit")
    class ScheduleVisitTests {
        @Test
        void showScheduleVisitForm_ShouldReturnScheduleView_HappyPath() throws Exception {
            when(doctorService.getById(anyLong())).thenReturn(new DoctorViewDTO());

            mockMvc.perform(get("/visits/schedule/1").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("visits/schedule"))
                    .andExpect(model().attributeExists("doctor", "visitData"));
        }

        @Test
        void showScheduleVisitForm_WithDate_ShouldReturnTimeSlots_HappyPath() throws Exception {
            when(doctorService.getById(anyLong())).thenReturn(new DoctorViewDTO());
            when(visitService.getVisitsByDoctorAndStatusAndDateRange(anyLong(), any(VisitStatus.class), any(LocalDate.class), any(LocalDate.class), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(Collections.singletonList(new VisitViewDTO())));

            mockMvc.perform(get("/visits/schedule/1").param("date", LocalDate.now().toString()).with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("visits/schedule"))
                    .andExpect(model().attributeExists("doctor", "visitData", "timeSlots", "selectedDate"));
        }

        @Test
        void processScheduleVisit_ShouldCreateVisitAndRedirect_HappyPath() throws Exception {
            OidcUser mockOidcUser = mock(OidcUser.class);
            when(mockOidcUser.getSubject()).thenReturn("test-user-subject-id");
            when(mockOidcUser.getName()).thenReturn("test-user");

            when(patientService.getByKeycloakId(anyString())).thenReturn(new PatientViewDTO());
            when(visitService.scheduleNewVisitByPatient(any(VisitCreateDTO.class))).thenReturn(new VisitViewDTO());

            mockMvc.perform(post("/visits/schedule")
                            .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_PATIENT")).oidcUser(mockOidcUser))
                            .flashAttr("visitData", new VisitCreateDTO())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/visits/*"));

            verify(visitService).scheduleNewVisitByPatient(any(VisitCreateDTO.class));
        }

        @Test
        void processScheduleVisit_WhenSlotIsAlreadyBooked_ShouldReturnFormWithError_ErrorCase() throws Exception {
            OidcUser mockOidcUser = mock(OidcUser.class);
            when(mockOidcUser.getSubject()).thenReturn("test-user-subject-id");
            when(mockOidcUser.getName()).thenReturn("test-user");

            when(patientService.getByKeycloakId(anyString())).thenReturn(new PatientViewDTO());
            when(visitService.scheduleNewVisitByPatient(any(VisitCreateDTO.class)))
                    .thenThrow(new InvalidInputException("The selected time slot is no longer available."));

            mockMvc.perform(post("/visits/schedule")
                            .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_PATIENT")).oidcUser(mockOidcUser))
                            .flashAttr("visitData", new VisitCreateDTO())
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/visits/schedule/*"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Visit Details")
    class VisitDetailsTests {
        @Test
        void getVisitDetails_ShouldReturnDetailsView_HappyPath() throws Exception {
            // ARRANGE
            DoctorViewDTO doctor = new DoctorViewDTO();
            doctor.setName("Dr. Test");
            PatientViewDTO patient = new PatientViewDTO();
            patient.setName("Test Patient");

            VisitViewDTO visit = new VisitViewDTO();
            visit.setDoctor(doctor);
            visit.setPatient(patient);

            when(visitService.getById(anyLong())).thenReturn(visit);

            // ACT & ASSERT
            mockMvc.perform(get("/visits/1").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("visits/details"))
                    .andExpect(model().attributeExists("visit"));
        }

        @Test
        void getVisitDetails_ForNonExistentVisit_ShouldReturnNotFound_ErrorCase() throws Exception {
            when(visitService.getById(anyLong())).thenThrow(new EntityNotFoundException("Visit not found"));

            mockMvc.perform(get("/visits/999").with(oidcLogin()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Cancel Visit")
    class CancelVisitTests {
        @Test
        void cancelVisit_ShouldCancelAndRedirect_HappyPath() throws Exception {
            doNothing().when(visitService).cancelVisit(anyLong());

            mockMvc.perform(post("/visits/1/cancel").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile/history?visitCancelled=true"));

            verify(visitService).cancelVisit(1L);
        }

        @Test
        void cancelVisit_WhenVisitNotFound_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Visit not found")).when(visitService).cancelVisit(99L);

            mockMvc.perform(post("/visits/99/cancel").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
