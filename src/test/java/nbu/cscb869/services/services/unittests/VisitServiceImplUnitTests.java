package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.VisitServiceImpl;
import nbu.cscb869.services.services.utility.contracts.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
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

    // --- Create Tests ---

    @Test
    void Create_WithNullChildren_ShouldSucceed_EdgeCase() {
        // ARRANGE
        VisitCreateDTO dto = new VisitCreateDTO();
        dto.setPatientId(1L);
        dto.setDoctorId(2L);
        dto.setDiagnosisId(3L);
        dto.setVisitDate(LocalDate.now());
        dto.setVisitTime(LocalTime.of(10, 0));
        dto.setTreatment(null);
        dto.setSickLeave(null);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(setupPatient(true)));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(new Doctor()));
        when(diagnosisRepository.findById(3L)).thenReturn(Optional.of(new Diagnosis()));
        when(visitRepository.save(any(Visit.class))).thenReturn(new Visit());

        // ACT
        visitService.create(dto);

        // ASSERT
        ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
        verify(visitRepository).save(visitCaptor.capture());
        assertNull(visitCaptor.getValue().getTreatment());
        assertNull(visitCaptor.getValue().getSickLeave());
    }

    @Test
    void Create_WithBookedTimeSlot_ShouldThrowInvalidInputException_ErrorCase() {
        // ARRANGE
        VisitCreateDTO dto = new VisitCreateDTO();
        dto.setDoctorId(2L);
        dto.setVisitDate(LocalDate.now());
        dto.setVisitTime(LocalTime.of(10, 0));

        when(patientRepository.findById(any())).thenReturn(Optional.of(setupPatient(true)));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(new Doctor()));
        when(diagnosisRepository.findById(any())).thenReturn(Optional.of(new Diagnosis()));
        when(visitRepository.findByDoctorAndDateTime(any(), any(), any())).thenReturn(Optional.of(new Visit()));

        // ACT & ASSERT
        assertThrows(InvalidInputException.class, () -> visitService.create(dto));
    }

    @Test
    void Create_WithNonExistentPatient_ShouldThrowEntityNotFoundException_ErrorCase() {
        VisitCreateDTO dto = new VisitCreateDTO();
        dto.setPatientId(99L);
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> visitService.create(dto));
    }

    // --- Update Tests ---

    @Test
    void Update_WithValidData_ShouldSucceed_HappyPath() {
        // ARRANGE
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

        // ACT
        VisitViewDTO result = visitService.update(dto);

        // ASSERT
        assertNotNull(result);
        verify(visitRepository).save(any(Visit.class));
    }

    @Test
    void Update_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> visitService.update(new VisitUpdateDTO()));
    }

    @Test
    void Update_WithNonExistentVisit_ShouldThrowEntityNotFoundException_ErrorCase() {
        VisitUpdateDTO dto = new VisitUpdateDTO();
        dto.setId(99L);
        when(visitRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> visitService.update(dto));
    }

    // --- Delete Tests ---

    @Test
    void Delete_WithExistingId_ShouldSucceed_HappyPath() {
        when(visitRepository.existsById(1L)).thenReturn(true);
        visitService.delete(1L);
        verify(visitRepository).deleteById(1L);
    }

    @Test
    void Delete_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
        when(visitRepository.existsById(99L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> visitService.delete(99L));
    }

    // --- GetById Tests ---

    @Test
    void GetById_AsPatientOwner_ShouldSucceed_HappyPath() {
        // ARRANGE
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("patient-owner-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
        );
        Visit visit = new Visit();
        visit.setPatient(setupPatient(true));

        when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
        when(modelMapper.map(visit, VisitViewDTO.class)).thenReturn(new VisitViewDTO());

        // ACT & ASSERT
        assertDoesNotThrow(() -> visitService.getById(1L));
    }

    @Test
    void GetById_AsOtherPatient_ShouldThrowAccessDeniedException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("other-patient-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
        );
        Visit visit = new Visit();
        visit.setPatient(setupPatient(true)); // Belongs to patient-owner-id

        when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> visitService.getById(1L));
    }

    // --- Reporting and Find Tests ---

    @Test
    void GetVisitsByPatient_AsPatientOwner_ShouldSucceed_HappyPath() {
        // ARRANGE
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("patient-owner-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
        );
        Patient patient = setupPatient(true);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(visitRepository.findByPatient(any(), any())).thenReturn(Page.empty());

        // ACT & ASSERT
        assertDoesNotThrow(() -> visitService.getVisitsByPatient(1L, 0, 10));
    }

    @Test
    void GetVisitsByPatient_AsOtherPatient_ShouldThrowAccessDeniedException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("other-patient-id", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
        );
        Patient patient = setupPatient(true); // Belongs to patient-owner-id

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> visitService.getVisitsByPatient(1L, 0, 10));
    }

    @Test
    void GetVisitsByDateRange_WithInvalidRange_ShouldThrowInvalidInputException_ErrorCase() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(1);
        assertThrows(InvalidInputException.class, () -> visitService.getVisitsByDateRange(start, end, 0, 10));
    }

    // --- GetAll Test ---

    @Test
    void GetAll_WithFilter_ShouldCallCorrectRepositoryMethod_HappyPath() {
        // ARRANGE
        String filter = "test-filter";
        when(visitRepository.findByPatientOrDoctorFilter(anyString(), any())).thenReturn(Page.empty());

        // ACT
        CompletableFuture<Page<VisitViewDTO>> future = visitService.getAll(0, 10, "visitDate", true, filter);
        future.join();

        // ASSERT
        verify(visitRepository).findByPatientOrDoctorFilter(eq(filter), any(Pageable.class));
        verify(visitRepository, never()).findAll(any(Pageable.class));
    }
}