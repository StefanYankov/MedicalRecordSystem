package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.services.common.exceptions.InvalidPatientException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class PatientServiceUnitTests {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Doctor gp;
    private Patient patient;
    private PatientCreateDTO createDTO;
    private PatientUpdateDTO updateDTO;
    private PatientViewDTO viewDTO;
    private DoctorPatientCountDTO countDTO;
    private String validEgn;
    private String validUniqueId;

    @BeforeEach
    void setUp() {
        validEgn = TestDataUtils.generateValidEgn();
        validUniqueId = TestDataUtils.generateUniqueIdNumber();

        gp = new Doctor();
        gp.setId(1L);
        gp.setName("Dr. Smith");
        gp.setUniqueIdNumber(validUniqueId);
        gp.setGeneralPractitioner(true);

        patient = new Patient();
        patient.setId(1L);
        patient.setName("John Doe");
        patient.setEgn(validEgn);
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(gp);
        patient.setIsDeleted(false);

        createDTO = PatientCreateDTO.builder()
                .name("John Doe")
                .egn(validEgn)
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(1L)
                .build();

        updateDTO = PatientUpdateDTO.builder()
                .id(1L)
                .name("Jane Doe")
                .lastInsurancePaymentDate(LocalDate.now().minusDays(1))
                .generalPractitionerId(1L)
                .build();

        viewDTO = PatientViewDTO.builder()
                .id(1L)
                .name("John Doe")
                .egn(validEgn)
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitioner(new DoctorViewDTO(1L, "Dr. Smith", validUniqueId, true, null, null))
                .build();

        countDTO = DoctorPatientCountDTO.builder()
                .doctor(gp)
                .patientCount(5L)
                .build();
    }

    // Happy Path
    @Test
    void create_ValidDTO_ReturnsPatientViewDTO() {
        when(patientRepository.findByEgn(createDTO.getEgn())).thenReturn(Optional.empty());
        when(doctorRepository.findById(createDTO.getGeneralPractitionerId())).thenReturn(Optional.of(gp));
        when(modelMapper.map(createDTO, Patient.class)).thenReturn(patient);
        when(patientRepository.save(patient)).thenReturn(patient);
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        PatientViewDTO result = patientService.create(createDTO);

        assertNotNull(result);
        assertEquals(viewDTO, result);
        verify(patientRepository).save(patient);
        verify(modelMapper).map(createDTO, Patient.class);
        verify(modelMapper).map(patient, PatientViewDTO.class);
    }

    @Test
    void update_ValidDTO_ReturnsUpdatedPatientViewDTO() {
        when(patientRepository.findById(updateDTO.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(updateDTO.getGeneralPractitionerId())).thenReturn(Optional.of(gp));
        doNothing().when(modelMapper).map(updateDTO, patient);
        when(patientRepository.save(patient)).thenReturn(patient);
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        PatientViewDTO result = patientService.update(updateDTO);

        assertNotNull(result);
        assertEquals(viewDTO, result);
        verify(patientRepository).save(patient);
        verify(modelMapper).map(updateDTO, patient);
        verify(modelMapper).map(patient, PatientViewDTO.class);
    }

    @Test
    void delete_ValidId_DeletesPatient() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        patientService.delete(1L);

        verify(patientRepository).delete(patient);
    }

    @Test
    void getById_ValidId_ReturnsPatientViewDTO() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        PatientViewDTO result = patientService.getById(1L);

        assertNotNull(result);
        assertEquals(viewDTO, result);
        verify(patientRepository).findById(1L);
        verify(modelMapper).map(patient, PatientViewDTO.class);
    }

    @Test
    void getByEgn_ValidEgn_ReturnsPatientViewDTO() {
        when(patientRepository.findByEgn(validEgn)).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        PatientViewDTO result = patientService.getByEgn(validEgn);

        assertNotNull(result);
        assertEquals(viewDTO, result);
        verify(patientRepository).findByEgn(validEgn);
        verify(modelMapper).map(patient, PatientViewDTO.class);
    }

    @Test
    void getAll_ValidParameters_ReturnsPaginatedPatients() {
        Page<Patient> page = new PageImpl<>(List.of(patient));
        when(patientRepository.findAllActive(any(PageRequest.class))).thenReturn(page);
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        Page<PatientViewDTO> result = patientService.getAll(0, 10, "name", true, null).join();

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(viewDTO, result.getContent().get(0));
        assertEquals(1, result.getTotalElements());
        verify(patientRepository).findAllActive(any(PageRequest.class));
        verify(modelMapper).map(patient, PatientViewDTO.class);
    }

    @Test
    void getAll_WithFilter_ReturnsFilteredPatients() {
        Page<Patient> page = new PageImpl<>(List.of(patient));
        when(patientRepository.findByNameOrEgnContaining(eq("%doe%"), any(PageRequest.class))).thenReturn(page);
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        Page<PatientViewDTO> result = patientService.getAll(0, 10, "name", true, "doe").join();

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(viewDTO, result.getContent().get(0));
        verify(patientRepository).findByNameOrEgnContaining(eq("%doe%"), any(PageRequest.class));
        verify(modelMapper).map(patient, PatientViewDTO.class);
    }

    @Test
    void getByGeneralPractitioner_ValidId_ReturnsPatients() {
        Page<Patient> page = new PageImpl<>(List.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(gp));
        when(patientRepository.findByGeneralPractitioner(eq(gp), any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        Page<PatientViewDTO> result = patientService.getByGeneralPractitioner(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(viewDTO, result.getContent().get(0));
        verify(patientRepository).findByGeneralPractitioner(eq(gp), any(Pageable.class));
        verify(modelMapper).map(patient, PatientViewDTO.class);
    }

    @Test
    void getPatientCountByGeneralPractitioner_ValidCall_ReturnsCounts() {
        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(List.of(countDTO));

        List<DoctorPatientCountDTO> result = patientService.getPatientCountByGeneralPractitioner();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getPatientCount());
        verify(patientRepository).countPatientsByGeneralPractitioner();
    }

    // Error Cases
    @Test
    void create_NullDTO_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.create(null));
        assertEquals(ExceptionMessages.formatInvalidDTONull("PatientCreateDTO"), ex.getMessage());
    }

    @Test
    void create_DuplicateEgn_ThrowsInvalidPatientException() {
        when(patientRepository.findByEgn(validEgn)).thenReturn(Optional.of(patient));

        InvalidPatientException ex = assertThrows(InvalidPatientException.class, () -> patientService.create(createDTO));
        assertEquals(ExceptionMessages.formatPatientEgnExists(validEgn), ex.getMessage());
        verify(patientRepository).findByEgn(validEgn);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void create_NonExistentGeneralPractitioner_ThrowsEntityNotFoundException() {
        when(patientRepository.findByEgn(validEgn)).thenReturn(Optional.empty());
        when(doctorRepository.findById(createDTO.getGeneralPractitionerId())).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.create(createDTO));
        assertEquals(ExceptionMessages.formatDoctorNotFoundById(createDTO.getGeneralPractitionerId()), ex.getMessage());
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void create_NonGeneralPractitioner_ThrowsInvalidPatientException() {
        Doctor nonGp = new Doctor();
        nonGp.setId(2L);
        nonGp.setName("Dr. Jones");
        nonGp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        nonGp.setGeneralPractitioner(false);

        when(patientRepository.findByEgn(validEgn)).thenReturn(Optional.empty());
        when(doctorRepository.findById(createDTO.getGeneralPractitionerId())).thenReturn(Optional.of(nonGp));

        InvalidPatientException ex = assertThrows(InvalidPatientException.class, () -> patientService.create(createDTO));
        assertEquals(ExceptionMessages.formatInvalidGeneralPractitioner(createDTO.getGeneralPractitionerId()), ex.getMessage());
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void update_NullDTO_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.update(null));
        assertEquals(ExceptionMessages.formatInvalidDTONull("PatientUpdateDTO"), ex.getMessage());
    }

    @Test
    void update_NullId_ThrowsInvalidDTOException() {
        updateDTO.setId(null);
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.update(updateDTO));
        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), ex.getMessage());
    }

    @Test
    void update_NonExistentPatient_ThrowsEntityNotFoundException() {
        when(patientRepository.findById(updateDTO.getId())).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.update(updateDTO));
        assertEquals(ExceptionMessages.formatPatientNotFoundById(updateDTO.getId()), ex.getMessage());
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void update_NonExistentGeneralPractitioner_ThrowsEntityNotFoundException() {
        when(patientRepository.findById(updateDTO.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(updateDTO.getGeneralPractitionerId())).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.update(updateDTO));
        assertEquals(ExceptionMessages.formatDoctorNotFoundById(updateDTO.getGeneralPractitionerId()), ex.getMessage());
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void update_NonGeneralPractitioner_ThrowsInvalidPatientException() {
        Doctor nonGp = new Doctor();
        nonGp.setId(2L);
        nonGp.setName("Dr. Jones");
        nonGp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        nonGp.setGeneralPractitioner(false);

        when(patientRepository.findById(updateDTO.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(updateDTO.getGeneralPractitionerId())).thenReturn(Optional.of(nonGp));

        InvalidPatientException ex = assertThrows(InvalidPatientException.class, () -> patientService.update(updateDTO));
        assertEquals(ExceptionMessages.formatInvalidGeneralPractitioner(updateDTO.getGeneralPractitionerId()), ex.getMessage());
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_NullId_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.delete(null));
        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), ex.getMessage());
    }

    @Test
    void delete_NonExistentPatient_ThrowsEntityNotFoundException() {
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.delete(1L));
        assertEquals(ExceptionMessages.formatPatientNotFoundById(1L), ex.getMessage());
        verify(patientRepository, never()).delete(any());
    }

    @Test
    void getById_NullId_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.getById(null));
        assertEquals(ExceptionMessages.formatInvalidFieldNull("ID"), ex.getMessage());
    }

    @Test
    void getById_NonExistentId_ThrowsEntityNotFoundException() {
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.getById(1L));
        assertEquals(ExceptionMessages.formatPatientNotFoundById(1L), ex.getMessage());
        verify(modelMapper, never()).map(any(), eq(PatientViewDTO.class));
    }

    @Test
    void getByEgn_NullEgn_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.getByEgn(null));
        assertEquals(ExceptionMessages.formatInvalidFieldEmpty("EGN"), ex.getMessage());
    }

    @Test
    void getByEgn_EmptyEgn_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.getByEgn(""));
        assertEquals(ExceptionMessages.formatInvalidFieldEmpty("EGN"), ex.getMessage());
    }

    @Test
    void getByEgn_NonExistentEgn_ThrowsEntityNotFoundException() {
        when(patientRepository.findByEgn(validEgn)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.getByEgn(validEgn));
        assertEquals(ExceptionMessages.formatPatientNotFoundByEgn(validEgn), ex.getMessage());
        verify(modelMapper, never()).map(any(), eq(PatientViewDTO.class));
    }

    @Test
    void getAll_NegativePage_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.getAll(-1, 10, "name", true, null).join());
        assertEquals("Page number must not be negative", ex.getMessage());
    }

    @Test
    void getAll_InvalidPageSize_ThrowsInvalidDTOException() {
        InvalidDTOException ex = assertThrows(InvalidDTOException.class, () -> patientService.getAll(0, 101, "name", true, null).join());
        assertEquals("Page size must be between 1 and 100", ex.getMessage());
    }

    @Test
    void getByGeneralPractitioner_NonExistentDoctor_ThrowsEntityNotFoundException() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> patientService.getByGeneralPractitioner(1L, PageRequest.of(0, 10)));
        assertEquals(ExceptionMessages.formatDoctorNotFoundById(1L), ex.getMessage());
        verify(patientRepository, never()).findByGeneralPractitioner(any(), any());
    }

    @Test
    void getByGeneralPractitioner_NonGeneralPractitioner_ThrowsInvalidPatientException() {
        Doctor nonGp = new Doctor();
        nonGp.setId(2L);
        nonGp.setName("Dr. Jones");
        nonGp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        nonGp.setGeneralPractitioner(false);

        when(doctorRepository.findById(2L)).thenReturn(Optional.of(nonGp));

        InvalidPatientException ex = assertThrows(InvalidPatientException.class, () -> patientService.getByGeneralPractitioner(2L, PageRequest.of(0, 10)));
        assertEquals(ExceptionMessages.formatInvalidGeneralPractitioner(2L), ex.getMessage());
        verify(patientRepository, never()).findByGeneralPractitioner(any(), any());
    }

    // Edge Cases
    @Test
    void getAll_EmptyResults_ReturnsEmptyPage() {
        Page<Patient> emptyPage = new PageImpl<>(List.of());
        when(patientRepository.findAllActive(any(PageRequest.class))).thenReturn(emptyPage);

        Page<PatientViewDTO> result = patientService.getAll(0, 10, "name", true, null).join();

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(patientRepository).findAllActive(any(PageRequest.class));
        verify(modelMapper, never()).map(any(), eq(PatientViewDTO.class));
    }

    @Test
    void getAll_FilterNoMatches_ReturnsEmptyPage() {
        Page<Patient> emptyPage = new PageImpl<>(List.of());
        when(patientRepository.findByNameOrEgnContaining(eq("%nomatch%"), any(PageRequest.class))).thenReturn(emptyPage);

        Page<PatientViewDTO> result = patientService.getAll(0, 10, "name", true, "nomatch").join();

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(patientRepository).findByNameOrEgnContaining(eq("%nomatch%"), any(PageRequest.class));
        verify(modelMapper, never()).map(any(), eq(PatientViewDTO.class));
    }

    @Test
    void getByGeneralPractitioner_NoPatients_ReturnsEmptyPage() {
        Page<Patient> emptyPage = new PageImpl<>(List.of());
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(gp));
        when(patientRepository.findByGeneralPractitioner(eq(gp), any(Pageable.class))).thenReturn(emptyPage);

        Page<PatientViewDTO> result = patientService.getByGeneralPractitioner(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(patientRepository).findByGeneralPractitioner(eq(gp), any(Pageable.class));
        verify(modelMapper, never()).map(any(), eq(PatientViewDTO.class));
    }

    @Test
    void getPatientCountByGeneralPractitioner_EmptyResults_ReturnsEmptyList() {
        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(List.of());

        List<DoctorPatientCountDTO> result = patientService.getPatientCountByGeneralPractitioner();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(patientRepository).countPatientsByGeneralPractitioner();
    }
}