package nbu.cscb869.web.controllers.admin.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.PatientService;
import nbu.cscb869.web.controllers.GlobalExceptionHandler;
import nbu.cscb869.web.controllers.admin.AdminPatientController;
import nbu.cscb869.web.viewmodels.AdminPatientViewModel;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AdminPatientControllerUnitTests {

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AdminPatientController adminPatientController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminPatientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("List Patients")
    class ListPatientsTests {
        @Test
        void listPatients_ShouldReturnListViewWithPatients_HappyPath() throws Exception {
            PatientViewDTO patientViewDTO = new PatientViewDTO();
            patientViewDTO.setId(1L);
            patientViewDTO.setName("John Doe");
            patientViewDTO.setGeneralPractitionerId(10L);

            AdminPatientViewModel adminPatientViewModel = new AdminPatientViewModel();
            adminPatientViewModel.setId(1L);
            adminPatientViewModel.setName("John Doe");
            adminPatientViewModel.setGeneralPractitionerName("Dr. Smith");

            Page<PatientViewDTO> patientDtoPage = new PageImpl<>(Collections.singletonList(patientViewDTO));
            
            DoctorViewDTO doctorViewDTO = new DoctorViewDTO();
            doctorViewDTO.setId(10L);
            doctorViewDTO.setName("Dr. Smith");
            doctorViewDTO.setUniqueIdNumber("12345");
            doctorViewDTO.setGeneralPractitioner(true);
            doctorViewDTO.setKeycloakId("keycloak1");
            doctorViewDTO.setImageUrl(null);
            doctorViewDTO.setSpecialties(Collections.emptySet());

            when(patientService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull()))
                    .thenReturn(CompletableFuture.completedFuture(patientDtoPage));
            when(modelMapper.map(any(PatientViewDTO.class), eq(AdminPatientViewModel.class)))
                    .thenReturn(adminPatientViewModel);
            when(doctorService.getById(anyLong())).thenReturn(doctorViewDTO);

            mockMvc.perform(get("/admin/patients"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/patients/list"))
                    .andExpect(model().attributeExists("patients"));

            verify(patientService).getAll(0, 10, "name", true, null);
            verify(doctorService).getById(10L);
        }

        @Test
        void listPatients_WithNoGp_ShouldReturnListViewWithPatients_EdgeCase() throws Exception {
            PatientViewDTO patientViewDTO = new PatientViewDTO();
            patientViewDTO.setId(1L);
            patientViewDTO.setName("John Doe");
            patientViewDTO.setGeneralPractitionerId(null);

            AdminPatientViewModel adminPatientViewModel = new AdminPatientViewModel();
            adminPatientViewModel.setId(1L);
            adminPatientViewModel.setName("John Doe");
            adminPatientViewModel.setGeneralPractitionerName("N/A");

            Page<PatientViewDTO> patientDtoPage = new PageImpl<>(Collections.singletonList(patientViewDTO));

            when(patientService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull()))
                    .thenReturn(CompletableFuture.completedFuture(patientDtoPage));
            when(modelMapper.map(any(PatientViewDTO.class), eq(AdminPatientViewModel.class)))
                    .thenReturn(adminPatientViewModel);

            mockMvc.perform(get("/admin/patients"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/patients/list"))
                    .andExpect(model().attributeExists("patients"));

            verify(patientService).getAll(0, 10, "name", true, null);
            verify(doctorService, never()).getById(anyLong());
        }

        @Test
        void listPatients_WithGpServiceError_ShouldReturnListViewWithPatientsAndNaGp_ErrorCase() throws Exception {
            PatientViewDTO patientViewDTO = new PatientViewDTO();
            patientViewDTO.setId(1L);
            patientViewDTO.setName("John Doe");
            patientViewDTO.setGeneralPractitionerId(10L);

            AdminPatientViewModel adminPatientViewModel = new AdminPatientViewModel();
            adminPatientViewModel.setId(1L);
            adminPatientViewModel.setName("John Doe");
            adminPatientViewModel.setGeneralPractitionerName("N/A");

            Page<PatientViewDTO> patientDtoPage = new PageImpl<>(Collections.singletonList(patientViewDTO));

            when(patientService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull()))
                    .thenReturn(CompletableFuture.completedFuture(patientDtoPage));
            when(modelMapper.map(any(PatientViewDTO.class), eq(AdminPatientViewModel.class)))
                    .thenReturn(adminPatientViewModel);
            doThrow(new RuntimeException("GP Service Error")).when(doctorService).getById(anyLong());

            mockMvc.perform(get("/admin/patients"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/patients/list"))
                    .andExpect(model().attributeExists("patients"));

            verify(patientService).getAll(0, 10, "name", true, null);
            verify(doctorService).getById(10L);
        }
    }

    @Nested
    @DisplayName("Edit Patient")
    class EditPatientTests {
        @Test
        void showEditPatientForm_ShouldReturnEditView_HappyPath() throws Exception {
            when(patientService.getById(anyLong())).thenReturn(new PatientViewDTO());
            when(doctorService.getAllAsync(anyInt(), anyInt(), anyString(), anyBoolean(), isNull()))
                    .thenReturn(CompletableFuture.completedFuture(new PageImpl<>(Collections.singletonList(new DoctorViewDTO()))));

            mockMvc.perform(get("/admin/patients/edit/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/patients/edit"))
                    .andExpect(model().attributeExists("patient", "doctors"));
        }

        @Test
        void editPatient_WithValidData_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            when(patientService.update(any(PatientUpdateDTO.class))).thenReturn(new PatientViewDTO());

            mockMvc.perform(post("/admin/patients/edit/1")
                            .param("id", "1")
                            .param("name", "Jane Doe")
                            .param("egn", "1234567890")
                            .param("generalPractitionerId", "10")
                            .param("lastInsurancePaymentDate", "2023-01-01"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/patients"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(patientService).update(any(PatientUpdateDTO.class));
        }

        @Test
        void editPatient_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            doThrow(new RuntimeException("Update error")).when(patientService).update(any(PatientUpdateDTO.class));

            mockMvc.perform(post("/admin/patients/edit/1")
                            .param("id", "1")
                            .param("name", "Jane Doe")
                            .param("egn", "1234567890")
                            .param("generalPractitionerId", "10")
                            .param("lastInsurancePaymentDate", "2023-01-01"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/patients"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Delete Patient")
    class DeletePatientTests {
        @Test
        void showDeleteConfirmation_ShouldReturnConfirmationView_HappyPath() throws Exception {
            when(patientService.getById(anyLong())).thenReturn(new PatientViewDTO());

            mockMvc.perform(get("/admin/patients/delete/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/patients/delete-confirm"))
                    .andExpect(model().attributeExists("patient"));
        }

        @Test
        void showDeleteConfirmation_ForNonExistentPatient_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Patient not found")).when(patientService).getById(anyLong());

            mockMvc.perform(get("/admin/patients/delete/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deletePatientConfirmed_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            doNothing().when(patientService).delete(anyLong());

            mockMvc.perform(post("/admin/patients/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/patients"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(patientService).delete(1L);
        }

        @Test
        void deletePatientConfirmed_WhenServiceThrowsException_ShouldRedirectAndShowError_ErrorCase() throws Exception {
            doThrow(new RuntimeException("Delete error")).when(patientService).delete(anyLong());

            mockMvc.perform(post("/admin/patients/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/patients"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("Update Insurance Status")
    class UpdateInsuranceStatusTests {
        @Test
        void updateInsuranceStatus_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            when(patientService.updateInsuranceStatus(anyLong())).thenReturn(new PatientViewDTO());

            mockMvc.perform(post("/admin/patients/1/update-insurance"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/patients/edit/1"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(patientService).updateInsuranceStatus(1L);
        }

        @Test
        void updateInsuranceStatus_WhenServiceThrowsException_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Patient not found")).when(patientService).updateInsuranceStatus(anyLong());

            mockMvc.perform(post("/admin/patients/1/update-insurance"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateInsuranceStatusManual_ShouldRedirectAndShowSuccess_HappyPath() throws Exception {
            PatientViewDTO patientViewDTO = new PatientViewDTO();
            patientViewDTO.setId(1L);
            patientViewDTO.setName("John Doe");
            patientViewDTO.setEgn("1234567890");
            patientViewDTO.setLastInsurancePaymentDate(LocalDate.now().minusDays(1));

            when(patientService.getById(anyLong())).thenReturn(patientViewDTO);
            when(modelMapper.map(any(PatientViewDTO.class), eq(PatientUpdateDTO.class))).thenReturn(new PatientUpdateDTO());
            when(patientService.update(any(PatientUpdateDTO.class))).thenReturn(new PatientViewDTO());

            mockMvc.perform(post("/admin/patients/1/update-insurance-manual")
                            .param("manualInsuranceDate", "2023-03-15"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/patients/edit/1"))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(patientService).getById(1L);
            verify(patientService).update(any(PatientUpdateDTO.class));
        }

        @Test
        void updateInsuranceStatusManual_WhenServiceThrowsException_ShouldReturnNotFound_ErrorCase() throws Exception {
            doThrow(new EntityNotFoundException("Patient not found")).when(patientService).getById(anyLong());

            mockMvc.perform(post("/admin/patients/1/update-insurance-manual")
                            .param("manualInsuranceDate", "2023-03-15"))
                    .andExpect(status().isNotFound());
        }
    }
}
