package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.services.data.dtos.DiagnosisCreateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisUpdateDTO;
import nbu.cscb869.services.data.dtos.DiagnosisViewDTO;
import nbu.cscb869.services.services.DiagnosisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagnosisServiceUnitTests {
    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private DiagnosisServiceImpl diagnosisService;

    private Diagnosis diagnosis;
    private DiagnosisCreateDTO createDTO;
    private DiagnosisUpdateDTO updateDTO;
    private DiagnosisViewDTO viewDTO;

    @BeforeEach
    void setUp() {
        diagnosis = new Diagnosis();
        diagnosis.setId(1L);
        diagnosis.setName("Flu");
        diagnosis.setDescription("Viral infection");

        createDTO = DiagnosisCreateDTO.builder()
                .name("Flu")
                .description("Viral infection")
                .build();

        updateDTO = DiagnosisUpdateDTO.builder()
                .id(1L)
                .name("Updated Flu")
                .description("Updated infection")
                .build();

        viewDTO = DiagnosisViewDTO.builder()
                .id(1L)
                .name("Flu")
                .description("Viral infection")
                .build();
    }

    // Happy Path
    @Test
    void Create_ValidDTO_SavesSuccessfully() {
        when(modelMapper.map(createDTO, Diagnosis.class)).thenReturn(diagnosis);
        when(diagnosisRepository.save(diagnosis)).thenReturn(diagnosis);
        when(modelMapper.map(diagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        DiagnosisViewDTO result = diagnosisService.create(createDTO);

        assertEquals(viewDTO, result);
        verify(modelMapper).map(createDTO, Diagnosis.class);
        verify(diagnosisRepository).save(diagnosis);
        verify(modelMapper).map(diagnosis, DiagnosisViewDTO.class);
    }

    @Test
    void Update_ValidDTO_UpdatesSuccessfully() {
        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(diagnosis));
        doAnswer(invocation -> {
            DiagnosisUpdateDTO dto = invocation.getArgument(0);
            Diagnosis target = invocation.getArgument(1);
            target.setName(dto.getName());
            target.setDescription(dto.getDescription());
            return null;
        }).when(modelMapper).map(updateDTO, diagnosis);
        when(diagnosisRepository.save(diagnosis)).thenReturn(diagnosis);
        when(modelMapper.map(diagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        DiagnosisViewDTO result = diagnosisService.update(updateDTO);

        assertEquals(viewDTO, result);
        verify(diagnosisRepository).findById(1L);
        verify(modelMapper).map(updateDTO, diagnosis);
        verify(diagnosisRepository).save(diagnosis);
        verify(modelMapper).map(diagnosis, DiagnosisViewDTO.class);
    }

    @Test
    void Delete_ExistingId_SoftDeletesSuccessfully() {
        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(diagnosis));
        doNothing().when(diagnosisRepository).delete(diagnosis);

        diagnosisService.delete(1L);

        verify(diagnosisRepository).findById(1L);
        verify(diagnosisRepository).delete(diagnosis);
    }

    @Test
    void GetById_ExistingId_ReturnsDiagnosis() {
        when(diagnosisRepository.findById(1L)).thenReturn(Optional.of(diagnosis));
        when(modelMapper.map(diagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        DiagnosisViewDTO result = diagnosisService.getById(1L);

        assertEquals(viewDTO, result);
        verify(diagnosisRepository).findById(1L);
        verify(modelMapper).map(diagnosis, DiagnosisViewDTO.class);
    }

    @Test
    void GetAll_ValidParameters_ReturnsPaged() {
        Page<Diagnosis> page = new PageImpl<>(Collections.singletonList(diagnosis));
        when(diagnosisRepository.findByNameContainingIgnoreCase(eq("Pneum"), any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(diagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        Page<DiagnosisViewDTO> result = diagnosisService.getAll(0, 10, "name", true, "Pneum").join();

        assertEquals(1, result.getTotalElements());
        assertEquals(viewDTO, result.getContent().get(0));
        verify(diagnosisRepository).findByNameContainingIgnoreCase(eq("Pneum"), any(Pageable.class));
        verify(modelMapper).map(diagnosis, DiagnosisViewDTO.class);
    }

    // Error Cases
    @Test
    void Create_NullDTO_ThrowsInvalidDTOException() {
        assertThrows(InvalidDTOException.class, () -> diagnosisService.create(null));
        verifyNoInteractions(diagnosisRepository, modelMapper);
    }

    @Test
    void Update_NullDTO_ThrowsInvalidDTOException() {
        assertThrows(InvalidDTOException.class, () -> diagnosisService.update(null));
        verifyNoInteractions(diagnosisRepository, modelMapper);
    }

    @Test
    void Update_NullId_ThrowsInvalidDTOException() {
        updateDTO.setId(null);
        assertThrows(InvalidDTOException.class, () -> diagnosisService.update(updateDTO));
        verifyNoInteractions(diagnosisRepository, modelMapper);
    }

    @Test
    void Update_NonExistentId_ThrowsEntityNotFoundException() {
        when(diagnosisRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> diagnosisService.update(updateDTO));
        verify(diagnosisRepository).findById(1L);
        verifyNoMoreInteractions(diagnosisRepository, modelMapper);
    }

    @Test
    void Delete_NullId_ThrowsInvalidDTOException() {
        assertThrows(InvalidDTOException.class, () -> diagnosisService.delete(null));
        verifyNoInteractions(diagnosisRepository);
    }

    @Test
    void Delete_NonExistentId_ThrowsEntityNotFoundException() {
        when(diagnosisRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> diagnosisService.delete(999L));
        verify(diagnosisRepository).findById(999L);
        verifyNoMoreInteractions(diagnosisRepository);
    }

    @Test
    void GetById_NullId_ThrowsInvalidDTOException() {
        assertThrows(InvalidDTOException.class, () -> diagnosisService.getById(null));
        verifyNoInteractions(diagnosisRepository, modelMapper);
    }

    @Test
    void GetById_NonExistentId_ThrowsEntityNotFoundException() {
        when(diagnosisRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> diagnosisService.getById(999L));
        verify(diagnosisRepository).findById(999L);
        verifyNoMoreInteractions(diagnosisRepository, modelMapper);
    }

    @Test
    void GetAll_NegativePage_ThrowsInvalidDTOException() {
        assertThrows(InvalidDTOException.class, () -> diagnosisService.getAll(-1, 10, "name", true, null).join());
        verifyNoInteractions(diagnosisRepository, modelMapper);
    }

    @Test
    void GetAll_ZeroPageSize_ThrowsInvalidDTOException() {
        assertThrows(InvalidDTOException.class, () -> diagnosisService.getAll(0, 0, "name", true, null).join());
        verifyNoInteractions(diagnosisRepository, modelMapper);
    }

    @Test
    void GetAll_ExcessivePageSize_ThrowsInvalidDTOException() {
        assertThrows(InvalidDTOException.class, () -> diagnosisService.getAll(0, 101, "name", true, null).join());
        verifyNoInteractions(diagnosisRepository, modelMapper);
    }

    // Edge Cases
    @Test
    void GetAll_EmptyFilter_ReturnsAll() {
        Page<Diagnosis> page = new PageImpl<>(Collections.singletonList(diagnosis));
        when(diagnosisRepository.findAllActive(any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(diagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        Page<DiagnosisViewDTO> result = diagnosisService.getAll(0, 10, "name", true, "").join();

        assertEquals(1, result.getTotalElements());
        assertEquals(viewDTO, result.getContent().get(0));
        verify(diagnosisRepository).findAllActive(any(Pageable.class));
        verify(modelMapper).map(diagnosis, DiagnosisViewDTO.class);
    }

    @Test
    void GetAll_NonExistentFilter_ReturnsEmpty() {
        Page<Diagnosis> emptyPage = new PageImpl<>(Collections.emptyList());
        when(diagnosisRepository.findByNameContainingIgnoreCase(eq("Nonexistent"), any(Pageable.class))).thenReturn(emptyPage);

        Page<DiagnosisViewDTO> result = diagnosisService.getAll(0, 10, "name", true, "Nonexistent").join();

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(diagnosisRepository).findByNameContainingIgnoreCase(eq("Nonexistent"), any(Pageable.class));
    }

    @Test
    void GetAll_MaximumPageSize_ReturnsPaged() {
        Page<Diagnosis> page = new PageImpl<>(Collections.singletonList(diagnosis));
        when(diagnosisRepository.findAllActive(any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(diagnosis, DiagnosisViewDTO.class)).thenReturn(viewDTO);

        Page<DiagnosisViewDTO> result = diagnosisService.getAll(0, 100, "name", true, "").join();

        assertEquals(1, result.getTotalElements());
        assertEquals(viewDTO, result.getContent().get(0));
        verify(diagnosisRepository).findAllActive(any(Pageable.class));
        verify(modelMapper).map(diagnosis, DiagnosisViewDTO.class);
    }
}