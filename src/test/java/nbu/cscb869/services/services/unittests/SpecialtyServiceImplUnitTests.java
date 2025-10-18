package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityInUseException;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.services.data.dtos.SpecialtyCreateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyUpdateDTO;
import nbu.cscb869.services.data.dtos.SpecialtyViewDTO;
import nbu.cscb869.services.services.SpecialtyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceImplUnitTests {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private DoctorRepository doctorRepository; // FIX: Add mock for the new dependency

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private SpecialtyServiceImpl specialtyService;

    // --- Create Tests ---

    @Test
    void Create_WithValidData_ShouldSucceed_HappyPath() {
        // ARRANGE
        SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
        createDTO.setName("Cardiology");
        Specialty specialty = new Specialty();
        SpecialtyViewDTO viewDTO = new SpecialtyViewDTO();

        when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.empty());
        when(modelMapper.map(createDTO, Specialty.class)).thenReturn(specialty);
        when(specialtyRepository.save(specialty)).thenReturn(specialty);
        when(modelMapper.map(specialty, SpecialtyViewDTO.class)).thenReturn(viewDTO);

        // ACT
        SpecialtyViewDTO result = specialtyService.create(createDTO);

        // ASSERT
        assertNotNull(result);
        verify(specialtyRepository).save(specialty);
    }

    @Test
    void Create_WithDuplicateName_ShouldThrowInvalidDTOException_ErrorCase() {
        // ARRANGE
        SpecialtyCreateDTO createDTO = new SpecialtyCreateDTO();
        createDTO.setName("Cardiology");
        when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(new Specialty()));

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> specialtyService.create(createDTO));
    }

    // --- Update Tests ---

    @Test
    void Update_WithValidData_ShouldSucceed_HappyPath() {
        // ARRANGE
        SpecialtyUpdateDTO updateDTO = new SpecialtyUpdateDTO(1L, "Cardiology Updated", "New Desc");
        Specialty existingSpecialty = new Specialty();
        existingSpecialty.setId(1L);

        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(existingSpecialty));
        when(specialtyRepository.findByName("Cardiology Updated")).thenReturn(Optional.empty());
        when(specialtyRepository.save(any(Specialty.class))).thenReturn(existingSpecialty);
        when(modelMapper.map(existingSpecialty, SpecialtyViewDTO.class)).thenReturn(new SpecialtyViewDTO());

        // ACT
        SpecialtyViewDTO result = specialtyService.update(updateDTO);

        // ASSERT
        assertNotNull(result);
        assertEquals("Cardiology Updated", existingSpecialty.getName());
        verify(specialtyRepository).save(existingSpecialty);
    }

    @Test
    void Update_ToSameName_ShouldSucceed_EdgeCase() {
        // ARRANGE
        SpecialtyUpdateDTO updateDTO = new SpecialtyUpdateDTO(1L, "Cardiology", "New Desc");
        Specialty specialty = new Specialty();
        specialty.setId(1L);
        specialty.setName("Cardiology");

        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialty));
        when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(specialty));
        when(specialtyRepository.save(any(Specialty.class))).thenReturn(specialty);
        when(modelMapper.map(specialty, SpecialtyViewDTO.class)).thenReturn(new SpecialtyViewDTO());

        // ACT & ASSERT
        assertDoesNotThrow(() -> specialtyService.update(updateDTO));
    }

    @Test
    void Update_OnlyDescription_ShouldSucceed_EdgeCase() {
        // ARRANGE
        SpecialtyUpdateDTO updateDTO = new SpecialtyUpdateDTO(1L, "Cardiology", "A new description");
        Specialty specialty = new Specialty();
        specialty.setId(1L);
        specialty.setName("Cardiology");
        specialty.setDescription("Old description");

        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialty));
        when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(specialty));
        when(specialtyRepository.save(any(Specialty.class))).thenReturn(specialty);

        // ACT
        specialtyService.update(updateDTO);

        // ASSERT
        assertEquals("A new description", specialty.getDescription());
        verify(specialtyRepository).save(specialty);
    }

    @Test
    void Update_ToNameUsedByAnotherSpecialty_ShouldThrowInvalidDTOException_ErrorCase() {
        // ARRANGE
        SpecialtyUpdateDTO updateDTO = new SpecialtyUpdateDTO(1L, "Neurology", "Desc");
        Specialty specialtyToUpdate = new Specialty();
        specialtyToUpdate.setId(1L);

        Specialty conflictingSpecialty = new Specialty();
        conflictingSpecialty.setId(2L); // Different ID
        conflictingSpecialty.setName("Neurology");

        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialtyToUpdate));
        when(specialtyRepository.findByName("Neurology")).thenReturn(Optional.of(conflictingSpecialty));

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> specialtyService.update(updateDTO));
    }

    @Test
    void Update_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
        // ARRANGE
        SpecialtyUpdateDTO updateDTO = new SpecialtyUpdateDTO(99L, "name", "desc");
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> specialtyService.update(updateDTO));
    }

    // --- Delete Tests ---

    @Test
    void Delete_WithUnusedSpecialty_ShouldSucceed_HappyPath() {
        // ARRANGE
        Specialty specialty = new Specialty();
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialty));
        when(doctorRepository.existsBySpecialtiesContains(specialty)).thenReturn(false); // FIX: Mock the new dependency
        doNothing().when(specialtyRepository).delete(specialty);

        // ACT
        specialtyService.delete(1L);

        // ASSERT
        verify(specialtyRepository).delete(specialty);
    }

    @Test
    void Delete_WithSpecialtyInUse_ShouldThrowEntityInUseException_ErrorCase() {
        // ARRANGE
        Specialty specialty = new Specialty();
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialty));
        when(doctorRepository.existsBySpecialtiesContains(specialty)).thenReturn(true); // FIX: Mock the new dependency

        // ACT & ASSERT
        assertThrows(EntityInUseException.class, () -> specialtyService.delete(1L));
    }

    @Test
    void Delete_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
        // ARRANGE
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> specialtyService.delete(99L));
    }

    // --- GetById Tests ---

    @Test
    void GetById_WithExistingId_ShouldSucceed_HappyPath() {
        // ARRANGE
        Specialty specialty = new Specialty();
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialty));
        when(modelMapper.map(specialty, SpecialtyViewDTO.class)).thenReturn(new SpecialtyViewDTO());

        // ACT
        SpecialtyViewDTO result = specialtyService.getById(1L);

        // ASSERT
        assertNotNull(result);
    }

    @Test
    void GetById_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
        // ARRANGE
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> specialtyService.getById(99L));
    }

    // --- GetAll Tests ---

    @Test
    void GetAll_WhenCalled_ShouldReturnPage_HappyPath() {
        // ARRANGE
        Page<Specialty> specialtyPage = new PageImpl<>(List.of(new Specialty()));
        when(specialtyRepository.findAll(any(Pageable.class))).thenReturn(specialtyPage);
        when(modelMapper.map(any(Specialty.class), eq(SpecialtyViewDTO.class))).thenReturn(new SpecialtyViewDTO());

        // ACT
        CompletableFuture<Page<SpecialtyViewDTO>> future = specialtyService.getAll(0, 10, "name", true);
        Page<SpecialtyViewDTO> result = future.join();

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
