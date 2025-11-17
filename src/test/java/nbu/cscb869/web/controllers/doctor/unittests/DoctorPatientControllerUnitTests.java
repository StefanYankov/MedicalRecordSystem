package nbu.cscb869.web.controllers.doctor.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.controllers.GlobalExceptionHandler;
import nbu.cscb869.web.controllers.doctor.DoctorPatientController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DoctorPatientController.class)
@Import(GlobalExceptionHandler.class)
class DoctorPatientControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;

    @MockBean
    private VisitService visitService;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private ModelMapper modelMapper;

    @Nested
    @DisplayName("List Patients Tests")
    class ListPatientsTests {
        @Test
        void listPatients_WhenCalled_ShouldReturnPatientListView_HappyPath() throws Exception {
            Page<PatientViewDTO> patientPage = new PageImpl<>(Collections.singletonList(new PatientViewDTO()));
            when(patientService.findAll(any(), isNull())).thenReturn(patientPage);

            mockMvc.perform(get("/doctor/patients")
                            .with(oidcLogin()) // Add authentication
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
                            .with(oidcLogin()) // Add authentication
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

        private OidcUser createMockOidcUser(String keycloakId) {
            OidcUser oidcUser = mock(OidcUser.class);
            when(oidcUser.getSubject()).thenReturn(keycloakId);
            when(oidcUser.getName()).thenReturn(keycloakId);
            return oidcUser;
        }

        @Test
        void showPatientHistory_AsAuthorizedDoctor_ShouldReturnHistoryView_HappyPath() throws Exception {
            OidcUser oidcUser = createMockOidcUser("doctor-keycloak-id");
            DoctorViewDTO doctorViewDTO = new DoctorViewDTO();
            doctorViewDTO.setId(1L);
            PatientViewDTO patientViewDTO = new PatientViewDTO();
            patientViewDTO.setId(1L);
            patientViewDTO.setGeneralPractitionerId(2L);

            when(doctorService.getByKeycloakId("doctor-keycloak-id")).thenReturn(doctorViewDTO);
            when(patientService.getById(1L)).thenReturn(patientViewDTO);
            when(visitService.getVisitsByPatient(anyLong(), anyInt(), anyInt())).thenReturn(Page.empty());
            when(doctorService.getById(2L)).thenReturn(new DoctorViewDTO());

            mockMvc.perform(get("/doctor/patients/1/history").with(oidcLogin().oidcUser(oidcUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/patients/history"))
                    .andExpect(model().attributeExists("patient", "loggedInDoctorId"));
        }

        @Test
        void showPatientHistory_WhenPatientNotFound_ShouldReturnErrorPage_ErrorCase() throws Exception {
            OidcUser oidcUser = createMockOidcUser("doctor-keycloak-id");
            when(doctorService.getByKeycloakId(anyString())).thenReturn(new DoctorViewDTO());
            when(patientService.getById(99L)).thenThrow(new EntityNotFoundException("Patient not found"));

            mockMvc.perform(get("/doctor/patients/99/history").with(oidcLogin().oidcUser(oidcUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("error"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void showPatientHistory_WhenDoctorNotFound_ShouldReturnErrorPage_ErrorCase() throws Exception {
            OidcUser oidcUser = createMockOidcUser("doctor-keycloak-id");
            when(doctorService.getByKeycloakId("doctor-keycloak-id")).thenThrow(new EntityNotFoundException("Doctor not found"));

            mockMvc.perform(get("/doctor/patients/1/history").with(oidcLogin().oidcUser(oidcUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("error"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void showPatientHistory_WhenPatientHasNoGp_ShouldRenderSuccessfully_EdgeCase() throws Exception {
            OidcUser oidcUser = createMockOidcUser("doctor-keycloak-id");
            DoctorViewDTO doctorViewDTO = new DoctorViewDTO();
            doctorViewDTO.setId(1L);
            PatientViewDTO patientViewDTO = new PatientViewDTO();
            patientViewDTO.setId(1L);
            patientViewDTO.setGeneralPractitionerId(null);

            when(doctorService.getByKeycloakId("doctor-keycloak-id")).thenReturn(doctorViewDTO);
            when(patientService.getById(1L)).thenReturn(patientViewDTO);
            when(visitService.getVisitsByPatient(anyLong(), anyInt(), anyInt())).thenReturn(Page.empty());

            mockMvc.perform(get("/doctor/patients/1/history").with(oidcLogin().oidcUser(oidcUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("doctor/patients/history"))
                    .andExpect(model().attributeExists("patient"));
        }
    }
}
