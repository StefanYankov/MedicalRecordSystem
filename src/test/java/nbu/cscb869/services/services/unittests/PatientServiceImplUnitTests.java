package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidPatientException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.PatientServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientServiceImplUnitTests {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    // --- Create Tests ---

    @Test
    void Create_ValidAdminDTO_ReturnsPatientViewDTO_HappyPath() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setName("Test Patient");
        dto.setEgn("1234567890");
        dto.setGeneralPractitionerId(1L);
        dto.setKeycloakId("new-patient-keycloak-id");

        Doctor gp = new Doctor();
        gp.setId(1L);
        gp.setGeneralPractitioner(true);

        Patient patient = new Patient();
        patient.setId(1L);

        PatientViewDTO viewDTO = new PatientViewDTO();

        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
        when(patientRepository.findByKeycloakId(dto.getKeycloakId())).thenReturn(Optional.empty());
        when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.of(gp));
        when(modelMapper.map(dto, Patient.class)).thenReturn(patient);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        // ACT
        PatientViewDTO result = patientService.create(dto);

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).save(patient);
    }

    @Test
    void Create_NullDTO_ThrowsInvalidDTOException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> patientService.create(null));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void Create_MissingKeycloakIdForAdmin_ThrowsInvalidDTOException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setKeycloakId(null);

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> patientService.create(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void Create_EgnAlreadyExists_ThrowsInvalidPatientException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setEgn("1234567890");
        dto.setKeycloakId("some-id");

        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.of(new Patient()));

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.create(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void Create_KeycloakIdAlreadyExists_ThrowsInvalidPatientException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setEgn("1234567890");
        dto.setKeycloakId("some-id");

        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
        when(patientRepository.findByKeycloakId(dto.getKeycloakId())).thenReturn(Optional.of(new Patient()));

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.create(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void Create_GeneralPractitionerNotFound_ThrowsEntityNotFoundException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setEgn("1234567890");
        dto.setGeneralPractitionerId(1L);
        dto.setKeycloakId("new-patient-keycloak-id");

        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
        when(patientRepository.findByKeycloakId(dto.getKeycloakId())).thenReturn(Optional.empty());
        when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> patientService.create(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void Create_DoctorIsNotGeneralPractitioner_ThrowsInvalidPatientException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setEgn("1234567890");
        dto.setGeneralPractitionerId(1L);
        dto.setKeycloakId("new-patient-keycloak-id");

        Doctor nonGp = new Doctor();
        nonGp.setId(1L);
        nonGp.setGeneralPractitioner(false);

        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
        when(patientRepository.findByKeycloakId(dto.getKeycloakId())).thenReturn(Optional.empty());
        when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.of(nonGp));

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.create(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    // --- Register Patient Tests ---

    @Test
    void RegisterPatient_ValidDTO_ReturnsPatientViewDTO_HappyPath() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setName("New Patient");
        dto.setEgn("0987654321");
        dto.setGeneralPractitionerId(2L);

        Doctor gp = new Doctor();
        gp.setId(2L);
        gp.setGeneralPractitioner(true);

        Patient patientFromMapper = new Patient();
        patientFromMapper.setId(2L);

        PatientViewDTO viewDTO = new PatientViewDTO();

        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
        when(patientRepository.findByKeycloakId(testKeycloakId)).thenReturn(Optional.empty());
        when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.of(gp));
        when(modelMapper.map(dto, Patient.class)).thenReturn(patientFromMapper);
        when(patientRepository.save(any(Patient.class))).thenReturn(patientFromMapper);
        when(modelMapper.map(patientFromMapper, PatientViewDTO.class)).thenReturn(viewDTO);

        // ACT
        PatientViewDTO result = patientService.registerPatient(dto);

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).save(patientFromMapper);
        assertEquals(testKeycloakId, patientFromMapper.getKeycloakId());
    }

    @Test
    void RegisterPatient_EgnAlreadyExists_ThrowsInvalidPatientException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setEgn("0987654321");

        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.of(new Patient()));

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.registerPatient(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void RegisterPatient_KeycloakIdAlreadyExists_ThrowsInvalidPatientException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setEgn("0987654321");

        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
        when(patientRepository.findByKeycloakId(testKeycloakId)).thenReturn(Optional.of(new Patient()));

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.registerPatient(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void RegisterPatient_DoctorIsNotGeneralPractitioner_ThrowsInvalidPatientException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        PatientCreateDTO dto = new PatientCreateDTO();
        dto.setEgn("0987654321");
        dto.setGeneralPractitionerId(2L);

        Doctor nonGp = new Doctor();
        nonGp.setId(2L);
        nonGp.setGeneralPractitioner(false);

        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
        when(patientRepository.findByKeycloakId(testKeycloakId)).thenReturn(Optional.empty());
        when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.of(nonGp));

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.registerPatient(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    // --- Update Tests ---

    @Test
    void Update_ValidPatientDTO_ReturnsPatientViewDTO_HappyPath() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        PatientUpdateDTO dto = new PatientUpdateDTO();
        dto.setId(1L);
        dto.setName("Updated Patient");
        dto.setEgn("1234567890");
        dto.setGeneralPractitionerId(1L);

        Doctor gp = new Doctor();
        gp.setId(1L);
        gp.setGeneralPractitioner(true);

        Patient existingPatient = new Patient();
        existingPatient.setId(1L);
        existingPatient.setKeycloakId(testKeycloakId);
        existingPatient.setEgn("1234567890");

        PatientViewDTO viewDTO = new PatientViewDTO();

        when(patientRepository.findById(dto.getId())).thenReturn(Optional.of(existingPatient));
        when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.of(gp));
        when(patientRepository.save(any(Patient.class))).thenReturn(existingPatient);
        when(modelMapper.map(existingPatient, PatientViewDTO.class)).thenReturn(viewDTO);

        // ACT
        PatientViewDTO result = patientService.update(dto);

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).save(existingPatient);
        verify(modelMapper).map(dto, existingPatient);
    }

    @Test
    void Update_ValidAdminDTO_ReturnsPatientViewDTO_HappyPath() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        PatientUpdateDTO dto = new PatientUpdateDTO();
        dto.setId(1L);
        dto.setName("Admin Updated Patient");
        dto.setEgn("1234567890");
        dto.setGeneralPractitionerId(1L);
        dto.setKeycloakId("new-keycloak-id");

        Doctor gp = new Doctor();
        gp.setId(1L);
        gp.setGeneralPractitioner(true);

        Patient existingPatient = new Patient();
        existingPatient.setId(1L);
        existingPatient.setKeycloakId("original-keycloak-id");
        existingPatient.setEgn("1234567890");

        PatientViewDTO viewDTO = new PatientViewDTO();

        when(patientRepository.findById(dto.getId())).thenReturn(Optional.of(existingPatient));
        when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.of(gp));
        when(patientRepository.findByKeycloakId("new-keycloak-id")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenReturn(existingPatient);
        when(modelMapper.map(existingPatient, PatientViewDTO.class)).thenReturn(viewDTO);

        // ACT
        PatientViewDTO result = patientService.update(dto);

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).save(existingPatient);
        verify(modelMapper).map(dto, existingPatient);
        assertEquals("new-keycloak-id", existingPatient.getKeycloakId());
    }

    @Test
    void Update_PatientUpdatesOtherPatient_ThrowsAccessDeniedException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        PatientUpdateDTO dto = new PatientUpdateDTO();
        dto.setId(1L);

        Patient otherPatient = new Patient();
        otherPatient.setId(1L);
        otherPatient.setKeycloakId("other-keycloak-id");

        when(patientRepository.findById(dto.getId())).thenReturn(Optional.of(otherPatient));

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> patientService.update(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void Update_EgnAlreadyExists_ThrowsInvalidPatientException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        PatientUpdateDTO dto = new PatientUpdateDTO();
        dto.setId(1L);
        dto.setEgn("existing-egn");

        Patient existingPatient = new Patient();
        existingPatient.setId(1L);
        existingPatient.setEgn("original-egn");

        Patient anotherPatientWithEgn = new Patient();
        anotherPatientWithEgn.setId(2L);
        anotherPatientWithEgn.setEgn("existing-egn");

        when(patientRepository.findById(dto.getId())).thenReturn(Optional.of(existingPatient));
        when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.of(anotherPatientWithEgn));

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.update(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void Update_KeycloakIdAlreadyExists_ThrowsInvalidPatientException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        PatientUpdateDTO dto = new PatientUpdateDTO();
        dto.setId(1L);
        dto.setEgn("1234567890");
        dto.setKeycloakId("existing-keycloak-id");

        Patient existingPatient = new Patient();
        existingPatient.setId(1L);
        existingPatient.setEgn("1234567890");
        existingPatient.setKeycloakId("original-keycloak-id");

        Patient anotherPatientWithKeycloakId = new Patient();
        anotherPatientWithKeycloakId.setId(2L);
        anotherPatientWithKeycloakId.setKeycloakId("existing-keycloak-id");

        when(patientRepository.findById(dto.getId())).thenReturn(Optional.of(existingPatient));
        when(patientRepository.findByKeycloakId(dto.getKeycloakId())).thenReturn(Optional.of(anotherPatientWithKeycloakId));

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.update(dto));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    // --- Delete Tests ---

    @Test
    void Delete_ValidId_DeletesPatient_HappyPath() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        Long id = 1L;
        Patient patient = new Patient();
        patient.setId(id);

        when(patientRepository.findById(id)).thenReturn(Optional.of(patient));

        // ACT
        patientService.delete(id);

        // ASSERT
        verify(patientRepository).delete(patient);
    }

    @Test
    void Delete_NonExistingId_ThrowsEntityNotFoundException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        Long id = 1L;
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> patientService.delete(id));
        verify(patientRepository, never()).delete(any(Patient.class));
    }

    // --- GetById Tests ---

    @Test
    void GetById_ExistingIdAsPatient_ReturnsPatientViewDTO_HappyPath() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        long patientId = 1L;
        Patient patient = new Patient();
        patient.setId(patientId);
        patient.setKeycloakId(testKeycloakId);

        PatientViewDTO viewDTO = new PatientViewDTO();
        viewDTO.setId(patientId);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        // ACT
        PatientViewDTO result = patientService.getById(patientId);

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).findById(patientId);
        verify(modelMapper).map(patient, PatientViewDTO.class);
    }

    @Test
    void GetById_ExistingIdAsDoctor_ReturnsPatientViewDTO_HappyPath() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))).when(authentication).getAuthorities();

        Long id = 1L;
        Patient patient = new Patient();
        patient.setId(id);
        patient.setKeycloakId("some-patient-id");

        PatientViewDTO viewDTO = new PatientViewDTO();

        when(patientRepository.findById(id)).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        // ACT
        PatientViewDTO result = patientService.getById(id);

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).findById(id);
    }

    @Test
    void GetById_PatientAccessesOtherPatient_ThrowsAccessDeniedException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        Long id = 1L;
        Patient otherPatient = new Patient();
        otherPatient.setId(id);
        otherPatient.setKeycloakId("other-keycloak-id");

        when(patientRepository.findById(id)).thenReturn(Optional.of(otherPatient));

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> patientService.getById(id));
    }

    @Test
    void GetById_NonExistingId_ThrowsEntityNotFoundException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        Long id = 1L;
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> patientService.getById(id));
    }

    // --- GetByEgn Tests ---

    @Test
    void GetByEgn_ExistingEgnAsPatient_ReturnsPatientViewDTO_HappyPath() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        String egn = "1234567890";
        Patient patient = new Patient();
        patient.setEgn(egn);
        patient.setKeycloakId(testKeycloakId);

        PatientViewDTO viewDTO = new PatientViewDTO();

        when(patientRepository.findByEgn(egn)).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        // ACT
        PatientViewDTO result = patientService.getByEgn(egn);

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).findByEgn(egn);
    }

    @Test
    void GetByEgn_PatientAccessesOtherPatient_ThrowsAccessDeniedException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        String egn = "1234567890";
        Patient otherPatient = new Patient();
        otherPatient.setEgn(egn);
        otherPatient.setKeycloakId("other-keycloak-id");

        when(patientRepository.findByEgn(egn)).thenReturn(Optional.of(otherPatient));

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> patientService.getByEgn(egn));
    }

    @Test
    void GetByEgn_NonExistingEgn_ThrowsEntityNotFoundException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        String egn = "1234567890";
        when(patientRepository.findByEgn(egn)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> patientService.getByEgn(egn));
    }

    // --- GetByKeycloakId Tests ---

    @Test
    void GetByKeycloakId_ExistingIdAsPatient_ReturnsPatientViewDTO_HappyPath() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        Patient patient = new Patient();
        patient.setKeycloakId(testKeycloakId);

        PatientViewDTO viewDTO = new PatientViewDTO();

        when(patientRepository.findByKeycloakId(testKeycloakId)).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

        // ACT
        PatientViewDTO result = patientService.getByKeycloakId(testKeycloakId);

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).findByKeycloakId(testKeycloakId);
    }

    @Test
    void GetByKeycloakId_PatientAccessesOtherPatient_ThrowsAccessDeniedException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        Patient otherPatient = new Patient();
        otherPatient.setKeycloakId("other-keycloak-id");

        when(patientRepository.findByKeycloakId("other-keycloak-id")).thenReturn(Optional.of(otherPatient));

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> patientService.getByKeycloakId("other-keycloak-id"));
    }

    @Test
    void GetByKeycloakId_NonExistingId_ThrowsEntityNotFoundException_ErrorCase() {
        // ARRANGE
        String testKeycloakId = "test-keycloak-id";
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testKeycloakId);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        when(patientRepository.findByKeycloakId(testKeycloakId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> patientService.getByKeycloakId(testKeycloakId));
    }

    // --- GetAll Tests ---

    @Test
    void GetAll_ValidPaginationAsAdmin_ReturnsPage_HappyPath() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        int page = 0;
        int size = 10;
        Page<Patient> patientPage = new PageImpl<>(List.of(new Patient()));
        when(patientRepository.findAll(any(Pageable.class))).thenReturn(patientPage);
        when(modelMapper.map(any(Patient.class), eq(PatientViewDTO.class))).thenReturn(new PatientViewDTO());

        // ACT
        CompletableFuture<Page<PatientViewDTO>> resultFuture = patientService.getAll(page, size, "name", true, null);
        Page<PatientViewDTO> result = resultFuture.join();

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).findAll(any(Pageable.class));
    }

    @Test
    void GetAll_ValidPaginationAsDoctor_ReturnsPage_HappyPath() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))).when(authentication).getAuthorities();

        int page = 0;
        int size = 10;
        Page<Patient> patientPage = new PageImpl<>(List.of(new Patient()));
        when(patientRepository.findAll(any(Pageable.class))).thenReturn(patientPage);
        when(modelMapper.map(any(Patient.class), eq(PatientViewDTO.class))).thenReturn(new PatientViewDTO());

        // ACT
        CompletableFuture<Page<PatientViewDTO>> resultFuture = patientService.getAll(page, size, "name", true, null);
        Page<PatientViewDTO> result = resultFuture.join();

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).findAll(any(Pageable.class));
    }

    @Test
    void GetAll_InvalidPagination_ThrowsInvalidDTOException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        // ACT & ASSERT
        assertThrows(InvalidDTOException.class, () -> patientService.getAll(-1, 0, "name", true, null));
        verify(patientRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void GetAll_WithFilter_ReturnsFilteredPage_HappyPath() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        int page = 0;
        int size = 10;
        String filter = "123";
        Page<Patient> patientPage = new PageImpl<>(List.of(new Patient()));
        when(patientRepository.findByEgnContaining(anyString(), any(Pageable.class))).thenReturn(patientPage);
        when(modelMapper.map(any(Patient.class), eq(PatientViewDTO.class))).thenReturn(new PatientViewDTO());

        // ACT
        CompletableFuture<Page<PatientViewDTO>> resultFuture = patientService.getAll(page, size, "name", true, filter);
        Page<PatientViewDTO> result = resultFuture.join();

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).findByEgnContaining(eq("%123%"), any(Pageable.class));
    }

    // --- GetByGeneralPractitioner Tests ---

    @Test
    void GetByGeneralPractitioner_ValidId_ReturnsPage_HappyPath() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        Long gpId = 1L;
        Doctor gp = new Doctor();
        gp.setId(gpId);
        gp.setGeneralPractitioner(true);

        Page<Patient> patientPage = new PageImpl<>(List.of(new Patient()));
        PatientViewDTO viewDTO = new PatientViewDTO();

        when(doctorRepository.findById(gpId)).thenReturn(Optional.of(gp));
        when(patientRepository.findByGeneralPractitioner(eq(gp), any(Pageable.class))).thenReturn(patientPage);
        when(modelMapper.map(any(Patient.class), eq(PatientViewDTO.class))).thenReturn(viewDTO);

        // ACT
        Page<PatientViewDTO> result = patientService.getByGeneralPractitioner(gpId, 0, 10);

        // ASSERT
        assertNotNull(result);
        verify(patientRepository).findByGeneralPractitioner(eq(gp), any(Pageable.class));
    }

    @Test
    void GetByGeneralPractitioner_NonExistingGP_ThrowsEntityNotFoundException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        Long gpId = 1L;
        when(doctorRepository.findById(gpId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> patientService.getByGeneralPractitioner(gpId, 0, 10));
        verify(patientRepository, never()).findByGeneralPractitioner(any(), any(Pageable.class));
    }

    @Test
    void GetByGeneralPractitioner_DoctorIsNotGP_ThrowsInvalidPatientException_ErrorCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        Long gpId = 1L;
        Doctor nonGp = new Doctor();
        nonGp.setId(gpId);
        nonGp.setGeneralPractitioner(false);

        when(doctorRepository.findById(gpId)).thenReturn(Optional.of(nonGp));

        // ACT & ASSERT
        assertThrows(InvalidPatientException.class, () -> patientService.getByGeneralPractitioner(gpId, 0, 10));
        verify(patientRepository, never()).findByGeneralPractitioner(any(), any(Pageable.class));
    }

    // --- GetPatientCountByGeneralPractitioner Tests ---

    @Test
    void GetPatientCountByGeneralPractitioner_ReturnsList_HappyPath() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        List<DoctorPatientCountDTO> countList = List.of(DoctorPatientCountDTO.builder().doctor(new Doctor()).patientCount(5L).build());
        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(countList);

        // ACT
        List<DoctorPatientCountDTO> result = patientService.getPatientCountByGeneralPractitioner();

        // ASSERT
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(patientRepository).countPatientsByGeneralPractitioner();
    }

    @Test
    void GetPatientCountByGeneralPractitioner_NoData_ReturnsEmptyList_EdgeCase() {
        // ARRANGE
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(Collections.emptyList());

        // ACT
        List<DoctorPatientCountDTO> result = patientService.getPatientCountByGeneralPractitioner();

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(patientRepository).countPatientsByGeneralPractitioner();
    }
}
