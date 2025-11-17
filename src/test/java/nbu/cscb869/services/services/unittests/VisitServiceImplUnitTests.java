package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.common.exceptions.PatientInsuranceException;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.VisitServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplUnitTests {

    @Mock
    private VisitRepository visitRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private DiagnosisRepository diagnosisRepository;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private VisitServiceImpl visitService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Patient setupPatient(boolean hasValidInsurance) {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setKeycloakId("patient-owner-id");
        patient.setLastInsurancePaymentDate(hasValidInsurance ? LocalDate.now() : LocalDate.now().minusMonths(7));
        return patient;
    }

    @Nested
    @DisplayName("Create and Schedule Tests")
    class CreateAndScheduleTests {
        @Test
        void create_WithFullDetails_ShouldSucceed_HappyPath() {
            VisitCreateDTO dto = new VisitCreateDTO();
            dto.setPatientId(1L);
            dto.setDoctorId(2L);
            dto.setDiagnosisId(3L);
            dto.setVisitDate(LocalDate.now());
            dto.setVisitTime(LocalTime.of(10, 0));
            dto.setTreatment(new TreatmentCreateDTO());
            dto.setSickLeave(new SickLeaveCreateDTO());

            when(patientRepository.findById(1L)).thenReturn(Optional.of(setupPatient(true)));
            when(doctorRepository.findById(2L)).thenReturn(Optional.of(new Doctor()));
            when(diagnosisRepository.findById(3L)).thenReturn(Optional.of(new Diagnosis()));
            when(visitRepository.save(any(Visit.class))).thenReturn(new Visit());

            visitService.create(dto);

            ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
            verify(visitRepository).save(visitCaptor.capture());
            assertNotNull(visitCaptor.getValue().getTreatment());
            assertNotNull(visitCaptor.getValue().getSickLeave());
            assertEquals(VisitStatus.SCHEDULED, visitCaptor.getValue().getStatus());
        }

        @Test
        void scheduleNewVisitByPatient_WithValidData_ShouldSucceed_HappyPath() {
            VisitCreateDTO dto = new VisitCreateDTO();
            dto.setPatientId(1L);
            dto.setDoctorId(2L);
            dto.setVisitDate(LocalDate.now());
            dto.setVisitTime(LocalTime.of(10, 0));

            when(patientRepository.findById(1L)).thenReturn(Optional.of(setupPatient(true)));
            when(doctorRepository.findById(2L)).thenReturn(Optional.of(new Doctor()));
            when(visitRepository.save(any(Visit.class))).thenReturn(new Visit());

            visitService.scheduleNewVisitByPatient(dto);

            ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
            verify(visitRepository).save(visitCaptor.capture());
            assertEquals(VisitStatus.SCHEDULED, visitCaptor.getValue().getStatus());
        }

        @Test
        void scheduleNewVisitByPatient_WithInvalidInsurance_ShouldThrowException_ErrorCase() {
            VisitCreateDTO dto = new VisitCreateDTO();
            dto.setPatientId(1L);
            dto.setDoctorId(2L);
            dto.setVisitDate(LocalDate.now());
            dto.setVisitTime(LocalTime.of(10, 0));

            when(patientRepository.findById(1L)).thenReturn(Optional.of(setupPatient(false))); // Invalid insurance
            when(doctorRepository.findById(2L)).thenReturn(Optional.of(new Doctor()));

            assertThrows(PatientInsuranceException.class, () -> visitService.scheduleNewVisitByPatient(dto));
        }

        @Test
        void create_WithBookedTimeSlot_ShouldThrowInvalidInputException_ErrorCase() {
            VisitCreateDTO dto = new VisitCreateDTO();
            dto.setPatientId(1L);
            dto.setDoctorId(2L);
            dto.setVisitDate(LocalDate.now());
            dto.setVisitTime(LocalTime.of(10, 0));

            when(patientRepository.findById(any())).thenReturn(Optional.of(setupPatient(true)));
            when(doctorRepository.findById(2L)).thenReturn(Optional.of(new Doctor()));
            when(visitRepository.findByDoctorAndDateTime(any(), any(), any())).thenReturn(Optional.of(new Visit()));

            assertThrows(InvalidInputException.class, () -> visitService.create(dto));
        }

        @Test
        void create_WithNonExistentPatient_ShouldThrowEntityNotFoundException_ErrorCase() {
            VisitCreateDTO dto = new VisitCreateDTO();
            dto.setPatientId(99L);
            when(patientRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> visitService.create(dto));
        }
    }

    @Nested
    @DisplayName("Update and Document Tests")
    class UpdateAndDocumentTests {
        @Test
        void update_WithValidData_ShouldSucceed_HappyPath() {
            VisitUpdateDTO dto = new VisitUpdateDTO();
            dto.setId(1L);
            dto.setPatientId(1L);
            dto.setDoctorId(2L);
            dto.setDiagnosisId(3L);
            dto.setVisitDate(LocalDate.now());
            dto.setVisitTime(LocalTime.of(10, 0));

            when(visitRepository.findById(1L)).thenReturn(Optional.of(new Visit()));
            when(patientRepository.findById(1L)).thenReturn(Optional.of(setupPatient(true)));
            when(doctorRepository.findById(2L)).thenReturn(Optional.of(new Doctor()));
            when(diagnosisRepository.findById(3L)).thenReturn(Optional.of(new Diagnosis()));
            when(visitRepository.save(any(Visit.class))).thenReturn(new Visit());
            when(modelMapper.map(any(Visit.class), eq(VisitViewDTO.class))).thenReturn(new VisitViewDTO());

            VisitViewDTO result = visitService.update(dto);

            assertNotNull(result);
            verify(visitRepository).save(any(Visit.class));
        }

        @Test
        void documentVisit_WithScheduledVisit_ShouldSucceed_HappyPath() {
            VisitDocumentationDTO dto = new VisitDocumentationDTO();
            dto.setDiagnosisId(1L);
            dto.setNotes("Patient is recovering well.");

            Visit visit = new Visit();
            visit.setStatus(VisitStatus.SCHEDULED); // Correct initial state

            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(new Diagnosis()));
            when(visitRepository.save(any(Visit.class))).thenReturn(new Visit());

            visitService.documentVisit(1L, dto);

            ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
            verify(visitRepository).save(visitCaptor.capture());
            assertEquals(VisitStatus.COMPLETED, visitCaptor.getValue().getStatus());
            assertEquals("Patient is recovering well.", visitCaptor.getValue().getNotes());
        }

        @Test
        void documentVisit_WithNonScheduledVisit_ShouldThrowException_ErrorCase() {
            VisitDocumentationDTO dto = new VisitDocumentationDTO();
            Visit visit = new Visit();
            visit.setStatus(VisitStatus.COMPLETED); // Incorrect initial state

            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));

            assertThrows(InvalidInputException.class, () -> visitService.documentVisit(1L, dto));
        }

        @Test
        void update_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
            assertThrows(InvalidDTOException.class, () -> visitService.update(new VisitUpdateDTO()));
        }

        @Test
        void update_WithNonExistentVisit_ShouldThrowEntityNotFoundException_ErrorCase() {
            VisitUpdateDTO dto = new VisitUpdateDTO();
            dto.setId(99L);
            when(visitRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> visitService.update(dto));
        }
    }

    @Nested
    @DisplayName("Delete and Cancel Tests")
    class DeleteAndCancelTests {
        @Test
        void delete_WithExistingId_ShouldSucceed_HappyPath() {
            when(visitRepository.existsById(1L)).thenReturn(true);
            visitService.delete(1L);
            verify(visitRepository).deleteById(1L);
        }

        @Test
        void cancelVisit_WithScheduledVisit_ShouldSucceed_HappyPath() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("patient-owner-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );
            Visit visit = new Visit();
            visit.setPatient(setupPatient(true));
            visit.setStatus(VisitStatus.SCHEDULED);

            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));

            visitService.cancelVisit(1L);

            ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
            verify(visitRepository).save(visitCaptor.capture());
            assertEquals(VisitStatus.CANCELLED_BY_PATIENT, visitCaptor.getValue().getStatus());
        }

        @Test
        void cancelVisit_WithNonScheduledVisit_ShouldThrowException_ErrorCase() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("patient-owner-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );
            Visit visit = new Visit();
            visit.setPatient(setupPatient(true));
            visit.setStatus(VisitStatus.COMPLETED); // Incorrect state

            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));

            assertThrows(InvalidInputException.class, () -> visitService.cancelVisit(1L));
        }

        @Test
        void delete_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
            when(visitRepository.existsById(99L)).thenReturn(false);
            assertThrows(EntityNotFoundException.class, () -> visitService.delete(99L));
        }
    }

    @Nested
    @DisplayName("GetById and Authorization Tests")
    class GetByIdAndAuthorizationTests {
        @Test
        void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("patient-owner-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );
            Visit visit = new Visit();
            visit.setPatient(setupPatient(true));

            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
            when(modelMapper.map(visit, VisitViewDTO.class)).thenReturn(new VisitViewDTO());

            assertDoesNotThrow(() -> visitService.getById(1L));
        }

        @Test
        void getById_AsOtherPatient_ShouldThrowAccessDeniedException_ErrorCase() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("other-patient-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );
            Visit visit = new Visit();
            visit.setPatient(setupPatient(true)); // Belongs to patient-owner-id

            when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));

            assertThrows(AccessDeniedException.class, () -> visitService.getById(1L));
        }
    }

    @Nested
    @DisplayName("Reporting and Find Tests")
    class ReportingAndFindTests {
        @Test
        void getVisitsByPatient_AsPatientOwner_ShouldSucceed_HappyPath() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("patient-owner-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );
            Patient patient = setupPatient(true);

            when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
            when(visitRepository.findByPatient(any(), any())).thenReturn(Page.empty());

            assertDoesNotThrow(() -> visitService.getVisitsByPatient(1L, 0, 10));
        }

        @Test
        void getVisitsByPatient_AsOtherPatient_ShouldThrowAccessDeniedException_ErrorCase() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("other-patient-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );
            Patient patient = setupPatient(true); // Belongs to patient-owner-id

            when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

            assertThrows(AccessDeniedException.class, () -> visitService.getVisitsByPatient(1L, 0, 10));
        }

        @Test
        void getVisitsByDateRange_WithInvalidRange_ShouldThrowInvalidInputException_ErrorCase() {
            LocalDate start = LocalDate.now();
            LocalDate end = LocalDate.now().minusDays(1);
            assertThrows(InvalidInputException.class, () -> visitService.getVisitsByDateRange(start, end, 0, 10));
        }

        @Test
        void getVisitsByDoctorAndStatusAndDateRange_WithValidData_ShouldCallRepository_HappyPath() {
            Long doctorId = 1L;
            VisitStatus status = VisitStatus.SCHEDULED;
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(1);
            Doctor doctor = new Doctor();

            when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
            when(visitRepository.findByDoctorAndStatusAndVisitDateBetweenOrderByVisitDateAscVisitTimeAsc(any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            visitService.getVisitsByDoctorAndStatusAndDateRange(doctorId, status, startDate, endDate, 0, 10);

            verify(visitRepository, times(1))
                    .findByDoctorAndStatusAndVisitDateBetweenOrderByVisitDateAscVisitTimeAsc(eq(doctor), eq(status), eq(startDate), eq(endDate), any(Pageable.class));
        }

        @Test
        void getMostFrequentDiagnoses_ShouldCallRepository_HappyPath() {
            visitService.getMostFrequentDiagnoses();
            verify(visitRepository, times(1)).findMostFrequentDiagnoses();
        }

        @Test
        void getMostFrequentSickLeaveMonth_ShouldCallRepository_HappyPath() {
            visitService.getMostFrequentSickLeaveMonth();
            verify(visitRepository, times(1)).findMostFrequentSickLeaveMonth();
        }
    }

    @Nested
    @DisplayName("GetAll Test")
    class GetAllTest {
        @Test
        void getAll_WithFilter_ShouldCallCorrectRepositoryMethod_HappyPath() {
            String filter = "test-filter";
            when(visitRepository.findByPatientOrDoctorFilter(anyString(), any())).thenReturn(Page.empty());

            CompletableFuture<Page<VisitViewDTO>> future = visitService.getAll(0, 10, "visitDate", true, filter);
            future.join();

            verify(visitRepository).findByPatientOrDoctorFilter(eq(filter), any(Pageable.class));
            verify(visitRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        void getAll_WhenServiceThrowsException_ShouldThrowException_ErrorCase() {
            when(visitRepository.findAll(any(Pageable.class))).thenThrow(new RuntimeException("DB Error"));

            assertThrows(RuntimeException.class, () -> {
                CompletableFuture<Page<VisitViewDTO>> future = visitService.getAll(0, 10, "visitDate", true, null);
                future.join();
            });
        }
    }
}
