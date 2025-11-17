package nbu.cscb869.web.api.unittests;

import nbu.cscb869.services.services.contracts.DoctorService;
import nbu.cscb869.services.services.contracts.VisitService;
import nbu.cscb869.web.api.controllers.ReportsApiController;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportsApiControllerUnitTests {
    @Mock
    private DoctorService doctorService;

    @Mock
    private VisitService visitService;

    @InjectMocks
    private ReportsApiController reportsApiController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportsApiController)
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("Report Endpoints")
    class ReportEndpoints {
        @Test
        void getPatientsByDiagnosis_ShouldReturnPage_HappyPath() throws Exception {
            when(visitService.getVisitsByDiagnosis(anyLong(), anyInt(), anyInt())).thenReturn(Page.empty());
            mockMvc.perform(get("/api/reports/patients-by-diagnosis").param("diagnosisId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getMostFrequentDiagnoses_ShouldReturnList_HappyPath() throws Exception {
            when(visitService.getMostFrequentDiagnoses()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/reports/most-frequent-diagnoses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        void getGpPatientCounts_ShouldReturnList_HappyPath() throws Exception {
            when(doctorService.getPatientCountByGeneralPractitioner()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/reports/gp-patient-counts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        void getDoctorVisitCounts_ShouldReturnList_HappyPath() throws Exception {
            when(visitService.getVisitCountByDoctor()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/reports/doctor-visit-counts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        void getDoctorsWithMostSickLeaves_ShouldReturnList_HappyPath() throws Exception {
            when(doctorService.getDoctorsWithMostSickLeaves()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/reports/doctors-with-most-sick-leaves"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        void getMostFrequentSickLeaveMonth_ShouldReturnList_HappyPath() throws Exception {
            when(visitService.getMostFrequentSickLeaveMonth()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/reports/most-frequent-sick-leave-month"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        void getVisitsByDateRange_ShouldReturnPage_HappyPath() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            when(visitService.getVisitsByDateRange(any(), any(), anyInt(), anyInt())).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
            mockMvc.perform(get("/api/reports/visits-by-date")
                            .param("startDate", LocalDate.now().toString())
                            .param("endDate", LocalDate.now().plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getDoctorVisitsByDateRange_ShouldReturnPage_HappyPath() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            when(visitService.getVisitsByDoctorAndDateRange(anyLong(), any(), any(), anyInt(), anyInt())).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
            mockMvc.perform(get("/api/reports/doctor-visits-by-date")
                            .param("doctorId", "1")
                            .param("startDate", LocalDate.now().toString())
                            .param("endDate", LocalDate.now().plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("Error Case Tests")
    class ErrorCaseTests {
        @Test
        void getPatientsByDiagnosis_WithoutDiagnosisId_ShouldReturnBadRequest_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/reports/patients-by-diagnosis"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void getVisitsByDateRange_WithoutDates_ShouldReturnBadRequest_ErrorCase() throws Exception {
            mockMvc.perform(get("/api/reports/visits-by-date"))
                    .andExpect(status().isBadRequest());
        }
    }
}
