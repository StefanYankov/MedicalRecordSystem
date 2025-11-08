package nbu.cscb869.web.controllers.admin.unittests;

import nbu.cscb869.data.dto.*;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;
import nbu.cscb869.services.services.contracts.DiagnosisService;
import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.controllers.GlobalExceptionHandler;
import nbu.cscb869.web.controllers.admin.AdminReportsController;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AdminReportsControllerUnitTests {

    @Mock
    private VisitService visitService;

    @Mock
    private DiagnosisService diagnosisService;

    @Mock
    private DoctorService doctorService;

    @InjectMocks
    private AdminReportsController adminReportsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminReportsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("Index Page")
    class IndexTests {
        @Test
        void reportsIndex_ShouldReturnCorrectView_HappyPath() throws Exception {
            mockMvc.perform(get("/admin/reports"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/index"));
        }
    }

    @Nested
    @DisplayName("Patients by Diagnosis Report")
    class PatientsByDiagnosisTests {
        @Test
        void getPatientsByDiagnosis_WithId_ShouldReturnData_HappyPath() throws Exception {
            Page<VisitViewDTO> page = new PageImpl<>(Collections.singletonList(new VisitViewDTO()));
            when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(Page.empty()));
            when(visitService.getVisitsByDiagnosis(anyLong(), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/admin/reports/patients-by-diagnosis").param("diagnosisId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/patients-by-diagnosis"))
                    .andExpect(model().attributeExists("diagnoses", "visits", "selectedDiagnosisId"));

            verify(visitService).getVisitsByDiagnosis(1L, 0, 10);
        }

        @Test
        void getPatientsByDiagnosis_WithoutId_ShouldNotCallService_EdgeCase() throws Exception {
            when(diagnosisService.getAll(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(Page.empty()));

            mockMvc.perform(get("/admin/reports/patients-by-diagnosis"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/patients-by-diagnosis"))
                    .andExpect(model().attributeExists("diagnoses"))
                    .andExpect(model().attributeDoesNotExist("visits"));

            verify(visitService, never()).getVisitsByDiagnosis(anyLong(), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("Most Frequent Diagnoses Report")
    class MostFrequentDiagnosesTests {
        @Test
        void getMostFrequentDiagnoses_ShouldReturnData_HappyPath() throws Exception {
            Diagnosis diagnosis = new Diagnosis();
            diagnosis.setName("Flu");
            DiagnosisVisitCountDTO dto = DiagnosisVisitCountDTO.builder().diagnosis(diagnosis).visitCount(10L).build();
            when(visitService.getMostFrequentDiagnoses()).thenReturn(Collections.singletonList(dto));

            mockMvc.perform(get("/admin/reports/most-frequent-diagnoses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/most-frequent-diagnoses"))
                    .andExpect(model().attributeExists("diagnoses"));
        }

        @Test
        void getMostFrequentDiagnoses_WhenNoData_ShouldReturnEmptyList_EdgeCase() throws Exception {
            when(visitService.getMostFrequentDiagnoses()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/admin/reports/most-frequent-diagnoses"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/most-frequent-diagnoses"))
                    .andExpect(model().attribute("diagnoses", Collections.emptyList()));
        }

        @Test
        void getMostFrequentDiagnoses_WhenServiceFails_ShouldReturnErrorView_ErrorCase() throws Exception {
            when(visitService.getMostFrequentDiagnoses()).thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(get("/admin/reports/most-frequent-diagnoses"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(view().name("error"));
        }
    }

    @Nested
    @DisplayName("Patients by GP Report")
    class PatientsByGpTests {
        @Test
        void getPatientsByGp_WithId_ShouldReturnData_HappyPath() throws Exception {
            Page<PatientViewDTO> page = new PageImpl<>(Collections.singletonList(new PatientViewDTO()));
            when(doctorService.getAllAsync(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(Page.empty()));
            when(doctorService.getPatientsByGeneralPractitioner(anyLong(), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/admin/reports/patients-by-gp").param("gpId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/patients-by-gp"))
                    .andExpect(model().attributeExists("doctors", "patients", "selectedGpId"));

            verify(doctorService).getPatientsByGeneralPractitioner(1L, 0, 10);
        }
    }

    @Nested
    @DisplayName("Patient Count by GP Report")
    class PatientCountByGpTests {
        @Test
        void getPatientCountByGp_ShouldReturnData_HappyPath() throws Exception {
            Doctor doctor = new Doctor();
            doctor.setName("Dr. Feelgood");
            DoctorPatientCountDTO dto = DoctorPatientCountDTO.builder().doctor(doctor).patientCount(5L).build();
            when(doctorService.getPatientCountByGeneralPractitioner()).thenReturn(Collections.singletonList(dto));

            mockMvc.perform(get("/admin/reports/patient-count-by-gp"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/patient-count-by-gp"))
                    .andExpect(model().attributeExists("gpCounts"));
        }
    }

    @Nested
    @DisplayName("Visit Count by Doctor Report")
    class VisitCountByDoctorTests {
        @Test
        void getVisitCountByDoctor_ShouldReturnData_HappyPath() throws Exception {
            Doctor doctor = new Doctor();
            doctor.setName("Dr. Who");
            DoctorVisitCountDTO dto = DoctorVisitCountDTO.builder().doctor(doctor).visitCount(10L).build();
            when(doctorService.getVisitCount()).thenReturn(Collections.singletonList(dto));

            mockMvc.perform(get("/admin/reports/visit-count-by-doctor"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/visit-count-by-doctor"))
                    .andExpect(model().attributeExists("visitCounts"));
        }
    }

    @Nested
    @DisplayName("Visits by Period Report")
    class VisitsByPeriodTests {
        @Test
        void getVisitsByPeriod_WithDates_ShouldReturnData_HappyPath() throws Exception {
            Page<VisitViewDTO> page = new PageImpl<>(Collections.singletonList(new VisitViewDTO()));
            when(visitService.getVisitsByDateRange(any(LocalDate.class), any(LocalDate.class), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/admin/reports/visits-by-period")
                            .param("startDate", "2023-01-01")
                            .param("endDate", "2023-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/visits-by-period"))
                    .andExpect(model().attributeExists("visits", "startDate", "endDate"));
        }
    }

    @Nested
    @DisplayName("Visits by Doctor and Period Report")
    class VisitsByDoctorAndPeriodTests {
        @Test
        void getVisitsByDoctorAndPeriod_WithParams_ShouldReturnData_HappyPath() throws Exception {
            Page<VisitViewDTO> page = new PageImpl<>(Collections.singletonList(new VisitViewDTO()));
            when(doctorService.getAllAsync(anyInt(), anyInt(), anyString(), anyBoolean(), isNull())).thenReturn(CompletableFuture.completedFuture(Page.empty()));
            when(doctorService.getVisitsByPeriod(anyLong(), any(LocalDate.class), any(LocalDate.class), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/admin/reports/visits-by-doctor-and-period")
                            .param("doctorId", "1")
                            .param("startDate", "2023-01-01")
                            .param("endDate", "2023-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/visits-by-doctor-and-period"))
                    .andExpect(model().attributeExists("doctors", "visits", "selectedDoctorId", "startDate", "endDate"));
        }
    }

    @Nested
    @DisplayName("Most Frequent Sick Leave Month Report")
    class MostFrequentSickLeaveMonthTests {
        @Test
        void getMostFrequentSickLeaveMonth_ShouldReturnData_HappyPath() throws Exception {
            when(visitService.getMostFrequentSickLeaveMonth()).thenReturn(Collections.singletonList(new MonthSickLeaveCountDTO(1, 5L)));

            mockMvc.perform(get("/admin/reports/most-frequent-sick-leave-month"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/most-frequent-sick-leave-month"))
                    .andExpect(model().attributeExists("sickLeaveMonths"));
        }
    }

    @Nested
    @DisplayName("Doctors with Most Sick Leaves Report")
    class DoctorsWithMostSickLeavesTests {
        @Test
        void getDoctorsWithMostSickLeaves_ShouldReturnData_HappyPath() throws Exception {
            Doctor doctor = new Doctor();
            doctor.setName("Dr. House");
            DoctorSickLeaveCountDTO dto = DoctorSickLeaveCountDTO.builder().doctor(doctor).sickLeaveCount(20L).build();
            when(doctorService.getDoctorsWithMostSickLeaves()).thenReturn(Collections.singletonList(dto));

            mockMvc.perform(get("/admin/reports/doctors-with-most-sick-leaves"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports/doctors-with-most-sick-leaves"))
                    .andExpect(model().attributeExists("doctorsWithMostSickLeaves"));
        }
    }
}
