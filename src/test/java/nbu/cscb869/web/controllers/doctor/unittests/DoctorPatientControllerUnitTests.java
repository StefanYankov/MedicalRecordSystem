package nbu.cscb869.web.controllers.doctor.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.controllers.doctor.DoctorPatientController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DoctorPatientControllerUnitTests {

    @Mock
    private PatientService patientService;

    @Mock
    private VisitService visitService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private DoctorPatientController doctorPatientController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(doctorPatientController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("List Patients Tests")
    class ListPatientsTests {
        @Test
        void listPatients_WhenCalled_ShouldReturnPatientListView_HappyPath() throws Exception {
            Page<PatientViewDTO> patientPage = new PageImpl<>(Collections.singletonList(new PatientViewDTO()));
            when(patientService.findAll(any(), isNull())).thenReturn(patientPage);

            mockMvc.perform(get("/doctor/patients")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/patients/list"))
                    .andExpect(model().attributeExists("patientPage"));
        }

        @Test
        void listPatients_WithKeyword_ShouldCallServiceWithKeyword_HappyPath() throws Exception {
            String keyword = "test";
            Page<PatientViewDTO> patientPage = new PageImpl<>(Collections.singletonList(new PatientViewDTO()));
            when(patientService.findAll(any(), eq(keyword))).thenReturn(patientPage);

            mockMvc.perform(get("/doctor/patients")
                            .param("keyword", keyword))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/patients/list"))
                    .andExpect(model().attribute("keyword", keyword));

            verify(patientService).findAll(any(), eq(keyword));
        }
    }

    @Nested
    @DisplayName("Show Patient History Tests")
    class ShowPatientHistoryTests {

        @Mock
        private OidcUser oidcUser;

        @Mock
        private SecurityContext securityContext;

        @Mock
        private Authentication authentication;

        @BeforeEach
        void setupAuthentication() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);
            when(authentication.getPrincipal()).thenReturn(oidcUser);
            when(oidcUser.getSubject()).thenReturn("doctor-keycloak-id");
        }

        @Test
        void showPatientHistory_AsAuthorizedDoctor_ShouldReturnHistoryView_HappyPath() throws Exception {
            DoctorViewDTO doctorViewDTO = new DoctorViewDTO();
            doctorViewDTO.setId(1L);
            PatientViewDTO patientViewDTO = new PatientViewDTO();
            patientViewDTO.setId(1L);
            patientViewDTO.setGeneralPractitionerId(2L);

            when(doctorService.getByKeycloakId("doctor-keycloak-id")).thenReturn(doctorViewDTO);
            when(patientService.getById(1L)).thenReturn(patientViewDTO);
            when(visitService.getVisitsByPatient(anyLong(), anyInt(), anyInt())).thenReturn(Page.empty());
            when(doctorService.getById(2L)).thenReturn(new DoctorViewDTO());

            mockMvc.perform(get("/doctor/patients/1/history"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/patients/history"))
                    .andExpect(model().attributeExists("patient", "loggedInDoctorId"));
        }

        @Test
        void showPatientHistory_WhenPatientNotFound_ShouldReturnErrorPage_ErrorCase() throws Exception {
            when(doctorService.getByKeycloakId(anyString())).thenReturn(new DoctorViewDTO());
            when(patientService.getById(99L)).thenThrow(new EntityNotFoundException("Patient not found"));

            mockMvc.perform(get("/doctor/patients/99/history"))
                    .andExpect(status().isOk()) // The controller handles the exception and returns a view
                    .andExpect(view().name("error/404"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void showPatientHistory_WhenDoctorNotFound_ShouldReturnErrorPage_ErrorCase() throws Exception {
            when(doctorService.getByKeycloakId("doctor-keycloak-id")).thenThrow(new EntityNotFoundException("Doctor not found"));

            mockMvc.perform(get("/doctor/patients/1/history"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error/404"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void showPatientHistory_WhenPatientHasNoGp_ShouldRenderSuccessfully_EdgeCase() throws Exception {
            DoctorViewDTO doctorViewDTO = new DoctorViewDTO();
            doctorViewDTO.setId(1L);
            PatientViewDTO patientViewDTO = new PatientViewDTO();
            patientViewDTO.setId(1L);
            patientViewDTO.setGeneralPractitionerId(null); // No GP

            when(doctorService.getByKeycloakId("doctor-keycloak-id")).thenReturn(doctorViewDTO);
            when(patientService.getById(1L)).thenReturn(patientViewDTO);
            when(visitService.getVisitsByPatient(anyLong(), anyInt(), anyInt())).thenReturn(Page.empty());

            mockMvc.perform(get("/doctor/patients/1/history"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/patients/history"))
                    .andExpect(model().attributeExists("patient"));
        }
    }
}
