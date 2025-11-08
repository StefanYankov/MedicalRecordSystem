package nbu.cscb869.web.controllers.patient.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.controllers.GlobalExceptionHandler;
import nbu.cscb869.web.controllers.PatientProfileController;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PatientProfileControllerUnitTests {

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private VisitService visitService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PatientProfileController patientProfileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientProfileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("Complete Profile")
    class CompleteProfileTests {
        @Test
        @WithMockUser
        void showCompleteProfileForm_ForNewPatient_ShouldReturnCreateForm() throws Exception {
            when(patientService.getByKeycloakId(anyString())).thenThrow(new EntityNotFoundException("Patient not found"));
            when(doctorService.findByCriteria(any(Specification.class), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(Page.empty());

            mockMvc.perform(get("/profile/complete").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("complete-profile"))
                    .andExpect(model().attributeExists("patient", "generalPractitioners"))
                    .andExpect(model().attribute("patient", any(PatientCreateDTO.class)));
        }

        @Test
        @WithMockUser
        void showCompleteProfileForm_ForExistingPatient_ShouldReturnUpdateForm() throws Exception {
            when(patientService.getByKeycloakId(anyString())).thenReturn(new PatientViewDTO());
            when(modelMapper.map(any(PatientViewDTO.class), eq(PatientUpdateDTO.class))).thenReturn(new PatientUpdateDTO());
            when(doctorService.findByCriteria(any(Specification.class), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(Page.empty());

            mockMvc.perform(get("/profile/complete").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("complete-profile"))
                    .andExpect(model().attributeExists("patient", "generalPractitioners"))
                    .andExpect(model().attribute("patient", any(PatientUpdateDTO.class)));
        }

        @Test
        @WithMockUser
        void completeProfile_ForNewPatient_ShouldCreatePatientAndRedirect() throws Exception {
            when(patientService.getByKeycloakId(anyString())).thenThrow(new EntityNotFoundException("Patient not found"));
            when(modelMapper.map(any(PatientUpdateDTO.class), eq(PatientCreateDTO.class))).thenReturn(new PatientCreateDTO());

            mockMvc.perform(post("/profile/complete").with(oidcLogin()).flashAttr("patient", new PatientUpdateDTO()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile/dashboard"));

            verify(patientService).create(any(PatientCreateDTO.class));
        }

        @Test
        @WithMockUser
        void completeProfile_ForExistingPatient_ShouldUpdatePatientAndRedirect() throws Exception {
            when(patientService.getByKeycloakId(anyString())).thenReturn(new PatientViewDTO());

            mockMvc.perform(post("/profile/complete").with(oidcLogin()).flashAttr("patient", new PatientUpdateDTO()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile/dashboard"));

            verify(patientService).update(any(PatientUpdateDTO.class));
        }
    }

    @Nested
    @DisplayName("Patient Dashboard")
    @WithMockUser(roles = "PATIENT")
    class DashboardTests {
        @Test
        void patientDashboard_ShouldReturnDashboardView_HappyPath() throws Exception {
            when(patientService.getByKeycloakId(anyString())).thenReturn(new PatientViewDTO());
            when(visitService.getVisitsByDoctorAndStatusAndDateRange(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(Page.empty());

            mockMvc.perform(get("/profile/dashboard").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("patient-dashboard"))
                    .andExpect(model().attributeExists("patient", "upcomingVisits"));
        }
    }

    @Nested
    @DisplayName("Medical History")
    @WithMockUser(roles = "PATIENT")
    class MedicalHistoryTests {
        @Test
        void medicalHistory_ShouldReturnHistoryView_HappyPath() throws Exception {
            when(patientService.getByKeycloakId(anyString())).thenReturn(new PatientViewDTO());
            when(visitService.getVisitsByPatient(any(), anyInt(), anyInt())).thenReturn(new PageImpl<>(Collections.singletonList(new VisitViewDTO())));

            mockMvc.perform(get("/profile/history").with(oidcLogin()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("medical-history"))
                    .andExpect(model().attributeExists("visitPage"));
        }
    }
}
