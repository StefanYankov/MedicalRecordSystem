package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidDoctorException;
import nbu.cscb869.common.exceptions.InvalidInputException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.services.DoctorServiceImpl;
import nbu.cscb869.services.services.utility.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplUnitTests {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    // --- Create Tests ---

    @Test
    void create_WithValidDataAndImage_ShouldSucceed_HappyPath() {
        // ARRANGE
        DoctorCreateDTO createDTO = new DoctorCreateDTO();
        createDTO.setName("Dr. Smith");
        createDTO.setUniqueIdNumber("12345");
        createDTO.setSpecialties(Set.of("Cardiology"));

        MultipartFile mockImage = mock(MultipartFile.class);
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        DoctorViewDTO expectedView = new DoctorViewDTO();
        expectedView.setId(1L);

        Specialty specialty = new Specialty("Cardiology", "Heart stuff", Collections.emptySet());

        when(mockImage.isEmpty()).thenReturn(false);
        when(doctorRepository.findByUniqueIdNumber(anyString())).thenReturn(Optional.empty());
        when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(specialty));
        when(cloudinaryService.uploadImage(mockImage)).thenReturn("http://image.url");
        when(modelMapper.map(createDTO, Doctor.class)).thenReturn(doctor);
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);
        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(expectedView);

        // ACT
        DoctorViewDTO result = doctorService.create(createDTO, mockImage);

        // ASSERT
        assertNotNull(result);
        assertEquals(expectedView.getId(), result.getId());
        verify(cloudinaryService).uploadImage(mockImage);
        verify(doctorRepository).save(doctor);
    }

    @Test
    void create_WithNullDto_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> doctorService.create(null, null));
    }

    @Test
    void create_WithExistingUniqueId_ShouldThrowInvalidDoctorException_ErrorCase() {
        // ARRANGE
        DoctorCreateDTO createDTO = new DoctorCreateDTO();
        createDTO.setName("Dr. Smith");
        createDTO.setUniqueIdNumber("12345");

        when(doctorRepository.findByUniqueIdNumber("12345")).thenReturn(Optional.of(new Doctor()));

        // ACT & ASSERT
        assertThrows(InvalidDoctorException.class, () -> doctorService.create(createDTO, null));
    }

    @Test
    void create_WithNonExistentSpecialty_ShouldThrowEntityNotFoundException_ErrorCase() {
        // ARRANGE
        DoctorCreateDTO createDTO = new DoctorCreateDTO();
        createDTO.setName("Dr. Smith");
        createDTO.setUniqueIdNumber("12345");
        createDTO.setSpecialties(Set.of("FakeSpecialty"));

        when(doctorRepository.findByUniqueIdNumber(anyString())).thenReturn(Optional.empty());
        when(specialtyRepository.findByName("FakeSpecialty")).thenReturn(Optional.empty());
        when(modelMapper.map(createDTO, Doctor.class)).thenReturn(new Doctor());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> doctorService.create(createDTO, null));
    }

    @Test
    void create_WithImageUploadFailure_ShouldThrowInvalidInputException_ErrorCase() {
        // ARRANGE
        DoctorCreateDTO createDTO = new DoctorCreateDTO();
        createDTO.setName("Dr. Smith");
        createDTO.setUniqueIdNumber("12345");

        MultipartFile mockImage = mock(MultipartFile.class);
        when(mockImage.isEmpty()).thenReturn(false);
        when(doctorRepository.findByUniqueIdNumber(anyString())).thenReturn(Optional.empty());
        when(modelMapper.map(createDTO, Doctor.class)).thenReturn(new Doctor());
        // Throw an unchecked exception, as the service's catch block handles generic Exception
        when(cloudinaryService.uploadImage(mockImage)).thenThrow(new RuntimeException("Upload failed"));

        // ACT & ASSERT
        assertThrows(InvalidInputException.class, () -> doctorService.create(createDTO, mockImage));
    }

    // --- Update Tests ---

    @Test
    void update_WithValidData_ShouldSucceed_HappyPath() {
        // ARRANGE
        DoctorUpdateDTO updateDTO = new DoctorUpdateDTO();
        updateDTO.setId(1L);
        updateDTO.setName("Dr. John Smith");
        updateDTO.setUniqueIdNumber("12345");
        updateDTO.setSpecialties(Set.of("Surgery"));

        Doctor existingDoctor = new Doctor();
        existingDoctor.setId(1L);
        existingDoctor.setUniqueIdNumber("12345");
        DoctorViewDTO expectedView = new DoctorViewDTO();
        Specialty specialty = new Specialty("Surgery", "", Collections.emptySet());

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(existingDoctor));
        when(specialtyRepository.findByName("Surgery")).thenReturn(Optional.of(specialty));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(existingDoctor);
        when(modelMapper.map(existingDoctor, DoctorViewDTO.class)).thenReturn(expectedView);

        // ACT
        DoctorViewDTO result = doctorService.update(updateDTO, null);

        // ASSERT
        assertNotNull(result);
        verify(doctorRepository).save(existingDoctor);
        assertEquals("Dr. John Smith", existingDoctor.getName());
    }

    @Test
    void update_WithNullDto_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> doctorService.update(null, null));
    }

    @Test
    void update_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> doctorService.update(new DoctorUpdateDTO(), null));
    }

    @Test
    void update_WithNonExistentDoctor_ShouldThrowEntityNotFoundException_ErrorCase() {
        // ARRANGE
        DoctorUpdateDTO updateDTO = new DoctorUpdateDTO();
        updateDTO.setId(99L);
        updateDTO.setName("Non Existent");
        updateDTO.setUniqueIdNumber("99999");

        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> doctorService.update(updateDTO, null));
    }

    @Test
    void update_WithDuplicateUniqueId_ShouldThrowInvalidDoctorException_ErrorCase() {
        // ARRANGE
        DoctorUpdateDTO updateDTO = new DoctorUpdateDTO();
        updateDTO.setId(1L);
        updateDTO.setName("Dr. Smith");
        updateDTO.setUniqueIdNumber("54321");

        Doctor existingDoctor = new Doctor();
        existingDoctor.setId(1L);
        existingDoctor.setUniqueIdNumber("12345");

        Doctor otherDoctor = new Doctor();
        otherDoctor.setId(2L);
        otherDoctor.setUniqueIdNumber("54321");

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(existingDoctor));
        when(doctorRepository.findByUniqueIdNumber("54321")).thenReturn(Optional.of(otherDoctor));

        // ACT & ASSERT
        assertThrows(InvalidDoctorException.class, () -> doctorService.update(updateDTO, null));
    }

    // --- Delete Tests ---

    @Test
    void delete_WithValidId_ShouldSucceed_HappyPath() {
        // ARRANGE
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setGeneralPractitioner(false);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        doNothing().when(doctorRepository).delete(doctor);

        // ACT
        doctorService.delete(1L);

        // ASSERT
        verify(doctorRepository).delete(doctor);
    }

    @Test
    void delete_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> doctorService.delete(null));
    }

    @Test
    void delete_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> doctorService.delete(99L));
    }

    @Test
    void delete_WithGeneralPractitionerWithPatients_ShouldThrowInvalidDoctorException_ErrorCase() {
        // ARRANGE
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setGeneralPractitioner(true);

        DoctorPatientCountDTO countDTO = mock(DoctorPatientCountDTO.class);
        when(countDTO.getDoctor()).thenReturn(doctor);
        when(countDTO.getPatientCount()).thenReturn(5L);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorRepository.findPatientCountByGeneralPractitioner()).thenReturn(List.of(countDTO));

        // ACT & ASSERT
        assertThrows(InvalidDoctorException.class, () -> doctorService.delete(1L));
    }

    // --- GetById/GetByUniqueIdNumber Tests ---

    @Test
    void getById_WithExistingId_ShouldReturnDoctor_HappyPath() {
        // ARRANGE
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        DoctorViewDTO expectedView = new DoctorViewDTO();
        expectedView.setId(1L);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(expectedView);

        // ACT
        DoctorViewDTO result = doctorService.getById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_WithNullId_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> doctorService.getById(null));
    }

    @Test
    void getById_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> doctorService.getById(99L));
    }

    @Test
    void getByUniqueIdNumber_WithExistingId_ShouldReturnDoctor_HappyPath() {
        Doctor doctor = new Doctor();
        doctor.setUniqueIdNumber("12345");
        DoctorViewDTO expectedView = new DoctorViewDTO();
        expectedView.setUniqueIdNumber("12345");

        when(doctorRepository.findByUniqueIdNumber("12345")).thenReturn(Optional.of(doctor));
        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(expectedView);

        DoctorViewDTO result = doctorService.getByUniqueIdNumber("12345");

        assertNotNull(result);
        assertEquals("12345", result.getUniqueIdNumber());
    }

    // --- GetAllAsync Tests ---

    @Test
    void getAllAsync_WithNoFilter_ShouldReturnAllDoctors_HappyPath() throws ExecutionException, InterruptedException {
        // ARRANGE
        Page<Doctor> doctorPage = new PageImpl<>(List.of(new Doctor()));
        when(doctorRepository.findAll(any(Pageable.class))).thenReturn(doctorPage);
        when(modelMapper.map(any(Doctor.class), eq(DoctorViewDTO.class))).thenReturn(new DoctorViewDTO());

        // ACT
        CompletableFuture<Page<DoctorViewDTO>> future = doctorService.getAllAsync(0, 10, "name", true, null);
        Page<DoctorViewDTO> result = future.get();

        // ASSERT
        assertEquals(1, result.getTotalElements());
        verify(doctorRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllAsync_WithFilter_ShouldReturnFilteredDoctors_HappyPath() throws ExecutionException, InterruptedException {
        // ARRANGE
        String filter = "123";
        Page<Doctor> doctorPage = new PageImpl<>(List.of(new Doctor()));
        when(doctorRepository.findByUniqueIdNumberContaining(contains(filter), any(Pageable.class))).thenReturn(doctorPage);
        when(modelMapper.map(any(Doctor.class), eq(DoctorViewDTO.class))).thenReturn(new DoctorViewDTO());

        // ACT
        CompletableFuture<Page<DoctorViewDTO>> future = doctorService.getAllAsync(0, 10, "name", true, filter);
        Page<DoctorViewDTO> result = future.get();

        // ASSERT
        assertEquals(1, result.getTotalElements());
        verify(doctorRepository).findByUniqueIdNumberContaining(contains(filter), any(Pageable.class));
    }

    @Test
    void getAllAsync_WithInvalidPagination_ShouldThrowInvalidDTOException_ErrorCase() {
        assertThrows(InvalidDTOException.class, () -> doctorService.getAllAsync(-1, 10, "name", true, null));
    }

    // --- Reporting Tests ---

    @Test
    void getPatientsByGeneralPractitioner_WithValidId_ShouldReturnPage_HappyPath() {
        Doctor gp = new Doctor();
        gp.setId(1L);
        gp.setGeneralPractitioner(true);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(gp));
        when(doctorRepository.findPatientsByGeneralPractitioner(any(), any())).thenReturn(Page.empty());

        doctorService.getPatientsByGeneralPractitioner(1L, 0, 10);

        verify(doctorRepository).findPatientsByGeneralPractitioner(any(), any());
    }

    @Test
    void getPatientsByGeneralPractitioner_WithNonGp_ShouldThrowInvalidDoctorException_ErrorCase() {
        Doctor notGp = new Doctor();
        notGp.setId(1L);
        notGp.setGeneralPractitioner(false);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(notGp));

        assertThrows(InvalidDoctorException.class, () -> doctorService.getPatientsByGeneralPractitioner(1L, 0, 10));
    }

    @Test
    void getPatientCountByGeneralPractitioner_ShouldReturnCounts_HappyPath() {
        // ARRANGE
        List<DoctorPatientCountDTO> expectedCounts = List.of(mock(DoctorPatientCountDTO.class));
        when(doctorRepository.findPatientCountByGeneralPractitioner()).thenReturn(expectedCounts);

        // ACT
        List<DoctorPatientCountDTO> result = doctorService.getPatientCountByGeneralPractitioner();

        // ASSERT
        assertFalse(result.isEmpty());
        verify(doctorRepository).findPatientCountByGeneralPractitioner();
    }

    @Test
    void getVisitCount_ShouldReturnCounts_HappyPath() {
        when(doctorRepository.findVisitCountByDoctor()).thenReturn(List.of(mock(DoctorVisitCountDTO.class)));
        List<DoctorVisitCountDTO> result = doctorService.getVisitCount();
        assertFalse(result.isEmpty());
        verify(doctorRepository).findVisitCountByDoctor();
    }

    @Test
    void getVisitsByPeriod_WithValidData_ShouldReturnPage_HappyPath() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(1);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(visitRepository.findByDoctorAndDateRange(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(new Visit())));
        when(modelMapper.map(any(Visit.class), eq(VisitViewDTO.class))).thenReturn(new VisitViewDTO());

        Page<VisitViewDTO> result = doctorService.getVisitsByPeriod(1L, start, end, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getVisitsByPeriod_WithInvalidDateRange_ShouldThrowInvalidInputException_ErrorCase() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(1);
        assertThrows(InvalidInputException.class, () -> doctorService.getVisitsByPeriod(1L, start, end, 0, 10));
    }

    @Test
    void getDoctorsWithMostSickLeaves_ShouldReturnList_HappyPath() {
        when(doctorRepository.findDoctorsWithMostSickLeaves()).thenReturn(List.of(mock(DoctorSickLeaveCountDTO.class)));
        List<DoctorSickLeaveCountDTO> result = doctorService.getDoctorsWithMostSickLeaves();
        assertFalse(result.isEmpty());
        verify(doctorRepository).findDoctorsWithMostSickLeaves();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCriteria_ShouldReturnPage_HappyPath() {
        // ARRANGE
        Specification<Doctor> spec = mock(Specification.class);
        // Use matchers for all arguments to fix InvalidUseOfMatchersException
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // ACT
        Page<DoctorViewDTO> result = doctorService.findByCriteria(spec, 0, 10, "name", true);

        // ASSERT
        assertNotNull(result);
        // Verify with matchers as well
        verify(doctorRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
