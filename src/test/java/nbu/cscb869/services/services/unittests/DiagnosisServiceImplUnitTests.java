package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.PatientDiagnosisDTO;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.DiagnosisServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagnosisServiceImplUnitTests {

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private DiagnosisServiceImpl diagnosisService;

    // --- Create Tests ---

    @Test
    void create_WithValidDTO_ShouldSucceed_HappyPath() {
        // ARRANGE
        DiagnosisCreateDTO createDTO = new DiagnosisCreateDTO("Flu", "Viral infection");
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setId(1L);
        DiagnosisViewDTO viewDTO = new DiagnosisViewDTO();

        when(diagnosisRepository.findByName("Flu")).thenReturn(Optional.empty());
        when(modelMapper.map(createDTO, Diagnosis.class)).thenReturn(diagnosis);
        when(diagnosisRepository.save(diagnosis)).thenReturn(diagnosis);
        when(modelMapper.map(diagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        // ACT
        DiagnosisViewDTO result = diagnosisService.create(createDTO);

        // ASSERT
        assertNotNull(result);
        verify(diagnosisRepository).save(diagnosis);
    }

    @Test
    void create_WithNullDTO_ShouldThrowException_ErrorCase() {
        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> diagnosisService.create(null));
        verify(diagnosisRepository, never()).save(any());
    }

    @Test
    void create_WithExistingName_ShouldThrowException_ErrorCase() {
        // ARRANGE
        DiagnosisCreateDTO createDTO = new DiagnosisCreateDTO("Flu", "Viral infection");
        when(diagnosisRepository.findByName("Flu")).thenReturn(Optional.of(new Diagnosis()));

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> diagnosisService.create(createDTO));
        verify(diagnosisRepository, never()).save(any());
    }

    // --- Update Tests ---

    @Test
    void update_WithValidDTO_ShouldSucceed_HappyPath() {
        // ARRANGE
        DiagnosisUpdateDTO updateDTO = new DiagnosisUpdateDTO(1L, "Influenza", "A serious viral infection");
        Diagnosis existingDiagnosis = new Diagnosis();
        existingDiagnosis.setId(1L);
        existingDiagnosis.setName("Old Name"); // Set an initial name
        DiagnosisViewDTO viewDTO = new DiagnosisViewDTO();

        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(existingDiagnosis));
        when(diagnosisRepository.findByName("Influenza")).thenReturn(Optional.empty());
        // Mock the map from DTO to existing entity
        doAnswer(invocation -> {
            DiagnosisUpdateDTO source = invocation.getArgument(0);
            Diagnosis destination = invocation.getArgument(1);
            destination.setName(source.getName());
            destination.setDescription(source.getDescription());
            return null;
        }).when(modelMapper).map(any(DiagnosisUpdateDTO.class), eq(existingDiagnosis));
        
        when(diagnosisRepository.save(existingDiagnosis)).thenReturn(existingDiagnosis);
        when(modelMapper.map(existingDiagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        // ACT
        DiagnosisViewDTO result = diagnosisService.update(updateDTO);

        // ASSERT
        assertNotNull(result);
        verify(diagnosisRepository).save(existingDiagnosis);
        verify(modelMapper).map(updateDTO, existingDiagnosis);
    }

    @Test
    void update_WithNonExistentId_ShouldThrowException_ErrorCase() {
        // ARRANGE
        DiagnosisUpdateDTO updateDTO = new DiagnosisUpdateDTO(99L, "Flu", "");
        when(diagnosisRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> diagnosisService.update(updateDTO));
        verify(diagnosisRepository, never()).save(any());
    }

    @Test
    void update_ToExistingName_ShouldThrowException_ErrorCase() {
        // ARRANGE
        DiagnosisUpdateDTO updateDTO = new DiagnosisUpdateDTO(1L, "Flu", "");
        Diagnosis diagnosisToUpdate = new Diagnosis();
        diagnosisToUpdate.setId(1L);
        Diagnosis existingDiagnosisWithSameName = new Diagnosis();
        existingDiagnosisWithSameName.setId(2L); // Different ID

        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(diagnosisToUpdate));
        when(diagnosisRepository.findByName("Flu")).thenReturn(Optional.of(existingDiagnosisWithSameName));

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> diagnosisService.update(updateDTO));
        verify(diagnosisRepository, never()).save(any());
    }

    // --- Delete Tests ---

    @Test
    void delete_WithUnusedDiagnosis_ShouldSucceed_HappyPath() {
        // ARRANGE
        Long diagnosisId = 1L;
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setId(diagnosisId);

        when(diagnosisRepository.findById(diagnosisId)).thenReturn(Optional.of(diagnosis));
        when(visitRepository.findByDiagnosis(eq(diagnosis), any(Pageable.class))).thenReturn(Page.empty());

        // ACT
        diagnosisService.delete(diagnosisId);

        // ASSERT
        verify(diagnosisRepository).delete(diagnosis);
    }

    @Test
    void delete_WithDiagnosisInUse_ShouldThrowException_ErrorCase() {
        // ARRANGE
        Long diagnosisId = 1L;
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setId(diagnosisId);

        when(diagnosisRepository.findById(diagnosisId)).thenReturn(Optional.of(diagnosis));
        when(visitRepository.findByDiagnosis(eq(diagnosis), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(new Visit())));

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> diagnosisService.delete(diagnosisId));
        verify(diagnosisRepository, never()).delete(any());
    }

    @Test
    void delete_WithNonExistentId_ShouldThrowException_ErrorCase() {
        // ARRANGE
        Long diagnosisId = 99L;
        when(diagnosisRepository.findById(diagnosisId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> diagnosisService.delete(diagnosisId));
        verify(diagnosisRepository, never()).delete(any());
    }

    // --- GetById Tests ---

    @Test
    void getById_WithExistingId_ShouldReturnDiagnosisViewDTO_HappyPath() {
        // ARRANGE
        Long id = 1L;
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setId(id);
        DiagnosisViewDTO viewDTO = new DiagnosisViewDTO();

        when(diagnosisRepository.findById(id)).thenReturn(Optional.of(diagnosis));
        when(modelMapper.map(diagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        // ACT
        DiagnosisViewDTO result = diagnosisService.getById(id);

        // ASSERT
        assertNotNull(result);
        verify(diagnosisRepository).findById(id);
    }

    @Test
    void getById_WithNonExistentId_ShouldThrowException_ErrorCase() {
        // ARRANGE
        Long id = 99L;
        when(diagnosisRepository.findById(id)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> diagnosisService.getById(id));
    }

    @Test
    void getById_WithNullId_ShouldThrowException_ErrorCase() {
        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> diagnosisService.getById(null));
    }

    // --- GetByName Tests ---

    @Test
    void getByName_WithExistingName_ShouldReturnDiagnosisViewDTO_HappyPath() {
        // ARRANGE
        String name = "Flu";
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName(name);
        DiagnosisViewDTO viewDTO = new DiagnosisViewDTO();

        when(diagnosisRepository.findByName(name)).thenReturn(Optional.of(diagnosis));
        when(modelMapper.map(diagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        // ACT
        DiagnosisViewDTO result = diagnosisService.getByName(name);

        // ASSERT
        assertNotNull(result);
        verify(diagnosisRepository).findByName(name);
    }

    @Test
    void getByName_WithNonExistentName_ShouldThrowException_ErrorCase() {
        // ARRANGE
        String name = "NonExistent";
        when(diagnosisRepository.findByName(name)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> diagnosisService.getByName(name));
    }

    @Test
    void getByName_WithNullName_ShouldThrowException_ErrorCase() {
        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> diagnosisService.getByName(null));
    }

    @Test
    void getByName_WithEmptyName_ShouldThrowException_ErrorCase() {
        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> diagnosisService.getByName(""));
    }

    // --- GetAll Tests ---

    @Test
    void getAll_WithValidPagination_ShouldReturnPage_HappyPath() throws ExecutionException, InterruptedException {
        // ARRANGE
        int page = 0;
        int size = 10;
        String orderBy = "name";
        boolean ascending = true;
        String filter = null;
        Page<Diagnosis> diagnosisPage = new PageImpl<>(Collections.singletonList(new Diagnosis()));
        DiagnosisViewDTO viewDTO = new DiagnosisViewDTO();

        when(diagnosisRepository.findAll(any(Pageable.class))).thenReturn(diagnosisPage);
        when(modelMapper.map(any(Diagnosis.class), eq(DiagnosisViewDTO.class))).thenReturn(viewDTO);

        // ACT
        CompletableFuture<Page<DiagnosisViewDTO>> resultFuture = diagnosisService.getAll(page, size, orderBy, ascending, filter);
        Page<DiagnosisViewDTO> result = resultFuture.get();

        // ASSERT
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(diagnosisRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAll_WithFilter_ShouldReturnFilteredPage_HappyPath() throws ExecutionException, InterruptedException {
        // ARRANGE
        int page = 0;
        int size = 10;
        String orderBy = "name";
        boolean ascending = true;
        String filter = "flu";
        Page<Diagnosis> diagnosisPage = new PageImpl<>(Collections.singletonList(new Diagnosis()));
        DiagnosisViewDTO viewDTO = new DiagnosisViewDTO();

        when(diagnosisRepository.findByNameContainingIgnoreCase(eq(filter), any(Pageable.class))).thenReturn(diagnosisPage);
        when(modelMapper.map(any(Diagnosis.class), eq(DiagnosisViewDTO.class))).thenReturn(viewDTO);

        // ACT
        CompletableFuture<Page<DiagnosisViewDTO>> resultFuture = diagnosisService.getAll(page, size, orderBy, ascending, filter);
        Page<DiagnosisViewDTO> result = resultFuture.get();

        // ASSERT
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(diagnosisRepository).findByNameContainingIgnoreCase(eq(filter), any(Pageable.class));
    }

    @Test
    void getAll_WithInvalidPagination_ShouldThrowException_ErrorCase() {
        // ARRANGE
        int page = -1;
        int size = 0;
        String orderBy = "name";
        boolean ascending = true;
        String filter = null;

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> diagnosisService.getAll(page, size, orderBy, ascending, filter));
        verify(diagnosisRepository, never()).findAll(any(Pageable.class));
    }

    // --- GetPatientsByDiagnosis Tests ---

    @Test
    void getPatientsByDiagnosis_WithValidId_ShouldReturnPage_HappyPath() {
        // ARRANGE
        Long diagnosisId = 1L;
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setId(diagnosisId);
        Page<PatientDiagnosisDTO> patientDiagnosisPage = new PageImpl<>(Collections.singletonList(PatientDiagnosisDTO.builder().build()));

        when(diagnosisRepository.findById(diagnosisId)).thenReturn(Optional.of(diagnosis));
        when(diagnosisRepository.findPatientsByDiagnosis(eq(diagnosis), any(Pageable.class))).thenReturn(patientDiagnosisPage);

        // ACT
        Page<PatientDiagnosisDTO> result = diagnosisService.getPatientsByDiagnosis(diagnosisId, 0, 10);

        // ASSERT
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(diagnosisRepository).findById(diagnosisId);
        verify(diagnosisRepository).findPatientsByDiagnosis(eq(diagnosis), any(Pageable.class));
    }

    @Test
    void getPatientsByDiagnosis_WithNonExistentId_ShouldThrowException_ErrorCase() {
        // ARRANGE
        Long diagnosisId = 99L;
        when(diagnosisRepository.findById(diagnosisId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> diagnosisService.getPatientsByDiagnosis(diagnosisId, 0, 10));
        verify(diagnosisRepository, never()).findPatientsByDiagnosis(any(), any(Pageable.class));
    }

    @Test
    void getPatientsByDiagnosis_WithNullId_ShouldThrowException_ErrorCase() {
        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> diagnosisService.getPatientsByDiagnosis(null, 0, 10));
        verify(diagnosisRepository, never()).findPatientsByDiagnosis(any(), any(Pageable.class));
    }

    // --- GetMostFrequentDiagnoses Tests ---

    @Test
    void getMostFrequentDiagnoses_ShouldReturnList_HappyPath() {
        // ARRANGE
        List<DiagnosisVisitCountDTO> dtoList = Collections.singletonList(new DiagnosisVisitCountDTO(1L, "Flu", 10L));
        when(diagnosisRepository.findMostFrequentDiagnoses()).thenReturn(dtoList);

        // ACT
        List<DiagnosisVisitCountDTO> result = diagnosisService.getMostFrequentDiagnoses();

        // ASSERT
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(diagnosisRepository).findMostFrequentDiagnoses();
    }

    @Test
    void getMostFrequentDiagnoses_WithNoData_ShouldReturnEmptyList_EdgeCase() {
        // ARRANGE
        when(diagnosisRepository.findMostFrequentDiagnoses()).thenReturn(Collections.emptyList());

        // ACT
        List<DiagnosisVisitCountDTO> result = diagnosisService.getMostFrequentDiagnoses();

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(diagnosisRepository).findMostFrequentDiagnoses();
    }
}
