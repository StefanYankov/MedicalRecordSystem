package nbu.cscb869.web.api.unittests;

import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.api.controllers.MeApiController;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MeApiControllerUnitTests {

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private VisitService visitService;

    @InjectMocks
    private MeApiController meApiController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(meApiController)
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("GET /api/me/dashboard")
    class GetDashboardTests {
        @Test
        void getMyDashboard_AsDoctor_ShouldReturnDoctorDashboard_HappyPath() throws Exception {
            Authentication auth = new UsernamePasswordAuthenticationToken("doctor-id", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR")));
            DoctorViewDTO doctor = new DoctorViewDTO();
            doctor.setId(1L);
            Pageable pageable = PageRequest.of(0, 10);
            Page<VisitViewDTO> visitsPage = new PageImpl<>(Collections.singletonList(new VisitViewDTO()), pageable, 1);

            when(doctorService.getByKeycloakId("doctor-id")).thenReturn(doctor);
            when(visitService.getVisitsByDoctorAndStatusAndDateRange(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(visitsPage);

            mockMvc.perform(get("/api/me/dashboard").principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getMyDashboard_AsPatient_ShouldReturnPatientDashboard_HappyPath() throws Exception {
            Authentication auth = new UsernamePasswordAuthenticationToken("patient-id", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")));
            PatientViewDTO patient = new PatientViewDTO();
            patient.setId(1L);
            patient.setName("John Doe");
            when(patientService.getByKeycloakId("patient-id")).thenReturn(patient);

            mockMvc.perform(get("/api/me/dashboard").principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("John Doe"));
        }

        @Test
        void getMyDashboard_AsUnauthenticatedUser_ShouldReturnUnauthorized_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/me/dashboard"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void getMyDashboard_WhenServiceThrowsException_ShouldReturnError_ErrorCase() throws Exception {
            Authentication auth = new UsernamePasswordAuthenticationToken("patient-id", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")));
            when(patientService.getByKeycloakId("patient-id")).thenThrow(new RuntimeException("Service unavailable"));

            mockMvc.perform(get("/api/me/dashboard").principal(auth))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void getMyDashboard_AsPatientWithNoGp_ShouldReturnOk_EdgeCase() throws Exception {
            Authentication auth = new UsernamePasswordAuthenticationToken("patient-id", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")));
            PatientViewDTO patient = new PatientViewDTO();
            patient.setId(1L);
            patient.setName("John Doe");
            patient.setGeneralPractitionerId(null); // No GP assigned

            when(patientService.getByKeycloakId("patient-id")).thenReturn(patient);

            mockMvc.perform(get("/api/me/dashboard").principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.generalPractitionerId").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/me/history")
    class GetHistoryTests {
        @Test
        void getMyHistory_AsPatient_ShouldReturnVisitHistory_HappyPath() throws Exception {
            Authentication auth = new UsernamePasswordAuthenticationToken("patient-id", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")));
            PatientViewDTO patient = new PatientViewDTO();
            patient.setId(1L);
            Pageable pageable = PageRequest.of(0, 10);
            Page<VisitViewDTO> historyPage = new PageImpl<>(Collections.singletonList(new VisitViewDTO()), pageable, 1);

            when(patientService.getByKeycloakId("patient-id")).thenReturn(patient);
            when(visitService.getVisitsByPatient(anyLong(), anyInt(), anyInt())).thenReturn(historyPage);

            mockMvc.perform(get("/api/me/history").principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getMyHistory_WhenPatientHasNoVisits_ShouldReturnEmptyPage_EdgeCase() throws Exception {
            Authentication auth = new UsernamePasswordAuthenticationToken("patient-id", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")));
            PatientViewDTO patient = new PatientViewDTO();
            patient.setId(1L);
            Pageable pageable = PageRequest.of(0, 10);
            Page<VisitViewDTO> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(patientService.getByKeycloakId("patient-id")).thenReturn(patient);
            when(visitService.getVisitsByPatient(anyLong(), anyInt(), anyInt())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/me/history").principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }
}
