package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.TreatmentRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.MedicineCreateDTO;
import nbu.cscb869.services.data.dtos.MedicineUpdateDTO;
import nbu.cscb869.services.data.dtos.TreatmentCreateDTO;
import nbu.cscb869.services.data.dtos.TreatmentUpdateDTO;
import nbu.cscb869.services.data.dtos.TreatmentViewDTO;
import nbu.cscb869.services.services.TreatmentServiceImpl;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplUnitTests {

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private TreatmentServiceImpl treatmentService;

    // --- Create Tests ---

    @Test
    void Create_WithValidData_ShouldSucceed_HappyPath() {
        // ARRANGE
        MedicineCreateDTO medDto = new MedicineCreateDTO("Aspirin", "500mg", "Once a day");
        TreatmentCreateDTO createDTO = new TreatmentCreateDTO("Standard treatment", 1L, List.of(medDto));
        Visit visit = new Visit();
        Treatment savedTreatment = new Treatment();

        when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
        when(treatmentRepository.save(any(Treatment.class))).thenReturn(savedTreatment);
        when(modelMapper.map(savedTreatment, TreatmentViewDTO.class)).thenReturn(new TreatmentViewDTO());

        // ACT
        treatmentService.create(createDTO);

        // ASSERT
        ArgumentCaptor<Treatment> treatmentCaptor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(treatmentCaptor.capture());
        Treatment capturedTreatment = treatmentCaptor.getValue();

        assertEquals("Standard treatment", capturedTreatment.getDescription());
        assertEquals(1, capturedTreatment.getMedicines().size());
        assertEquals("Aspirin", capturedTreatment.getMedicines().get(0).getName());
    }

    @Test
    void Create_WithNullMedicinesList_ShouldSucceed_EdgeCase() {
        // ARRANGE
        TreatmentCreateDTO createDTO = new TreatmentCreateDTO("Observation only", 1L, null);
        Visit visit = new Visit();
        Treatment savedTreatment = new Treatment();

        when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
        when(treatmentRepository.save(any(Treatment.class))).thenReturn(savedTreatment);
        when(modelMapper.map(savedTreatment, TreatmentViewDTO.class)).thenReturn(new TreatmentViewDTO());

        // ACT
        treatmentService.create(createDTO);

        // ASSERT
        ArgumentCaptor<Treatment> treatmentCaptor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(treatmentCaptor.capture());
        assertTrue(treatmentCaptor.getValue().getMedicines().isEmpty());
    }

    @Test
    void Create_WithNullDto_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> treatmentService.create(null));
    }

    @Test
    void Create_WithNonExistentVisit_ShouldThrowEntityNotFoundException_ErrorCase() {
        // ARRANGE
        TreatmentCreateDTO createDTO = new TreatmentCreateDTO("desc", 99L, Collections.emptyList());
        when(visitRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> treatmentService.create(createDTO));
    }

    // --- Update Tests ---

    @Test
    void Update_WithValidData_ShouldSucceed_HappyPath() {
        // ARRANGE
        MedicineUpdateDTO medDto = new MedicineUpdateDTO(null, "Ibuprofen", "200mg", "Twice a day");
        TreatmentUpdateDTO updateDTO = new TreatmentUpdateDTO(10L, "Updated desc", 1L, List.of(medDto));
        Treatment existingTreatment = new Treatment();
        existingTreatment.setMedicines(new ArrayList<>()); // Initialize list
        Visit visit = new Visit();

        when(treatmentRepository.findById(10L)).thenReturn(Optional.of(existingTreatment));
        when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
        when(treatmentRepository.save(any(Treatment.class))).thenReturn(existingTreatment);
        when(modelMapper.map(existingTreatment, TreatmentViewDTO.class)).thenReturn(new TreatmentViewDTO());

        // ACT
        treatmentService.update(updateDTO);

        // ASSERT
        ArgumentCaptor<Treatment> treatmentCaptor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(treatmentCaptor.capture());
        Treatment capturedTreatment = treatmentCaptor.getValue();

        assertEquals("Updated desc", capturedTreatment.getDescription());
        assertEquals(1, capturedTreatment.getMedicines().size());
        assertEquals("Ibuprofen", capturedTreatment.getMedicines().get(0).getName());
    }

    @Test
    void Update_WithNullMedicinesList_ShouldClearExistingMedicines_EdgeCase() {
        // ARRANGE
        TreatmentUpdateDTO updateDTO = new TreatmentUpdateDTO(10L, "No more meds", 1L, null);
        Treatment existingTreatment = new Treatment();
        existingTreatment.setMedicines(new ArrayList<>(List.of(new Medicine()))); // Has one medicine
        Visit visit = new Visit();

        when(treatmentRepository.findById(10L)).thenReturn(Optional.of(existingTreatment));
        when(visitRepository.findById(1L)).thenReturn(Optional.of(visit));
        when(treatmentRepository.save(any(Treatment.class))).thenReturn(existingTreatment);

        // ACT
        treatmentService.update(updateDTO);

        // ASSERT
        ArgumentCaptor<Treatment> treatmentCaptor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(treatmentCaptor.capture());
        assertTrue(treatmentCaptor.getValue().getMedicines().isEmpty());
    }

    @Test
    void Update_WithNullDto_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> treatmentService.update(null));
    }

    @Test
    void Update_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> treatmentService.update(new TreatmentUpdateDTO()));
    }

    @Test
    void Update_WithNonExistentTreatment_ShouldThrowEntityNotFoundException_ErrorCase() {
        // ARRANGE
        TreatmentUpdateDTO updateDTO = new TreatmentUpdateDTO(99L, "desc", 1L, Collections.emptyList());
        when(treatmentRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> treatmentService.update(updateDTO));
    }

    @Test
    void Update_WithNonExistentVisit_ShouldThrowEntityNotFoundException_ErrorCase() {
        // ARRANGE
        TreatmentUpdateDTO updateDTO = new TreatmentUpdateDTO(10L, "desc", 99L, Collections.emptyList());
        when(treatmentRepository.findById(10L)).thenReturn(Optional.of(new Treatment()));
        when(visitRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> treatmentService.update(updateDTO));
    }

    // --- Delete Tests ---

    @Test
    void Delete_WithExistingId_ShouldSucceed_HappyPath() {
        // ARRANGE
        when(treatmentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(treatmentRepository).deleteById(1L);

        // ACT
        treatmentService.delete(1L);

        // ASSERT
        verify(treatmentRepository).deleteById(1L);
    }

    @Test
    void Delete_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> treatmentService.delete(null));
    }

    @Test
    void Delete_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
        // ARRANGE
        when(treatmentRepository.existsById(99L)).thenReturn(false);

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> treatmentService.delete(99L));
    }

    // --- GetById Tests ---

    @Test
    void GetById_AsAdmin_ShouldSucceed_HappyPath() {
        // ARRANGE
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        Treatment treatment = new Treatment();
        when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));
        when(modelMapper.map(treatment, TreatmentViewDTO.class)).thenReturn(new TreatmentViewDTO());

        // ACT
        TreatmentViewDTO result = treatmentService.getById(1L);

        // ASSERT
        assertNotNull(result);
        SecurityContextHolder.clearContext();
    }

    @Test
    void GetById_AsPatientOwner_ShouldSucceed_HappyPath() {
        // ARRANGE
        String patientKeycloakId = "patient-owner";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(patientKeycloakId, "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
        );

        Patient patient = new Patient();
        patient.setKeycloakId(patientKeycloakId);
        Visit visit = new Visit();
        visit.setPatient(patient);
        Treatment treatment = new Treatment();
        treatment.setVisit(visit);

        when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));
        when(modelMapper.map(treatment, TreatmentViewDTO.class)).thenReturn(new TreatmentViewDTO());

        // ACT
        TreatmentViewDTO result = treatmentService.getById(1L);

        // ASSERT
        assertNotNull(result);
        SecurityContextHolder.clearContext();
    }

    @Test
    void GetById_AsOtherPatient_ShouldThrowAccessDeniedException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("other-patient", "pass", List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
        );

        Patient patient = new Patient();
        patient.setKeycloakId("patient-owner"); // Different ID
        Visit visit = new Visit();
        visit.setPatient(patient);
        Treatment treatment = new Treatment();
        treatment.setVisit(visit);

        when(treatmentRepository.findById(1L)).thenReturn(Optional.of(treatment));

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> treatmentService.getById(1L));
        SecurityContextHolder.clearContext();
    }

    @Test
    void GetById_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> treatmentService.getById(null));
    }

    // --- GetAll Tests ---

    @Test
    void GetAll_WithValidPagination_ShouldReturnPage_HappyPath() {
        // ARRANGE
        Page<Treatment> treatmentPage = new PageImpl<>(List.of(new Treatment()));
        when(treatmentRepository.findAll(any(Pageable.class))).thenReturn(treatmentPage);
        when(modelMapper.map(any(Treatment.class), eq(TreatmentViewDTO.class))).thenReturn(new TreatmentViewDTO());

        // ACT
        CompletableFuture<Page<TreatmentViewDTO>> future = treatmentService.getAll(0, 10, "description", true);
        Page<TreatmentViewDTO> result = future.join();

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(treatmentRepository).findAll(any(Pageable.class));
    }

    @Test
    void GetAll_WithInvalidPagination_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> treatmentService.getAll(-1, 10, "description", true));
    }
}