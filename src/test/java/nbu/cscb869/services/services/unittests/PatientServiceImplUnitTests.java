package nbu.cscb869.services.services.unittests;

import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidPatientException;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.PatientServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.LocalDate;
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
@DisplayName("PatientServiceImpl Unit Tests")
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
    private OidcUser oidcUser;

    @BeforeEach
    void setup() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Patient setupPatient(Long id, String keycloakId, String egn, String name, Long gpId, boolean isGp, LocalDate lastInsurancePaymentDate) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setKeycloakId(keycloakId);
        patient.setEgn(egn);
        patient.setName(name);
        Doctor gp = new Doctor();
        gp.setId(gpId);
        gp.setGeneralPractitioner(isGp);
        patient.setGeneralPractitioner(gp);
        patient.setLastInsurancePaymentDate(lastInsurancePaymentDate);
        return patient;
    }

    private Doctor setupDoctor(Long id, boolean isGp) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setGeneralPractitioner(isGp);
        return doctor;
    }

    @Nested
    @DisplayName("Create Patient Functionality")
    class CreateTests {
        @Test
        @DisplayName("create_AsAdmin_WithValidDTO_ShouldReturnPatientViewDTO_HappyPath")
        void create_AsAdmin_WithValidDTO_ShouldReturnPatientViewDTO_HappyPath() {
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setName("Test Patient");
            dto.setEgn(TestDataUtils.generateValidEgn());
            dto.setGeneralPractitionerId(1L);
            dto.setKeycloakId("new-patient-keycloak-id");

            Doctor gp = setupDoctor(1L, true);
            Patient patient = setupPatient(1L, dto.getKeycloakId(), dto.getEgn(), dto.getName(), gp.getId(), gp.isGeneralPractitioner(), dto.getLastInsurancePaymentDate());
            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setId(patient.getId());

            when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
            when(patientRepository.findByKeycloakId(dto.getKeycloakId())).thenReturn(Optional.empty());
            when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.of(gp));
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);
            when(modelMapper.map(any(Patient.class), eq(PatientViewDTO.class))).thenReturn(viewDTO);

            PatientViewDTO result = patientService.create(dto);

            assertNotNull(result);
            assertEquals(patient.getId(), result.getId());
            verify(patientRepository).save(any(Patient.class));
        }

        @Test
        @DisplayName("create_WithNullDTO_ShouldThrowInvalidDTOException_ErrorCase")
        void create_WithNullDTO_ShouldThrowInvalidDTOException_ErrorCase() {
            assertThrows(InvalidDTOException.class, () -> patientService.create(null));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("create_WithMissingKeycloakId_ShouldThrowInvalidDTOException_ErrorCase")
        void create_WithMissingKeycloakId_ShouldThrowInvalidDTOException_ErrorCase() {
            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setKeycloakId(null);

            assertThrows(InvalidDTOException.class, () -> patientService.create(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("create_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase")
        void create_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase() {
            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setEgn(TestDataUtils.generateValidEgn());
            dto.setKeycloakId("some-id");

            when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.of(new Patient()));

            assertThrows(InvalidDTOException.class, () -> patientService.create(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("create_WithExistingKeycloakId_ShouldThrowInvalidDTOException_ErrorCase")
        void create_WithExistingKeycloakId_ShouldThrowInvalidDTOException_ErrorCase() {
            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setEgn(TestDataUtils.generateValidEgn());
            dto.setKeycloakId("some-id");

            when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
            when(patientRepository.findByKeycloakId(dto.getKeycloakId())).thenReturn(Optional.of(new Patient()));

            assertThrows(InvalidDTOException.class, () -> patientService.create(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("create_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase")
        void create_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase() {
            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setEgn(TestDataUtils.generateValidEgn());
            dto.setGeneralPractitionerId(1L);
            dto.setKeycloakId("new-patient-keycloak-id");

            when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
            when(patientRepository.findByKeycloakId(dto.getKeycloakId())).thenReturn(Optional.empty());
            when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> patientService.create(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("create_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase")
        void create_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase() {
            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setEgn(TestDataUtils.generateValidEgn());
            dto.setGeneralPractitionerId(1L);
            dto.setKeycloakId("new-patient-keycloak-id");

            Doctor nonGp = setupDoctor(1L, false);

            when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty());
            when(patientRepository.findByKeycloakId(dto.getKeycloakId())).thenReturn(Optional.empty());
            when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.of(nonGp));

            assertThrows(InvalidDTOException.class, () -> patientService.create(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }
    }

    @Nested
    @DisplayName("Register Patient Functionality")
    class RegisterPatientTests {
        @BeforeEach
        void setupSecurityContext() {
            when(authentication.getPrincipal()).thenReturn(oidcUser);
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();
        }

        @Test
        @DisplayName("registerPatient_WithValidDTO_ShouldReturnPatientViewDTO_HappyPath")
        void registerPatient_WithValidDTO_ShouldReturnPatientViewDTO_HappyPath() {
            String testKeycloakId = "test-keycloak-id";
            String testName = "New Patient";
            String testEgn = TestDataUtils.generateValidEgn();
            Long testGpId = 2L;

            when(oidcUser.getSubject()).thenReturn(testKeycloakId);
            when(oidcUser.getFullName()).thenReturn(testName);

            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setEgn(testEgn);
            dto.setGeneralPractitionerId(testGpId);

            Doctor gp = setupDoctor(testGpId, true);
            Patient patient = setupPatient(2L, testKeycloakId, testEgn, testName, testGpId, true, null);
            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setId(patient.getId());
            viewDTO.setKeycloakId(patient.getKeycloakId());
            viewDTO.setName(patient.getName());
            viewDTO.setEgn(patient.getEgn());

            when(patientRepository.findByEgn(testEgn)).thenReturn(Optional.empty());
            when(patientRepository.findByKeycloakId(testKeycloakId)).thenReturn(Optional.empty());
            when(doctorRepository.findById(testGpId)).thenReturn(Optional.of(gp));
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);
            when(modelMapper.map(any(Patient.class), eq(PatientViewDTO.class))).thenReturn(viewDTO);

            PatientViewDTO result = patientService.registerPatient(dto);

            assertNotNull(result);
            assertEquals(patient.getId(), result.getId());
            assertEquals(testKeycloakId, result.getKeycloakId());
            assertEquals(testName, result.getName());
            assertEquals(testEgn, result.getEgn());
            verify(patientRepository).save(any(Patient.class));
        }

        @Test
        @DisplayName("registerPatient_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase")
        void registerPatient_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase() {
            String testKeycloakId = "test-keycloak-id";
            String testName = "New Patient";
            String testEgn = TestDataUtils.generateValidEgn();

            when(oidcUser.getSubject()).thenReturn(testKeycloakId);
            when(oidcUser.getFullName()).thenReturn(testName);

            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setEgn(testEgn);
            dto.setGeneralPractitionerId(2L);

            when(patientRepository.findByEgn(testEgn)).thenReturn(Optional.of(new Patient()));

            assertThrows(InvalidDTOException.class, () -> patientService.registerPatient(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("registerPatient_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase")
        void registerPatient_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase() {
            String testKeycloakId = "test-keycloak-id";
            String testName = "New Patient";
            String testEgn = TestDataUtils.generateValidEgn();
            Long testGpId = 2L;

            when(oidcUser.getSubject()).thenReturn(testKeycloakId);
            when(oidcUser.getFullName()).thenReturn(testName);

            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setEgn(testEgn);
            dto.setGeneralPractitionerId(testGpId);

            when(patientRepository.findByEgn(testEgn)).thenReturn(Optional.empty());
            when(patientRepository.findByKeycloakId(testKeycloakId)).thenReturn(Optional.empty());
            when(doctorRepository.findById(testGpId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> patientService.registerPatient(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("registerPatient_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase")
        void registerPatient_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase() {
            String testKeycloakId = "test-keycloak-id";
            String testName = "New Patient";
            String testEgn = TestDataUtils.generateValidEgn();
            Long testGpId = 2L;

            when(oidcUser.getSubject()).thenReturn(testKeycloakId);
            when(oidcUser.getFullName()).thenReturn(testName);

            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setEgn(testEgn);
            dto.setGeneralPractitionerId(testGpId);

            Doctor nonGp = setupDoctor(testGpId, false);

            when(patientRepository.findByEgn(testEgn)).thenReturn(Optional.empty());
            when(patientRepository.findByKeycloakId(testKeycloakId)).thenReturn(Optional.empty());
            when(doctorRepository.findById(testGpId)).thenReturn(Optional.of(nonGp));

            assertThrows(InvalidDTOException.class, () -> patientService.registerPatient(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }
    }

    @Nested
    @DisplayName("Update Patient Functionality")
    class UpdateTests {
        @Test
        @DisplayName("update_AsAdmin_WithValidDTO_ShouldReturnPatientViewDTO_HappyPath")
        void update_AsAdmin_WithValidDTO_ShouldReturnPatientViewDTO_HappyPath() {
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

            Long patientId = 1L;
            String originalEgn = TestDataUtils.generateValidEgn();
            String originalKeycloakId = "original-keycloak-id";
            String originalName = "Original Patient";
            Long originalGpId = 10L;
            LocalDate originalInsuranceDate = LocalDate.now().minusMonths(10);

            Patient existingPatient = setupPatient(patientId, originalKeycloakId, originalEgn, originalName, originalGpId, true, originalInsuranceDate);

            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(patientId);
            dto.setName("Admin Updated Patient");
            dto.setEgn(TestDataUtils.generateValidEgn());
            dto.setGeneralPractitionerId(11L);
            dto.setKeycloakId("new-keycloak-id");
            dto.setLastInsurancePaymentDate(LocalDate.now().minusMonths(2));

            Doctor newGp = setupDoctor(11L, true);

            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setId(patientId);
            viewDTO.setName(dto.getName());
            viewDTO.setEgn(dto.getEgn());
            viewDTO.setKeycloakId(dto.getKeycloakId());
            viewDTO.setGeneralPractitionerId(newGp.getId());
            viewDTO.setLastInsurancePaymentDate(dto.getLastInsurancePaymentDate());

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));
            when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.empty()); // New EGN is unique
            when(doctorRepository.findById(dto.getGeneralPractitionerId())).thenReturn(Optional.of(newGp));
            when(patientRepository.save(any(Patient.class))).thenReturn(existingPatient);
            when(modelMapper.map(any(Patient.class), eq(PatientViewDTO.class))).thenReturn(viewDTO);

            PatientViewDTO result = patientService.update(dto);

            assertNotNull(result);
            assertEquals(dto.getName(), result.getName());
            assertEquals(dto.getEgn(), result.getEgn());
            assertEquals(dto.getKeycloakId(), result.getKeycloakId());
            assertEquals(dto.getGeneralPractitionerId(), result.getGeneralPractitionerId());
            assertEquals(dto.getLastInsurancePaymentDate(), result.getLastInsurancePaymentDate());

            verify(patientRepository).save(any(Patient.class));
        }

        @Test
        @DisplayName("update_WithPartialDTO_ShouldUpdateOnlyProvidedFields_HappyPath")
        void update_WithPartialDTO_ShouldUpdateOnlyProvidedFields_HappyPath() {
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

            Long patientId = 1L;
            String originalEgn = TestDataUtils.generateValidEgn();
            String originalKeycloakId = "original-keycloak-id";
            String originalName = "Original Patient";
            Long originalGpId = 10L;
            LocalDate originalInsuranceDate = LocalDate.now().minusMonths(10);

            Patient existingPatient = setupPatient(patientId, originalKeycloakId, originalEgn, originalName, originalGpId, true, originalInsuranceDate);

            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(patientId);
            dto.setName("Partially Updated Name"); // Only update name
            // Other fields are null in DTO

            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setId(patientId);
            viewDTO.setName(dto.getName());
            viewDTO.setEgn(originalEgn);
            viewDTO.setKeycloakId(originalKeycloakId);
            viewDTO.setGeneralPractitionerId(originalGpId);
            viewDTO.setLastInsurancePaymentDate(originalInsuranceDate);

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));
            when(patientRepository.save(any(Patient.class))).thenReturn(existingPatient);
            when(modelMapper.map(any(Patient.class), eq(PatientViewDTO.class))).thenReturn(viewDTO);

            PatientViewDTO result = patientService.update(dto);

            assertNotNull(result);
            assertEquals(dto.getName(), result.getName());
            assertEquals(originalEgn, result.getEgn()); // Should remain unchanged
            assertEquals(originalKeycloakId, result.getKeycloakId()); // Should remain unchanged
            assertEquals(originalGpId, result.getGeneralPractitionerId()); // Should remain unchanged
            assertEquals(originalInsuranceDate, result.getLastInsurancePaymentDate()); // Should remain unchanged

            verify(patientRepository).save(any(Patient.class));
        }

        @Test
        @DisplayName("update_WithNonExistentPatient_ShouldThrowEntityNotFoundException_ErrorCase")
        void update_WithNonExistentPatient_ShouldThrowEntityNotFoundException_ErrorCase() {
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(99L);
            dto.setName("Non Existent");

            when(patientRepository.findById(dto.getId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> patientService.update(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("update_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase")
        void update_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase() {
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(1L);
            dto.setEgn(TestDataUtils.generateValidEgn());

            Patient existingPatient = setupPatient(1L, "keycloak1", TestDataUtils.generateValidEgn(), "Patient1", 10L, true, LocalDate.now());
            Patient anotherPatientWithEgn = setupPatient(2L, "keycloak2", dto.getEgn(), "Patient2", 11L, true, LocalDate.now());

            when(patientRepository.findById(dto.getId())).thenReturn(Optional.of(existingPatient));
            when(patientRepository.findByEgn(dto.getEgn())).thenReturn(Optional.of(anotherPatientWithEgn));

            assertThrows(InvalidDTOException.class, () -> patientService.update(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("update_WithKeycloakIdAlreadyInUseByAnotherPatient_ShouldThrowInvalidDTOException_ErrorCase")
        void update_WithKeycloakIdAlreadyInUseByAnotherPatient_ShouldThrowInvalidDTOException_ErrorCase() {
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

            Long patientId = 1L;
            String originalKeycloakId = "original-keycloak-id";
            String newKeycloakId = "keycloak-id-in-use";

            Patient existingPatient = setupPatient(patientId, originalKeycloakId, TestDataUtils.generateValidEgn(), "Patient1", 10L, true, LocalDate.now());
            Patient anotherPatient = setupPatient(2L, newKeycloakId, TestDataUtils.generateValidEgn(), "Patient2", 11L, true, LocalDate.now());

            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(patientId);
            dto.setKeycloakId(newKeycloakId);
            dto.setEgn(existingPatient.getEgn()); // Keep EGN same or unique

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));
            when(patientRepository.findByEgn(existingPatient.getEgn())).thenReturn(Optional.of(existingPatient)); // EGN is not changing or is unique
            when(patientRepository.findByKeycloakId(newKeycloakId)).thenReturn(Optional.of(anotherPatient));

            assertThrows(InvalidDTOException.class, () -> patientService.update(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("update_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase")
        void update_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase() {
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

            Long patientId = 1L;
            Long nonExistentGpId = 99L;

            Patient existingPatient = setupPatient(patientId, "keycloak1", TestDataUtils.generateValidEgn(), "Patient1", 10L, true, LocalDate.now());

            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(patientId);
            dto.setGeneralPractitionerId(nonExistentGpId);
            dto.setEgn(existingPatient.getEgn()); // Keep EGN same or unique

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));
            when(patientRepository.findByEgn(existingPatient.getEgn())).thenReturn(Optional.of(existingPatient));
            when(doctorRepository.findById(nonExistentGpId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> patientService.update(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("update_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase")
        void update_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase() {
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();

            Long patientId = 1L;
            Long nonGpDoctorId = 99L;

            Patient existingPatient = setupPatient(patientId, "keycloak1", TestDataUtils.generateValidEgn(), "Patient1", 10L, true, LocalDate.now());
            Doctor nonGpDoctor = setupDoctor(nonGpDoctorId, false);

            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(patientId);
            dto.setGeneralPractitionerId(nonGpDoctorId);
            dto.setEgn(existingPatient.getEgn()); // Keep EGN same or unique

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));
            when(patientRepository.findByEgn(existingPatient.getEgn())).thenReturn(Optional.of(existingPatient));
            when(doctorRepository.findById(nonGpDoctorId)).thenReturn(Optional.of(nonGpDoctor));

            assertThrows(InvalidDTOException.class, () -> patientService.update(dto));
            verify(patientRepository, never()).save(any(Patient.class));
        }
    }

    @Nested
    @DisplayName("Delete Patient Functionality")
    class DeleteTests {
        @Test
        @DisplayName("delete_WithValidId_ShouldSucceed_HappyPath")
        void delete_WithValidId_ShouldSucceed_HappyPath() {
            Long id = 1L;
            when(patientRepository.existsById(id)).thenReturn(true);

            patientService.delete(id);

            verify(patientRepository).deleteById(id);
        }

        @Test
        @DisplayName("delete_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase")
        void delete_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
            Long id = 99L;
            when(patientRepository.existsById(id)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> patientService.delete(id));
            verify(patientRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Get and Find Patient Functionality")
    class GetAndFindTests {
        @Test
        @DisplayName("getById_AsPatientOwner_ShouldSucceed_HappyPath")
        void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
            // ARRANGE
            String keycloakId = "patient-owner-id";
            when(authentication.getPrincipal()).thenReturn(oidcUser);
            when(oidcUser.getSubject()).thenReturn(keycloakId);
            when(authentication.getName()).thenReturn(keycloakId);
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

            long patientId = 1L;
            Patient patient = setupPatient(patientId, keycloakId, TestDataUtils.generateValidEgn(), "Owner", 1L, true, LocalDate.now());
            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setId(patientId);

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

            // ACT & ASSERT
            assertDoesNotThrow(() -> patientService.getById(patientId));
            verify(modelMapper).map(patient, PatientViewDTO.class);
        }

        @Test
        @DisplayName("getById_AsOtherPatient_ShouldThrowAccessDeniedException_ErrorCase")
        void getById_AsOtherPatient_ShouldThrowAccessDeniedException_ErrorCase() {
            // ARRANGE
            String otherPatientId = "other-patient-id";
            when(authentication.getPrincipal()).thenReturn(oidcUser);
            when(oidcUser.getSubject()).thenReturn(otherPatientId);
            when(authentication.getName()).thenReturn(otherPatientId);
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

            long patientIdToAccess = 1L;
            Patient patientToAccess = setupPatient(patientIdToAccess, "patient-owner-id", TestDataUtils.generateValidEgn(), "Owner", 1L, true, LocalDate.now());

            when(patientRepository.findById(patientIdToAccess)).thenReturn(Optional.of(patientToAccess));

            // ACT & ASSERT
            assertThrows(AccessDeniedException.class, () -> patientService.getById(patientIdToAccess));
        }

        @Test
        @DisplayName("getAll_WithFilter_ShouldCallCorrectRepositoryMethod_HappyPath")
        void getAll_WithFilter_ShouldCallCorrectRepositoryMethod_HappyPath() {
            String filter = "test-filter";
            when(patientRepository.findByEgnContaining(anyString(), any())).thenReturn(Page.empty());

            CompletableFuture<Page<PatientViewDTO>> future = patientService.getAll(0, 10, "name", true, filter);
            future.join();

            verify(patientRepository).findByEgnContaining(eq(filter), any(Pageable.class));
            verify(patientRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("getPatientCountByGeneralPractitioner_ShouldReturnList_HappyPath")
        void getPatientCountByGeneralPractitioner_ShouldReturnList_HappyPath() {
            List<DoctorPatientCountDTO> countList = List.of(DoctorPatientCountDTO.builder().doctor(new Doctor()).patientCount(5L).build());
            when(doctorRepository.findPatientCountByGeneralPractitioner()).thenReturn(countList);

            List<DoctorPatientCountDTO> result = patientService.getPatientCountByGeneralPractitioner();

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            verify(doctorRepository).findPatientCountByGeneralPractitioner();
        }

        @Test
        @DisplayName("getByEgn_WithValidEgn_ShouldReturnPatientViewDTO_HappyPath")
        void getByEgn_WithValidEgn_ShouldReturnPatientViewDTO_HappyPath() {
            // ARRANGE
            String keycloakId = "keycloak-id";
            String egn = TestDataUtils.generateValidEgn();
            Patient patient = setupPatient(1L, keycloakId, egn, "Test Patient", 1L, true, LocalDate.now());
            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setEgn(egn);

            when(patientRepository.findByEgn(egn)).thenReturn(Optional.of(patient));
            when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);
            when(authentication.getPrincipal()).thenReturn(oidcUser);
            when(oidcUser.getSubject()).thenReturn(keycloakId);
            when(authentication.getName()).thenReturn(keycloakId); // FIX: Add this missing mock
            doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))).when(authentication).getAuthorities();

            // ACT
            PatientViewDTO result = patientService.getByEgn(egn);

            // ASSERT
            assertNotNull(result);
            assertEquals(egn, result.getEgn());
            verify(patientRepository).findByEgn(egn);
            verify(modelMapper).map(patient, PatientViewDTO.class);
        }

        @Test
        @DisplayName("getByEgn_WithNonExistentEgn_ShouldThrowEntityNotFoundException_ErrorCase")
        void getByEgn_WithNonExistentEgn_ShouldThrowEntityNotFoundException_ErrorCase() {
            String egn = TestDataUtils.generateValidEgn();
            when(patientRepository.findByEgn(egn)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> patientService.getByEgn(egn));
            verify(patientRepository).findByEgn(egn);
            verify(modelMapper, never()).map(any(Patient.class), any(PatientViewDTO.class));
        }

        @Test
        @DisplayName("getByKeycloakId_WithValidId_ShouldReturnPatientViewDTO_HappyPath")
        void getByKeycloakId_WithValidId_ShouldReturnPatientViewDTO_HappyPath() {
            String keycloakId = "valid-keycloak-id";
            Patient patient = setupPatient(1L, keycloakId, TestDataUtils.generateValidEgn(), "Test Patient", 1L, true, LocalDate.now());
            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setKeycloakId(keycloakId);

            when(patientRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(patient));
            when(modelMapper.map(patient, PatientViewDTO.class)).thenReturn(viewDTO);

            PatientViewDTO result = patientService.getByKeycloakId(keycloakId);

            assertNotNull(result);
            assertEquals(keycloakId, result.getKeycloakId());
            verify(patientRepository).findByKeycloakId(keycloakId);
            verify(modelMapper).map(patient, PatientViewDTO.class);
        }

        @Test
        @DisplayName("getByKeycloakId_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase")
        void getByKeycloakId_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
            String keycloakId = "non-existent-keycloak-id";
            when(patientRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> patientService.getByKeycloakId(keycloakId));
            verify(patientRepository).findByKeycloakId(keycloakId);
            verify(modelMapper, never()).map(any(Patient.class), any(PatientViewDTO.class));
        }
    }

    @Nested
    @DisplayName("Update Insurance Status Functionality")
    class UpdateInsuranceStatusTests {
        @Test
        @DisplayName("updateInsuranceStatus_WithValidId_ShouldUpdateDate_HappyPath")
        void updateInsuranceStatus_WithValidId_ShouldUpdateDate_HappyPath() {
            Long patientId = 1L;
            Patient patient = setupPatient(patientId, "keycloak-id", TestDataUtils.generateValidEgn(), "Test Patient", 1L, true, LocalDate.now().minusYears(1));
            PatientViewDTO viewDTO = new PatientViewDTO();
            viewDTO.setId(patientId);
            viewDTO.setLastInsurancePaymentDate(LocalDate.now());

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);
            when(modelMapper.map(any(Patient.class), eq(PatientViewDTO.class))).thenReturn(viewDTO);

            PatientViewDTO result = patientService.updateInsuranceStatus(patientId);

            assertNotNull(result);
            assertEquals(LocalDate.now(), result.getLastInsurancePaymentDate());
            verify(patientRepository).save(any(Patient.class));
        }

        @Test
        @DisplayName("updateInsuranceStatus_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase")
        void updateInsuranceStatus_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
            Long patientId = 99L;
            when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> patientService.updateInsuranceStatus(patientId));
            verify(patientRepository, never()).save(any(Patient.class));
        }
    }

    @Nested
    @DisplayName("Get By General Practitioner Functionality")
    class GetByGeneralPractitionerTests {
        @Test
        @DisplayName("getByGeneralPractitioner_WithValidGpId_ShouldReturnPageOfPatients_HappyPath")
        void getByGeneralPractitioner_WithValidGpId_ShouldReturnPageOfPatients_HappyPath() {
            Long gpId = 1L;
            Doctor gp = setupDoctor(gpId, true);
            Patient patient1 = setupPatient(1L, "keycloak1", TestDataUtils.generateValidEgn(), "Patient1", gpId, true, LocalDate.now());
            Patient patient2 = setupPatient(2L, "keycloak2", TestDataUtils.generateValidEgn(), "Patient2", gpId, true, LocalDate.now());
            List<Patient> patients = List.of(patient1, patient2);
            Page<Patient> patientPage = new PageImpl<>(patients);

            PatientViewDTO viewDTO1 = new PatientViewDTO();
            viewDTO1.setId(patient1.getId());
            PatientViewDTO viewDTO2 = new PatientViewDTO();
            viewDTO2.setId(patient2.getId());

            when(doctorRepository.findById(gpId)).thenReturn(Optional.of(gp));
            when(patientRepository.findByGeneralPractitioner(eq(gp), any(Pageable.class))).thenReturn(patientPage);
            when(modelMapper.map(patient1, PatientViewDTO.class)).thenReturn(viewDTO1);
            when(modelMapper.map(patient2, PatientViewDTO.class)).thenReturn(viewDTO2);

            Page<PatientViewDTO> result = patientService.getByGeneralPractitioner(gpId, 0, 10);

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            verify(doctorRepository).findById(gpId);
            verify(patientRepository).findByGeneralPractitioner(eq(gp), any(Pageable.class));
        }

        @Test
        @DisplayName("getByGeneralPractitioner_WithNonExistentGpId_ShouldThrowEntityNotFoundException_ErrorCase")
        void getByGeneralPractitioner_WithNonExistentGpId_ShouldThrowEntityNotFoundException_ErrorCase() {
            Long gpId = 99L;
            when(doctorRepository.findById(gpId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> patientService.getByGeneralPractitioner(gpId, 0, 10));
            verify(patientRepository, never()).findByGeneralPractitioner(any(Doctor.class), any(Pageable.class));
        }

        @Test
        @DisplayName("getByGeneralPractitioner_WithNonGpDoctor_ShouldThrowInvalidPatientException_ErrorCase")
        void getByGeneralPractitioner_WithNonGpDoctor_ShouldThrowInvalidPatientException_ErrorCase() {
            Long gpId = 1L;
            Doctor nonGp = setupDoctor(gpId, false);

            when(doctorRepository.findById(gpId)).thenReturn(Optional.of(nonGp));

            assertThrows(InvalidPatientException.class, () -> patientService.getByGeneralPractitioner(gpId, 0, 10));
            verify(patientRepository, never()).findByGeneralPractitioner(any(Doctor.class), any(Pageable.class));
        }
    }
}
