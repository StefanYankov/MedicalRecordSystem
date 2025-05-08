//package nbu.cscb869.services.services.unittests;
//
//import nbu.cscb869.common.exceptions.*;
//import nbu.cscb869.data.dto.DoctorPatientCountDTO;
//import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
//import nbu.cscb869.data.dto.DoctorVisitCountDTO;
//import nbu.cscb869.data.models.Doctor;
//import nbu.cscb869.data.models.Patient;
//import nbu.cscb869.data.models.Specialty;
//import nbu.cscb869.data.models.Visit;
//import nbu.cscb869.data.repositories.DoctorRepository;
//import nbu.cscb869.data.repositories.PatientRepository;
//import nbu.cscb869.data.repositories.SpecialtyRepository;
//import nbu.cscb869.data.repositories.VisitRepository;
//import nbu.cscb869.services.data.dtos.*;
//import nbu.cscb869.services.services.DoctorServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.modelmapper.ModelMapper;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.text.MessageFormat;
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.STRICT_STUBS)
//public class DoctorServiceUnitTests {
//
//    @Mock
//    private DoctorRepository doctorRepository;
//
//    @Mock
//    private SpecialtyRepository specialtyRepository;
//
//    @Mock
//    private PatientRepository patientRepository;
//
//    @Mock
//    private VisitRepository visitRepository;
//
//    @Mock
//    private ModelMapper modelMapper;
//
//    @InjectMocks
//    private DoctorServiceImpl doctorService;
//
//    private Doctor doctor;
//    private DoctorCreateDTO createDTO;
//    private DoctorUpdateDTO updateDTO;
//    private DoctorViewDTO viewDTO;
//    private Specialty specialty;
//    private Set<Long> specialtyIds;
//    private Patient patient;
//    private PatientViewDTO patientViewDTO;
//    private Visit visit;
//    private VisitViewDTO visitViewDTO;
//
//    @BeforeEach
//    void setUp() {
//        doctor = new Doctor();
//        doctor.setId(1L);
//        doctor.setName("Dr. Smith");
//        doctor.setUniqueIdNumber("DOC12345");
//        doctor.setGeneralPractitioner(true);
//        doctor.setIsDeleted(false);
//
//        specialty = new Specialty();
//        specialty.setId(1L);
//        specialty.setName("Cardiology");
//
//        specialtyIds = Set.of(1L);
//
//        createDTO = DoctorCreateDTO.builder()
//                .name("Dr. Smith")
//                .uniqueIdNumber("DOC12345")
//                .isGeneralPractitioner(true)
//                .specialtyIds(specialtyIds)
//                .build();
//
//        updateDTO = DoctorUpdateDTO.builder()
//                .id(1L)
//                .name("Dr. Smith Updated")
//                .isGeneralPractitioner(true)
//                .specialtyIds(specialtyIds)
//                .build();
//
//        viewDTO = DoctorViewDTO.builder()
//                .id(1L)
//                .name("Dr. Smith")
//                .uniqueIdNumber("DOC12345")
//                .isGeneralPractitioner(true)
//                .specialties(Set.of(new SpecialtyViewDTO(1L, "Cardiology", null)))
//                .build();
//
//        patient = new Patient();
//        patient.setId(1L);
//        patient.setName("John Doe");
//        patient.setEgn("1234567890");
//
//        patientViewDTO = new PatientViewDTO(1L, "John Doe", "1234567890", LocalDate.now(), viewDTO);
//
//        visit = new Visit();
//        visit.setId(1L);
//        visit.setVisitDate(LocalDate.now());
//
//        visitViewDTO = new VisitViewDTO(1L, LocalDate.now(), LocalTime.now(), false, patientViewDTO, viewDTO, null, null, null);
//    }
//
//    // Happy Path
//    @Test
//    void Create_ValidDTO_ReturnsDoctorViewDTO() {
//        when(doctorRepository.findByUniqueIdNumber(createDTO.getUniqueIdNumber())).thenReturn(Optional.empty());
//        when(specialtyRepository.findAllById(specialtyIds)).thenReturn(List.of(specialty));
//        when(modelMapper.map(createDTO, Doctor.class)).thenReturn(doctor);
//        when(doctorRepository.save(doctor)).thenReturn(doctor);
//        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(viewDTO);
//
//        DoctorViewDTO result = doctorService.create(createDTO);
//
//        assertNotNull(result);
//        assertEquals(viewDTO, result);
//        verify(doctorRepository).save(doctor);
//        verify(modelMapper).map(createDTO, Doctor.class);
//        verify(modelMapper).map(doctor, DoctorViewDTO.class);
//    }
//
//    @Test
//    void Update_ValidDTO_ReturnsUpdatedDoctorViewDTO() {
//        when(doctorRepository.findById(updateDTO.getId())).thenReturn(Optional.of(doctor));
//        when(specialtyRepository.findAllById(specialtyIds)).thenReturn(List.of(specialty));
//        doNothing().when(modelMapper).map(updateDTO, doctor);
//        when(doctorRepository.save(doctor)).thenReturn(doctor);
//        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(viewDTO);
//
//        DoctorViewDTO result = doctorService.update(updateDTO);
//
//        assertNotNull(result);
//        assertEquals(viewDTO, result);
//        verify(doctorRepository).save(doctor);
//        verify(modelMapper).map(updateDTO, doctor);
//        verify(modelMapper).map(doctor, DoctorViewDTO.class);
//    }
//
//    @Test
//    void Delete_ValidId_DeletesDoctor() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(List.of());
//
//        doctorService.delete(1L);
//
//        verify(doctorRepository).delete(doctor);
//    }
//
//    @Test
//    void GetById_ValidId_ReturnsDoctorViewDTO() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(viewDTO);
//
//        DoctorViewDTO result = doctorService.getById(1L);
//
//        assertNotNull(result);
//        assertEquals(viewDTO, result);
//        verify(doctorRepository).findById(1L);
//        verify(modelMapper).map(doctor, DoctorViewDTO.class);
//    }
//
//    @Test
//    void GetByUniqueIdNumber_ValidUniqueId_ReturnsDoctorViewDTO() {
//        when(doctorRepository.findByUniqueIdNumber("DOC12345")).thenReturn(Optional.of(doctor));
//        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(viewDTO);
//
//        DoctorViewDTO result = doctorService.getByUniqueIdNumber("DOC12345");
//
//        assertNotNull(result);
//        assertEquals(viewDTO, result);
//        verify(doctorRepository).findByUniqueIdNumber("DOC12345");
//        verify(modelMapper).map(doctor, DoctorViewDTO.class);
//    }
//
//    @Test
//    void GetAll_ValidParameters_ReturnsPaginatedDoctors() {
//        Page<Doctor> page = new PageImpl<>(List.of(doctor));
//        when(doctorRepository.findAllActive(any(PageRequest.class))).thenReturn(page);
//        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(viewDTO);
//
//        Page<DoctorViewDTO> result = doctorService.getAll(0, 10, "name", true, null).join();
//
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals(viewDTO, result.getContent().getFirst());
//        assertEquals(1, result.getTotalElements());
//        verify(doctorRepository).findAllActive(any(PageRequest.class));
//        verify(modelMapper).map(doctor, DoctorViewDTO.class);
//    }
//
//    @Test
//    void GetAll_WithFilter_ReturnsFilteredDoctors() {
//        Page<Doctor> page = new PageImpl<>(List.of(doctor));
//        when(doctorRepository.findByNameOrUniqueIdNumberContaining(eq("%smith%"), any(PageRequest.class))).thenReturn(page);
//        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(viewDTO);
//
//        Page<DoctorViewDTO> result = doctorService.getAll(0, 10, "name", true, "smith").join();
//
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals(viewDTO, result.getContent().getFirst());
//        verify(doctorRepository).findByNameOrUniqueIdNumberContaining(eq("%smith%"), any(PageRequest.class));
//        verify(modelMapper).map(doctor, DoctorViewDTO.class);
//    }
//
//    @Test
//    void FindByCriteria_ValidConditions_ReturnsFilteredDoctors() {
//        Map<String, Object> conditions = Map.of("name", "Smith", "isGeneralPractitioner", "true", "specialtyId", "1");
//        Page<Doctor> page = new PageImpl<>(List.of(doctor));
//        when(doctorRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
//        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(viewDTO);
//
//        Page<DoctorViewDTO> result = doctorService.findByCriteria(conditions, 0, 10, "name", true);
//
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals(viewDTO, result.getContent().getFirst());
//        verify(doctorRepository).findAll(any(Specification.class), any(PageRequest.class));
//        verify(modelMapper).map(doctor, DoctorViewDTO.class);
//    }
//
//    @Test
//    void GetPatientsByGeneralPractitioner_ValidId_ReturnsPatients() {
//        Page<Patient> page = new PageImpl<>(List.of(patient));
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, Integer.MAX_VALUE))).thenReturn(page);
//        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(patientViewDTO);
//
//        Page<PatientViewDTO> result = doctorService.getPatientsByGeneralPractitioner(1L);
//
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals(patientViewDTO, result.getContent().getFirst());
//        verify(patientRepository).findByGeneralPractitioner(doctor, PageRequest.of(0, Integer.MAX_VALUE));
//        verify(modelMapper).map(patient, PatientViewDTO.class);
//    }
//
//    @Test
//    void GetPatientCountByGeneralPractitioner_ValidId_ReturnsCount() {
//        DoctorPatientCountDTO countDTO = DoctorPatientCountDTO.builder().doctor(doctor).patientCount(5L).build();
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(doctorRepository.findPatientCountByGeneralPractitioner()).thenReturn(List.of(countDTO));
//
//        DoctorPatientCountDTO result = doctorService.getPatientCountByGeneralPractitioner(1L);
//
//        assertNotNull(result);
//        assertEquals(5L, result.getPatientCount());
//        verify(doctorRepository).findPatientCountByGeneralPractitioner();
//    }
//
//    @Test
//    void GetVisitCount_ValidId_ReturnsCount() {
//        DoctorVisitCountDTO countDTO = DoctorVisitCountDTO.builder().doctor(doctor).visitCount(3L).build();
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(doctorRepository.findVisitCountByDoctor()).thenReturn(List.of(countDTO));
//
//        DoctorVisitCountDTO result = doctorService.getVisitCount(1L);
//
//        assertNotNull(result);
//        assertEquals(3L, result.getVisitCount());
//        verify(doctorRepository).findVisitCountByDoctor();
//    }
//
//    @Test
//    void GetVisitsByPeriod_ValidParameters_ReturnsVisits() {
//        Page<Visit> page = new PageImpl<>(List.of(visit));
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(visitRepository.findByDoctorAndDateRange(eq(doctor), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)))
//                .thenReturn(page);
//        when(modelMapper.map(visit, VisitViewDTO.class)).thenReturn(visitViewDTO);
//
//        Page<VisitViewDTO> result = doctorService.getVisitsByPeriod(1L, LocalDate.now(), LocalDate.now()).join();
//
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals(visitViewDTO, result.getContent().getFirst());
//        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class));
//        verify(modelMapper).map(visit, VisitViewDTO.class);
//    }
//
//    @Test
//    void GetDoctorsWithMostSickLeaves_ValidCall_ReturnsSickLeaveCounts() {
//        DoctorSickLeaveCountDTO countDTO = DoctorSickLeaveCountDTO.builder().doctor(doctor).sickLeaveCount(10L).build();
//        when(doctorRepository.findDoctorsWithMostSickLeaves()).thenReturn(List.of(countDTO));
//
//        List<DoctorSickLeaveCountDTO> result = doctorService.getDoctorsWithMostSickLeaves();
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals(10L, result.getFirst().getSickLeaveCount());
//        verify(doctorRepository).findDoctorsWithMostSickLeaves();
//    }
//
//    // Error Cases
//    @Test
//    void Create_NullDTO_ThrowsInvalidDTOException() {
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.create(null));
//        assertEquals(ExceptionMessages.formatInvalidDTONull("DoctorCreateDTO"), exception.getMessage());
//    }
//
//    @Test
//    void Create_DuplicateUniqueId_ThrowsInvalidDoctorException() {
//        when(doctorRepository.findByUniqueIdNumber(createDTO.getUniqueIdNumber())).thenReturn(Optional.of(doctor));
//
//        InvalidDoctorException exception = assertThrows(InvalidDoctorException.class, () -> doctorService.create(createDTO));
//        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_UNIQUE_ID_EXISTS, createDTO.getUniqueIdNumber()), exception.getMessage());
//    }
//
//    @Test
//    void Create_InvalidSpecialtyIds_ThrowsInvalidDoctorException() {
//        when(doctorRepository.findByUniqueIdNumber(createDTO.getUniqueIdNumber())).thenReturn(Optional.empty());
//        when(specialtyRepository.findAllById(specialtyIds)).thenReturn(List.of());
//
//        InvalidDoctorException exception = assertThrows(InvalidDoctorException.class, () -> doctorService.create(createDTO));
//        assertEquals("One or more specialty IDs are invalid", exception.getMessage());
//    }
//
//    @Test
//    void Update_NullDTO_ThrowsInvalidDTOException() {
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.update(null));
//        assertEquals(ExceptionMessages.formatInvalidDTONull("DoctorUpdateDTO"), exception.getMessage());
//    }
//
//    @Test
//    void Update_NullId_ThrowsInvalidDTOException() {
//        updateDTO.setId(null);
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.update(updateDTO));
//        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), exception.getMessage());
//    }
//
//    @Test
//    void Update_NonExistentDoctor_ThrowsEntityNotFoundException() {
//        when(doctorRepository.findById(updateDTO.getId())).thenReturn(Optional.empty());
//
//        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.update(updateDTO));
//        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, updateDTO.getId()), exception.getMessage());
//    }
//
//    @Test
//    void Delete_NullId_ThrowsInvalidDTOException() {
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.delete(null));
//        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), exception.getMessage());
//    }
//
//    @Test
//    void Delete_NonExistentDoctor_ThrowsEntityNotFoundException() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
//
//        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.delete(1L));
//        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 1L), exception.getMessage());
//    }
//
//    @Test
//    void Delete_GpWithPatients_ThrowsInvalidDoctorException() {
//        DoctorPatientCountDTO countDTO = DoctorPatientCountDTO.builder().doctor(doctor).patientCount(1L).build();
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(List.of(countDTO));
//
//        InvalidDoctorException exception = assertThrows(InvalidDoctorException.class, () -> doctorService.delete(1L));
//        assertEquals(ExceptionMessages.DOCTOR_HAS_ACTIVE_PATIENTS, exception.getMessage());
//    }
//
//    @Test
//    void GetById_NullId_ThrowsInvalidDTOException() {
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.getById(null));
//        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), exception.getMessage());
//    }
//
//    @Test
//    void GetById_NonExistentId_ThrowsEntityNotFoundException() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
//
//        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getById(1L));
//        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 1L), exception.getMessage());
//        verify(doctorRepository).findById(1L);
//        verify(modelMapper, never()).map(any(), eq(DoctorViewDTO.class));
//    }
//
//    @Test
//    void GetByUniqueIdNumber_NullUniqueId_ThrowsInvalidDTOException() {
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.getByUniqueIdNumber(null));
//        assertEquals(ExceptionMessages.formatInvalidFieldEmpty("uniqueIdNumber"), exception.getMessage());
//    }
//
//    @Test
//    void GetByUniqueIdNumber_NonExistentUniqueId_ThrowsEntityNotFoundException() {
//        when(doctorRepository.findByUniqueIdNumber("DOC12345")).thenReturn(Optional.empty());
//
//        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getByUniqueIdNumber("DOC12345"));
//        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_UNIQUE_ID, "DOC12345"), exception.getMessage());
//        verify(doctorRepository).findByUniqueIdNumber("DOC12345");
//        verify(modelMapper, never()).map(any(), eq(DoctorViewDTO.class));
//    }
//
//    @Test
//    void GetAll_NegativePage_ThrowsInvalidInputException() {
//        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> doctorService.getAll(-1, 10, "name", true, null).join());
//        assertEquals("Page number must not be negative", exception.getMessage());
//    }
//
//    @Test
//    void GetAll_InvalidPageSize_ThrowsInvalidInputException() {
//        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> doctorService.getAll(0, 101, "name", true, null).join());
//        assertEquals("Page size must be between 1 and 100", exception.getMessage());
//    }
//
//    @Test
//    void FindByCriteria_NegativePage_ThrowsInvalidInputException() {
//        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> doctorService.findByCriteria(Map.of(), -1, 10, "name", true));
//        assertEquals("Page number must not be negative", exception.getMessage());
//    }
//
//    @Test
//    void FindByCriteria_InvalidPageSize_ThrowsInvalidInputException() {
//        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> doctorService.findByCriteria(Map.of(), 0, 101, "name", true));
//        assertEquals("Page size must be between 1 and 100", exception.getMessage());
//    }
//
//    @Test
//    void GetPatientsByGeneralPractitioner_NullId_ThrowsInvalidDTOException() {
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.getPatientsByGeneralPractitioner(null));
//        assertEquals(ExceptionMessages.formatInvalidFieldNull("doctorId"), exception.getMessage());
//    }
//
//    @Test
//    void GetPatientsByGeneralPractitioner_NonExistentDoctor_ThrowsEntityNotFoundException() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
//
//        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getPatientsByGeneralPractitioner(1L));
//        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 1L), exception.getMessage());
//    }
//
//    @Test
//    void GetPatientCountByGeneralPractitioner_NullId_ThrowsInvalidDTOException() {
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.getPatientCountByGeneralPractitioner(null));
//        assertEquals(ExceptionMessages.formatInvalidFieldNull("doctorId"), exception.getMessage());
//    }
//
//    @Test
//    void GetPatientCountByGeneralPractitioner_NonExistentDoctor_ThrowsEntityNotFoundException() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
//
//        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getPatientCountByGeneralPractitioner(1L));
//        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 1L), exception.getMessage());
//    }
//
//    @Test
//    void GetVisitCount_NullId_ThrowsInvalidDTOException() {
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.getVisitCount(null));
//        assertEquals(ExceptionMessages.formatInvalidFieldNull("doctorId"), exception.getMessage());
//    }
//
//    @Test
//    void GetVisitCount_NonExistentDoctor_ThrowsEntityNotFoundException() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
//
//        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getVisitCount(1L));
//        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 1L), exception.getMessage());
//    }
//
//    @Test
//    void GetVisitsByPeriod_NullInputs_ThrowsInvalidDTOException() {
//        InvalidDTOException idException = assertThrows(InvalidDTOException.class, () -> doctorService.getVisitsByPeriod(null, LocalDate.now(), LocalDate.now()).join());
//        assertEquals(ExceptionMessages.formatInvalidFieldNull("doctorId"), idException.getMessage());
//
//        InvalidDTOException startDateException = assertThrows(InvalidDTOException.class, () -> doctorService.getVisitsByPeriod(1L, null, LocalDate.now()).join());
//        assertEquals(ExceptionMessages.formatInvalidFieldNull("startDate"), startDateException.getMessage());
//
//        InvalidDTOException endDateException = assertThrows(InvalidDTOException.class, () -> doctorService.getVisitsByPeriod(1L, LocalDate.now(), null).join());
//        assertEquals(ExceptionMessages.formatInvalidFieldNull("endDate"), endDateException.getMessage());
//    }
//
//    @Test
//    void GetVisitsByPeriod_NonExistentDoctor_ThrowsEntityNotFoundException() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
//
//        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> doctorService.getVisitsByPeriod(1L, LocalDate.now(), LocalDate.now()).join());
//        assertEquals(MessageFormat.format(ExceptionMessages.DOCTOR_NOT_FOUND_BY_ID, 1L), exception.getMessage());
//    }
//
//    @Test
//    void GetVisitsByPeriod_InvalidDateRange_ThrowsInvalidDTOException() {
//        LocalDate startDate = LocalDate.now();
//        LocalDate endDate = startDate.minusDays(1);
//
//        InvalidDTOException exception = assertThrows(InvalidDTOException.class, () -> doctorService.getVisitsByPeriod(1L, startDate, endDate).join());
//        assertEquals(MessageFormat.format(ExceptionMessages.INVALID_DATE_RANGE, startDate, endDate), exception.getMessage());
//        verify(doctorRepository, never()).findById(any());
//        verify(visitRepository, never()).findByDoctorAndDateRange(any(), any(), any(), any());
//    }
//
//    // Edge Cases
//    @Test
//    void Create_EmptySpecialtyIds_ReturnsDoctorViewDTO() {
//        createDTO.setSpecialtyIds(Set.of());
//        when(doctorRepository.findByUniqueIdNumber(createDTO.getUniqueIdNumber())).thenReturn(Optional.empty());
//        when(modelMapper.map(createDTO, Doctor.class)).thenReturn(doctor);
//        when(doctorRepository.save(doctor)).thenReturn(doctor);
//        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(viewDTO);
//
//        DoctorViewDTO result = doctorService.create(createDTO);
//
//        assertNotNull(result);
//        assertEquals(viewDTO, result);
//        verify(doctorRepository).save(doctor);
//        verify(specialtyRepository, never()).findAllById(any());
//    }
//
//    @Test
//    void GetAll_EmptyResults_ReturnsEmptyPage() {
//        Page<Doctor> emptyPage = new PageImpl<>(List.of());
//        when(doctorRepository.findAllActive(any(PageRequest.class))).thenReturn(emptyPage);
//
//        Page<DoctorViewDTO> result = doctorService.getAll(0, 10, "name", true, null).join();
//
//        assertNotNull(result);
//        assertTrue(result.getContent().isEmpty());
//        assertEquals(0, result.getTotalElements());
//        verify(doctorRepository).findAllActive(any(PageRequest.class));
//        verify(modelMapper, never()).map(any(), eq(DoctorViewDTO.class));
//    }
//
//    @Test
//    void FindByCriteria_EmptyConditions_ReturnsAllDoctors() {
//        Page<Doctor> page = new PageImpl<>(List.of(doctor));
//        when(doctorRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
//        when(modelMapper.map(doctor, DoctorViewDTO.class)).thenReturn(viewDTO);
//
//        Page<DoctorViewDTO> result = doctorService.findByCriteria(Map.of(), 0, 10, "name", true);
//
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals(viewDTO, result.getContent().getFirst());
//        verify(doctorRepository).findAll(any(Specification.class), any(PageRequest.class));
//        verify(modelMapper).map(doctor, DoctorViewDTO.class);
//    }
//
//    @Test
//    void GetPatientsByGeneralPractitioner_NoPatients_ReturnsEmptyPage() {
//        Page<Patient> emptyPage = new PageImpl<>(List.of());
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, Integer.MAX_VALUE))).thenReturn(emptyPage);
//
//        Page<PatientViewDTO> result = doctorService.getPatientsByGeneralPractitioner(1L);
//
//        assertNotNull(result);
//        assertTrue(result.getContent().isEmpty());
//        assertEquals(0, result.getTotalElements());
//        verify(patientRepository).findByGeneralPractitioner(doctor, PageRequest.of(0, Integer.MAX_VALUE));
//        verify(modelMapper, never()).map(any(), eq(PatientViewDTO.class));
//    }
//
//    @Test
//    void GetPatientCountByGeneralPractitioner_ZeroPatients_ReturnsZeroCount() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(doctorRepository.findPatientCountByGeneralPractitioner()).thenReturn(List.of());
//
//        DoctorPatientCountDTO result = doctorService.getPatientCountByGeneralPractitioner(1L);
//
//        assertNotNull(result);
//        assertEquals(0L, result.getPatientCount());
//        verify(doctorRepository).findPatientCountByGeneralPractitioner();
//    }
//
//    @Test
//    void GetVisitCount_ZeroVisits_ReturnsZeroCount() {
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(doctorRepository.findVisitCountByDoctor()).thenReturn(List.of());
//
//        DoctorVisitCountDTO result = doctorService.getVisitCount(1L);
//
//        assertNotNull(result);
//        assertEquals(0L, result.getVisitCount());
//        verify(doctorRepository).findVisitCountByDoctor();
//    }
//
//    @Test
//    void GetVisitsByPeriod_NoVisits_ReturnsEmptyPage() {
//        Page<Visit> emptyPage = new PageImpl<>(List.of());
//        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
//        when(visitRepository.findByDoctorAndDateRange(eq(doctor), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class)))
//                .thenReturn(emptyPage);
//
//        Page<VisitViewDTO> result = doctorService.getVisitsByPeriod(1L, LocalDate.now(), LocalDate.now()).join();
//
//        assertNotNull(result);
//        assertTrue(result.getContent().isEmpty());
//        assertEquals(0, result.getTotalElements());
//        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), any(LocalDate.class), any(LocalDate.class), any(PageRequest.class));
//        verify(modelMapper, never()).map(any(), eq(VisitViewDTO.class));
//    }
//
//    @Test
//    void GetDoctorsWithMostSickLeaves_EmptyResults_ReturnsEmptyList() {
//        when(doctorRepository.findDoctorsWithMostSickLeaves()).thenReturn(List.of());
//
//        List<DoctorSickLeaveCountDTO> result = doctorService.getDoctorsWithMostSickLeaves();
//
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//        verify(doctorRepository).findDoctorsWithMostSickLeaves();
//    }
//}