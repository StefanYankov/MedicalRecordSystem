package nbu.cscb869.services.services.integrationtests;

import nbu.cscb869.MedicalRecordSystemApplication;
import nbu.cscb869.common.exceptions.EntityNotFoundException;
import nbu.cscb869.common.exceptions.InvalidDTOException;
import nbu.cscb869.common.exceptions.InvalidPatientException;
import nbu.cscb869.config.KeycloakAdminConfig;
import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.security.WithMockKeycloakUser;
import nbu.cscb869.services.data.dtos.PatientCreateDTO;
import nbu.cscb869.services.data.dtos.PatientUpdateDTO;
import nbu.cscb869.services.data.dtos.PatientViewDTO;
import nbu.cscb869.services.services.contracts.PatientService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = MedicalRecordSystemApplication.class)
@ActiveProfiles("test")
@Transactional
@Import({PatientServiceImplIntegrationTests.AsyncTestConfig.class, PatientServiceImplIntegrationTests.TestConfig.class, PatientServiceImplIntegrationTests.MockKeycloakAdminConfig.class})
@DisplayName("PatientServiceImpl Integration Tests")
class PatientServiceImplIntegrationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistrationRepository repo = Mockito.mock(ClientRegistrationRepository.class);
            ClientRegistration registration = Mockito.mock(ClientRegistration.class);
            ClientRegistration.ProviderDetails providerDetails = Mockito.mock(ClientRegistration.ProviderDetails.class);

            // Crucial stubs to allow KeycloakAdminConfig to initialize
            when(providerDetails.getIssuerUri()).thenReturn("http://localhost:8081/realms/test-realm");
            when(registration.getProviderDetails()).thenReturn(providerDetails);
            when(registration.getClientId()).thenReturn("test-client");
            when(registration.getClientSecret()).thenReturn("test-secret");
            when(repo.findByRegistrationId("keycloak")).thenReturn(registration);

            return repo;
        }

        @Bean
        @Primary
        public JwtDecoder jwtDecoder() {
            return Mockito.mock(JwtDecoder.class);
        }
    }

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    // TestConfiguration to mock the Keycloak admin client
    @TestConfiguration
    static class MockKeycloakAdminConfig {
        @Bean
        @Primary // Ensure this mock takes precedence over the real Keycloak bean
        public org.keycloak.admin.client.Keycloak keycloak() {
            return mock(org.keycloak.admin.client.Keycloak.class);
        }
    }

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private VisitRepository visitRepository;

    private Doctor testDoctor;
    private Doctor nonGpDoctor;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        doctorRepository.deleteAll();

        testDoctor = new Doctor();
        testDoctor.setName("Dr. House");
        testDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        testDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        testDoctor.setGeneralPractitioner(true);
        doctorRepository.save(testDoctor);

        nonGpDoctor = new Doctor();
        nonGpDoctor.setName("Dr. NonGP");
        nonGpDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        nonGpDoctor.setKeycloakId(TestDataUtils.generateKeycloakId());
        nonGpDoctor.setGeneralPractitioner(false);
        doctorRepository.save(nonGpDoctor);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Create Patient Functionality")
    class CreateTests {
        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("create_AsAdmin_WithValidDTO_ShouldSucceed_HappyPath")
        void create_AsAdmin_WithValidDTO_ShouldSucceed_HappyPath() {
            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("Patient by Admin");
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(testDoctor.getId());
            createDTO.setKeycloakId(TestDataUtils.generateKeycloakId());

            PatientViewDTO result = patientService.create(createDTO);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("Patient by Admin", result.getName());
            assertTrue(patientRepository.findById(result.getId()).isPresent());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
        @DisplayName("create_AsPatient_ShouldBeDenied_ErrorCase")
        void create_AsPatient_ShouldBeDenied_ErrorCase() {
            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("Attempt by Patient");
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(testDoctor.getId());
            createDTO.setKeycloakId("some-keycloak-id");

            assertThrows(AccessDeniedException.class, () -> patientService.create(createDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("create_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase")
        void create_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase() {
            Patient existingPatient = new Patient();
            existingPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
            existingPatient.setName("Existing EGN Patient");
            existingPatient.setEgn(TestDataUtils.generateValidEgn()); // Use valid EGN
            existingPatient.setGeneralPractitioner(testDoctor);
            patientRepository.save(existingPatient);

            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("New Patient");
            createDTO.setEgn(existingPatient.getEgn()); // Duplicate EGN
            createDTO.setGeneralPractitionerId(testDoctor.getId());
            createDTO.setKeycloakId(TestDataUtils.generateKeycloakId());

            assertThrows(InvalidDTOException.class, () -> patientService.create(createDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("create_WithExistingKeycloakId_ShouldThrowInvalidDTOException_ErrorCase")
        void create_WithExistingKeycloakId_ShouldThrowInvalidDTOException_ErrorCase() {
            Patient existingPatient = new Patient();
            existingPatient.setKeycloakId("existing-keycloak-id");
            existingPatient.setName("Existing Keycloak Patient");
            existingPatient.setEgn(TestDataUtils.generateValidEgn());
            existingPatient.setGeneralPractitioner(testDoctor);
            patientRepository.save(existingPatient);

            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("New Patient");
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(testDoctor.getId());
            createDTO.setKeycloakId("existing-keycloak-id"); // Duplicate Keycloak ID

            assertThrows(InvalidDTOException.class, () -> patientService.create(createDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("create_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase")
        void create_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase() {
            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("Patient with Bad GP");
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(999L); // Non-existent GP
            createDTO.setKeycloakId(TestDataUtils.generateKeycloakId());

            assertThrows(EntityNotFoundException.class, () -> patientService.create(createDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("create_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase")
        void create_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase() {
            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setName("Patient with Non-GP Doctor");
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(nonGpDoctor.getId()); // Non-GP Doctor
            createDTO.setKeycloakId(TestDataUtils.generateKeycloakId());

            assertThrows(InvalidDTOException.class, () -> patientService.create(createDTO));
        }
    }

    @Nested
    @DisplayName("Register Patient Functionality")
    class RegisterPatientTests {
        private OidcUser mockOidcUser;

        @BeforeEach
        void setupRegisterTest() {
            mockOidcUser = mock(OidcUser.class);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(mockOidcUser, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );
        }

        @Test
        @DisplayName("registerPatient_WithValidDataAsPatient_ShouldSucceed_HappyPath")
        void registerPatient_WithValidDataAsPatient_ShouldSucceed_HappyPath() {
            when(mockOidcUser.getSubject()).thenReturn("new-patient-id");
            when(mockOidcUser.getFullName()).thenReturn("New Patient");

            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(testDoctor.getId());

            PatientViewDTO result = patientService.registerPatient(createDTO);

            assertNotNull(result);
            assertEquals("New Patient", result.getName());
            assertNotNull(result.getId());

            Patient savedPatient = patientRepository.findById(result.getId()).orElse(null);
            assertNotNull(savedPatient);
            assertEquals("new-patient-id", savedPatient.getKeycloakId());
            assertEquals(testDoctor.getId(), savedPatient.getGeneralPractitioner().getId());
        }

        @Test
        @DisplayName("registerPatient_WhenPatientProfileAlreadyExists_ShouldThrowException_ErrorCase")
        void registerPatient_WhenPatientProfileAlreadyExists_ShouldThrowException_ErrorCase() {
            when(mockOidcUser.getSubject()).thenReturn("existing-patient-id");

            Patient existingPatient = new Patient();
            existingPatient.setKeycloakId("existing-patient-id");
            existingPatient.setName("Already Exists");
            existingPatient.setEgn(TestDataUtils.generateValidEgn());
            existingPatient.setGeneralPractitioner(testDoctor);
            patientRepository.save(existingPatient);

            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(testDoctor.getId());

            assertThrows(InvalidDTOException.class, () -> patientService.registerPatient(createDTO));
        }

        @Test
        @DisplayName("registerPatient_AsDoctor_ShouldBeDenied_ErrorCase")
        void registerPatient_AsDoctor_ShouldBeDenied_ErrorCase() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("user", "pass", Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR")))
            );
            PatientCreateDTO createDTO = new PatientCreateDTO();
            assertThrows(AccessDeniedException.class, () -> patientService.registerPatient(createDTO));
        }

        @Test
        @DisplayName("registerPatient_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase")
        void registerPatient_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase() {
            when(mockOidcUser.getSubject()).thenReturn("new-patient-id");

            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(999L); // Non-existent GP

            assertThrows(EntityNotFoundException.class, () -> patientService.registerPatient(createDTO));
        }

        @Test
        @DisplayName("registerPatient_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase")
        void registerPatient_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase() {
            when(mockOidcUser.getSubject()).thenReturn("new-patient-id");

            PatientCreateDTO createDTO = new PatientCreateDTO();
            createDTO.setEgn(TestDataUtils.generateValidEgn());
            createDTO.setGeneralPractitionerId(nonGpDoctor.getId()); // Non-GP Doctor

            assertThrows(InvalidDTOException.class, () -> patientService.registerPatient(createDTO));
        }
    }

    @Nested
    @DisplayName("Update Patient Functionality")
    class UpdateTests {
        private Patient patientToUpdate;

        @BeforeEach
        void setupUpdateTest() {
            patientToUpdate = new Patient();
            patientToUpdate.setKeycloakId(TestDataUtils.generateKeycloakId());
            patientToUpdate.setName("Original Name");
            patientToUpdate.setEgn(TestDataUtils.generateValidEgn());
            patientToUpdate.setGeneralPractitioner(testDoctor);
            patientRepository.save(patientToUpdate);
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("update_AsAdmin_WithFullDTO_ShouldUpdateAllFields_HappyPath")
        void update_AsAdmin_WithFullDTO_ShouldUpdateAllFields_HappyPath() {
            Doctor newGp = new Doctor();
            newGp.setName("Dr. New GP");
            newGp.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            newGp.setKeycloakId(TestDataUtils.generateKeycloakId());
            newGp.setGeneralPractitioner(true);
            doctorRepository.save(newGp);

            PatientUpdateDTO updateDTO = new PatientUpdateDTO();
            updateDTO.setId(patientToUpdate.getId());
            updateDTO.setName("Updated by Admin");
            updateDTO.setEgn(TestDataUtils.generateValidEgn());
            updateDTO.setGeneralPractitionerId(newGp.getId());
            updateDTO.setKeycloakId(TestDataUtils.generateKeycloakId());
            updateDTO.setLastInsurancePaymentDate(LocalDate.now().minusMonths(1));

            PatientViewDTO result = patientService.update(updateDTO);

            assertNotNull(result);
            assertEquals(updateDTO.getName(), result.getName());
            assertEquals(updateDTO.getEgn(), result.getEgn());
            assertEquals(updateDTO.getKeycloakId(), result.getKeycloakId());
            assertEquals(updateDTO.getGeneralPractitionerId(), result.getGeneralPractitionerId());
            assertEquals(updateDTO.getLastInsurancePaymentDate(), result.getLastInsurancePaymentDate());

            Patient updatedInDb = patientRepository.findById(patientToUpdate.getId()).get();
            assertEquals(updateDTO.getName(), updatedInDb.getName());
            assertEquals(updateDTO.getEgn(), updatedInDb.getEgn());
            assertEquals(updateDTO.getKeycloakId(), updatedInDb.getKeycloakId());
            assertEquals(updateDTO.getGeneralPractitionerId(), updatedInDb.getGeneralPractitioner().getId());
            assertEquals(updateDTO.getLastInsurancePaymentDate(), updatedInDb.getLastInsurancePaymentDate());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("update_WithPartialDTO_ShouldUpdateOnlyProvidedFields_HappyPath")
        void update_WithPartialDTO_ShouldUpdateOnlyProvidedFields_HappyPath() {
            String originalName = patientToUpdate.getName();
            String originalEgn = patientToUpdate.getEgn();
            String originalKeycloakId = patientToUpdate.getKeycloakId();
            Long originalGpId = patientToUpdate.getGeneralPractitioner().getId();
            LocalDate originalInsuranceDate = patientToUpdate.getLastInsurancePaymentDate();

            PatientUpdateDTO updateDTO = new PatientUpdateDTO();
            updateDTO.setId(patientToUpdate.getId());
            updateDTO.setName("Partial Update Name"); // Only update name
            // Other fields are null in DTO, should not change in entity

            PatientViewDTO result = patientService.update(updateDTO);

            assertNotNull(result);
            assertEquals("Partial Update Name", result.getName());
            assertEquals(originalEgn, result.getEgn());
            assertEquals(originalKeycloakId, result.getKeycloakId());
            assertEquals(originalGpId, result.getGeneralPractitionerId());
            assertEquals(originalInsuranceDate, result.getLastInsurancePaymentDate());

            Patient updatedInDb = patientRepository.findById(patientToUpdate.getId()).get();
            assertEquals("Partial Update Name", updatedInDb.getName());
            assertEquals(originalEgn, updatedInDb.getEgn());
            assertEquals(originalKeycloakId, updatedInDb.getKeycloakId());
            assertEquals(originalGpId, updatedInDb.getGeneralPractitioner().getId());
            assertEquals(originalInsuranceDate, updatedInDb.getLastInsurancePaymentDate());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("update_WithNonExistentPatient_ShouldThrowEntityNotFoundException_ErrorCase")
        void update_WithNonExistentPatient_ShouldThrowEntityNotFoundException_ErrorCase() {
            PatientUpdateDTO updateDTO = new PatientUpdateDTO();
            updateDTO.setId(999L); // Non-existent ID
            updateDTO.setName("Non Existent");

            assertThrows(EntityNotFoundException.class, () -> patientService.update(updateDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("update_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase")
        void update_WithExistingEgn_ShouldThrowInvalidDTOException_ErrorCase() {
            Patient anotherPatient = new Patient();
            anotherPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
            anotherPatient.setName("Another Patient");
            anotherPatient.setEgn(TestDataUtils.generateValidEgn()); // Use valid EGN
            anotherPatient.setGeneralPractitioner(testDoctor);
            patientRepository.save(anotherPatient);

            PatientUpdateDTO updateDTO = new PatientUpdateDTO();
            updateDTO.setId(patientToUpdate.getId());
            updateDTO.setEgn(anotherPatient.getEgn()); // EGN already used by anotherPatient

            assertThrows(InvalidDTOException.class, () -> patientService.update(updateDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("update_WithKeycloakIdAlreadyInUseByAnotherPatient_ShouldThrowInvalidDTOException_ErrorCase")
        void update_WithKeycloakIdAlreadyInUseByAnotherPatient_ShouldThrowInvalidDTOException_ErrorCase() {
            Patient anotherPatient = new Patient();
            anotherPatient.setKeycloakId("keycloak-id-in-use");
            anotherPatient.setName("Another Patient");
            anotherPatient.setEgn(TestDataUtils.generateValidEgn());
            anotherPatient.setGeneralPractitioner(testDoctor);
            patientRepository.save(anotherPatient);

            PatientUpdateDTO updateDTO = new PatientUpdateDTO();
            updateDTO.setId(patientToUpdate.getId());
            updateDTO.setKeycloakId("keycloak-id-in-use"); // Keycloak ID already used by anotherPatient

            assertThrows(InvalidDTOException.class, () -> patientService.update(updateDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("update_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase")
        void update_WithNonExistentGP_ShouldThrowEntityNotFoundException_ErrorCase() {
            PatientUpdateDTO updateDTO = new PatientUpdateDTO();
            updateDTO.setId(patientToUpdate.getId());
            updateDTO.setGeneralPractitionerId(999L); // Non-existent GP

            assertThrows(EntityNotFoundException.class, () -> patientService.update(updateDTO));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("update_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase")
        void update_WithNonGpDoctor_ShouldThrowInvalidDTOException_ErrorCase() {
            PatientUpdateDTO updateDTO = new PatientUpdateDTO();
            updateDTO.setId(patientToUpdate.getId());
            updateDTO.setGeneralPractitionerId(nonGpDoctor.getId()); // Non-GP Doctor

            assertThrows(InvalidDTOException.class, () -> patientService.update(updateDTO));
        }
    }

    @Nested
    @DisplayName("Delete Patient Functionality")
    class DeleteTests {
        private Patient patientToDelete;

        @BeforeEach
        void setupDeleteTest() {
            patientToDelete = new Patient();
            patientToDelete.setKeycloakId(TestDataUtils.generateKeycloakId());
            patientToDelete.setName("To Delete");
            patientToDelete.setEgn(TestDataUtils.generateValidEgn());
            patientToDelete.setGeneralPractitioner(testDoctor);
            patientRepository.save(patientToDelete);
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("delete_AsAdmin_ShouldSucceed_HappyPath")
        void delete_AsAdmin_ShouldSucceed_HappyPath() {
            long patientId = patientToDelete.getId();
            patientService.delete(patientId);
            assertFalse(patientRepository.findById(patientId).isPresent());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
        @DisplayName("delete_AsPatient_ShouldBeDenied_ErrorCase")
        void delete_AsPatient_ShouldBeDenied_ErrorCase() {
            long patientId = patientToDelete.getId();
            assertThrows(AccessDeniedException.class, () -> patientService.delete(patientId));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("delete_NonExistentPatient_ShouldThrowEntityNotFoundException_ErrorCase")
        void delete_NonExistentPatient_ShouldThrowEntityNotFoundException_ErrorCase() {
            assertThrows(EntityNotFoundException.class, () -> patientService.delete(999L));
        }
    }

    @Nested
    @DisplayName("Get and Find Patient Functionality")
    class GetAndFindTests {
        private Patient patientOwner;
        private Patient anotherPatient;

        @BeforeEach
        void setupGetAndFindTests() {
            patientOwner = new Patient();
            patientOwner.setKeycloakId(TestDataUtils.generateKeycloakId()); // Use generated Keycloak ID
            patientOwner.setName("Patient Owner");
            patientOwner.setEgn(TestDataUtils.generateValidEgn());
            patientOwner.setGeneralPractitioner(testDoctor);
            patientRepository.save(patientOwner);

            anotherPatient = new Patient();
            anotherPatient.setKeycloakId(TestDataUtils.generateKeycloakId());
            anotherPatient.setName("Another Patient");
            anotherPatient.setEgn(TestDataUtils.generateValidEgn());
            anotherPatient.setGeneralPractitioner(testDoctor);
            patientRepository.save(anotherPatient);

            // Ensure data is committed for subsequent queries in the same test class
            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();
        }

        @Test
        @DisplayName("getById_AsPatientOwner_ShouldSucceed_HappyPath")
        void getById_AsPatientOwner_ShouldSucceed_HappyPath() {
            // Manually set security context to provide a mocked OidcUser
            OidcUser mockOidcUser = mock(OidcUser.class);
            when(mockOidcUser.getSubject()).thenReturn(patientOwner.getKeycloakId()); // Use dynamically generated Keycloak ID
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(mockOidcUser, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );

            PatientViewDTO result = patientService.getById(patientOwner.getId());
            assertNotNull(result);
            assertEquals(patientOwner.getId(), result.getId());
            assertEquals("Patient Owner", result.getName());
        }

        @Test
        @DisplayName("getById_AsOtherPatient_ShouldBeDenied_ErrorCase")
        void getById_AsOtherPatient_ShouldBeDenied_ErrorCase() {
            // Manually set security context to provide a mocked OidcUser for the 'other' patient
            OidcUser mockOidcUser = mock(OidcUser.class);
            when(mockOidcUser.getSubject()).thenReturn("other-patient-id"); // Keycloak ID for the user performing the action
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(mockOidcUser, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );
            assertThrows(AccessDeniedException.class, () -> patientService.getById(patientOwner.getId()));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        @DisplayName("getById_AsDoctor_ShouldSucceed_HappyPath")
        void getById_AsDoctor_ShouldSucceed_HappyPath() {
            PatientViewDTO result = patientService.getById(patientOwner.getId());
            assertNotNull(result);
            assertEquals(patientOwner.getId(), result.getId());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("getAll_AsAdmin_ShouldReturnPageOfPatients_HappyPath")
        void getAll_AsAdmin_ShouldReturnPageOfPatients_HappyPath() throws ExecutionException, InterruptedException {
            CompletableFuture<Page<PatientViewDTO>> future = patientService.getAll(0, 10, "name", true, null);
            Page<PatientViewDTO> result = future.get();
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.getTotalElements() >= 2); // At least patientOwner and anotherPatient
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_PATIENT")
        @DisplayName("getAll_AsPatient_ShouldBeDenied_ErrorCase")
        void getAll_AsPatient_ShouldBeDenied_ErrorCase() {
            // This test expects AccessDeniedException. If the service method is not secured,
            // it might throw a different exception or succeed. Requires @PreAuthorize("hasRole('ADMIN')")
            // on the service's getAll method for this to pass as expected.
            CompletableFuture<Page<PatientViewDTO>> future = patientService.getAll(0, 10, "name", true, null);
            ExecutionException exception = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(AccessDeniedException.class, exception.getCause());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("getPatientCountByGeneralPractitioner_AsAdmin_ShouldSucceed_HappyPath")
        void getPatientCountByGeneralPractitioner_AsAdmin_ShouldSucceed_HappyPath() {
            Patient patient3 = new Patient();
            patient3.setKeycloakId(TestDataUtils.generateKeycloakId());
            patient3.setName("Patient 3");
            patient3.setEgn(TestDataUtils.generateValidEgn());
            patient3.setGeneralPractitioner(testDoctor);
            patientRepository.save(patient3);

            List<DoctorPatientCountDTO> result = patientService.getPatientCountByGeneralPractitioner();
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.stream().anyMatch(dto -> dto.getDoctor().getId().equals(testDoctor.getId()) && dto.getPatientCount() >= 3));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_DOCTOR")
        @DisplayName("getPatientCountByGeneralPractitioner_AsDoctor_ShouldBeDenied_ErrorCase")
        void getPatientCountByGeneralPractitioner_AsDoctor_ShouldBeDenied_ErrorCase() {
            assertThrows(AccessDeniedException.class, () -> patientService.getPatientCountByGeneralPractitioner());
        }

        @Test
        @DisplayName("getByEgn_WithValidEgn_ShouldReturnPatientViewDTO_HappyPath")
        void getByEgn_WithValidEgn_ShouldReturnPatientViewDTO_HappyPath() {
            // Manually set security context to provide a mocked OidcUser
            OidcUser mockOidcUser = mock(OidcUser.class);
            when(mockOidcUser.getSubject()).thenReturn(patientOwner.getKeycloakId()); // Keycloak ID for the user performing the action
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(mockOidcUser, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT")))
            );

            PatientViewDTO result = patientService.getByEgn(patientOwner.getEgn());
            assertNotNull(result);
            assertEquals(patientOwner.getEgn(), result.getEgn());
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
        @DisplayName("getByEgn_WithNonExistentEgn_ShouldThrowEntityNotFoundException_ErrorCase")
        void getByEgn_WithNonExistentEgn_ShouldThrowEntityNotFoundException_ErrorCase() {
            assertThrows(EntityNotFoundException.class, () -> patientService.getByEgn("9999999999"));
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
        @DisplayName("getByKeycloakId_WithValidId_ShouldReturnPatientViewDTO_HappyPath")
        void getByKeycloakId_WithValidId_ShouldReturnPatientViewDTO_HappyPath() {
            PatientViewDTO result = patientService.getByKeycloakId(patientOwner.getKeycloakId());
            assertNotNull(result);
            assertEquals(patientOwner.getKeycloakId(), result.getKeycloakId());
        }

        @Test
        @WithMockKeycloakUser(keycloakId = "patient-owner-id", authorities = "ROLE_PATIENT")
        @DisplayName("getByKeycloakId_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase")
        void getByKeycloakId_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
            assertThrows(EntityNotFoundException.class, () -> patientService.getByKeycloakId("non-existent-keycloak-id"));
        }
    }

    @Nested
    @DisplayName("Update Insurance Status Functionality")
    class UpdateInsuranceStatusTests {
        private Patient patientForInsurance;

        @BeforeEach
        void setupInsuranceTest() {
            patientForInsurance = new Patient();
            patientForInsurance.setKeycloakId(TestDataUtils.generateKeycloakId());
            patientForInsurance.setName("Insurance Patient");
            patientForInsurance.setEgn(TestDataUtils.generateValidEgn());
            patientForInsurance.setGeneralPractitioner(testDoctor);
            patientForInsurance.setLastInsurancePaymentDate(LocalDate.now().minusYears(1));
            patientRepository.save(patientForInsurance);
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("updateInsuranceStatus_WithValidId_ShouldUpdateDate_HappyPath")
        void updateInsuranceStatus_WithValidId_ShouldUpdateDate_HappyPath() {
            PatientViewDTO result = patientService.updateInsuranceStatus(patientForInsurance.getId());
            assertNotNull(result);
            assertEquals(LocalDate.now(), result.getLastInsurancePaymentDate());

            Patient updatedInDb = patientRepository.findById(patientForInsurance.getId()).get();
            assertEquals(LocalDate.now(), updatedInDb.getLastInsurancePaymentDate());
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("updateInsuranceStatus_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase")
        void updateInsuranceStatus_WithNonExistentId_ShouldThrowEntityNotFoundException_ErrorCase() {
            assertThrows(EntityNotFoundException.class, () -> patientService.updateInsuranceStatus(999L));
        }
    }

    @Nested
    @DisplayName("Get By General Practitioner Functionality")
    class GetByGeneralPractitionerTests {
        private Doctor gpWithPatients;
        private Patient patient1;
        private Patient patient2;

        @BeforeEach
        void setupGpTests() {
            gpWithPatients = new Doctor();
            gpWithPatients.setName("Dr. GP with Patients");
            gpWithPatients.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
            gpWithPatients.setKeycloakId(TestDataUtils.generateKeycloakId());
            gpWithPatients.setGeneralPractitioner(true);
            doctorRepository.save(gpWithPatients);

            patient1 = new Patient();
            patient1.setKeycloakId(TestDataUtils.generateKeycloakId());
            patient1.setName("GP Patient 1");
            patient1.setEgn(TestDataUtils.generateValidEgn());
            patient1.setGeneralPractitioner(gpWithPatients);
            patientRepository.save(patient1);

            patient2 = new Patient();
            patient2.setKeycloakId(TestDataUtils.generateKeycloakId());
            patient2.setName("GP Patient 2");
            patient2.setEgn(TestDataUtils.generateValidEgn());
            patient2.setGeneralPractitioner(gpWithPatients);
            patientRepository.save(patient2);
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("getByGeneralPractitioner_WithValidGpId_ShouldReturnPageOfPatients_HappyPath")
        void getByGeneralPractitioner_WithValidGpId_ShouldReturnPageOfPatients_HappyPath() {
            Page<PatientViewDTO> result = patientService.getByGeneralPractitioner(gpWithPatients.getId(), 0, 10);
            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            assertTrue(result.stream().anyMatch(p -> p.getId().equals(patient1.getId())));
            assertTrue(result.stream().anyMatch(p -> p.getId().equals(patient2.getId())));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("getByGeneralPractitioner_WithNonExistentGpId_ShouldThrowEntityNotFoundException_ErrorCase")
        void getByGeneralPractitioner_WithNonExistentGpId_ShouldThrowEntityNotFoundException_ErrorCase() {
            assertThrows(EntityNotFoundException.class, () -> patientService.getByGeneralPractitioner(999L, 0, 10));
        }

        @Test
        @WithMockKeycloakUser(authorities = "ROLE_ADMIN")
        @DisplayName("getByGeneralPractitioner_WithNonGpDoctor_ShouldThrowInvalidPatientException_ErrorCase")
        void getByGeneralPractitioner_WithNonGpDoctor_ShouldThrowInvalidPatientException_ErrorCase() {
            // Changed expected exception type from InvalidDTOException to InvalidPatientException
            assertThrows(InvalidPatientException.class, () -> patientService.getByGeneralPractitioner(nonGpDoctor.getId(), 0, 10));
        }
    }
}
