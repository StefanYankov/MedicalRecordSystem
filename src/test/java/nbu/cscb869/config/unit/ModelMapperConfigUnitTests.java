package nbu.cscb869.config.unit;

import nbu.cscb869.config.ModelMapperConfig;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.models.enums.VisitStatus;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.services.data.dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelMapperConfigUnitTests {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private DoctorRepository doctorRepository;

    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        ModelMapperConfig modelMapperConfig = new ModelMapperConfig(specialtyRepository);
        modelMapper = modelMapperConfig.modelMapper();
    }

    @Nested
    @DisplayName("Doctor Mappings")
    class DoctorMappings {
        @Test
        void mapDoctorToDoctorViewDTO_ShouldMapCorrectly() {
            // ARRANGE
            Specialty cardio = new Specialty();
            cardio.setName("Cardiology");
            Doctor doctor = new Doctor();
            doctor.setId(1L);
            doctor.setName("Dr. Smith");
            doctor.setUniqueIdNumber("DOC123");
            doctor.setSpecialties(Set.of(cardio));
            doctor.setGeneralPractitioner(true);

            // ACT
            DoctorViewDTO dto = modelMapper.map(doctor, DoctorViewDTO.class);

            // ASSERT
            assertEquals(doctor.getId(), dto.getId());
            assertEquals(doctor.getName(), dto.getName());
            assertEquals(doctor.getUniqueIdNumber(), dto.getUniqueIdNumber());
            assertTrue(dto.isGeneralPractitioner());
            assertEquals(1, dto.getSpecialties().size());
            assertTrue(dto.getSpecialties().contains("Cardiology"));
        }

        @Test
        void mapDoctorToDoctorViewDTO_WithNullSpecialties_ShouldMapToEmptySet() {
            // ARRANGE
            Doctor doctor = new Doctor();
            doctor.setSpecialties(null);

            // ACT
            DoctorViewDTO dto = modelMapper.map(doctor, DoctorViewDTO.class);

            // ASSERT
            assertNotNull(dto.getSpecialties());
            assertTrue(dto.getSpecialties().isEmpty());
        }

        @Test
        void mapDoctorCreateDTOToDoctor_ShouldConvertSpecialtyNames() {
            // ARRANGE
            DoctorCreateDTO dto = new DoctorCreateDTO();
            dto.setSpecialties(Set.of("Cardiology"));
            Specialty cardio = new Specialty();
            cardio.setName("Cardiology");

            when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(cardio));

            // ACT
            Doctor doctor = modelMapper.map(dto, Doctor.class);

            // ASSERT
            assertNotNull(doctor.getSpecialties());
            assertEquals(1, doctor.getSpecialties().size());
            assertEquals("Cardiology", doctor.getSpecialties().iterator().next().getName());
        }

        @Test
        void mapDoctorCreateDTOToDoctor_WithNewSpecialty_ShouldCreateAndMap() {
            // ARRANGE
            DoctorCreateDTO dto = new DoctorCreateDTO();
            dto.setSpecialties(Set.of("Newcology"));

            // Mock repository returning empty for the new specialty
            when(specialtyRepository.findByName("Newcology")).thenReturn(Optional.empty());

            // Mock the save operation that would be triggered by the converter
            when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> {
                Specialty newSpecialty = invocation.getArgument(0);
                newSpecialty.setId(99L); // Simulate saving and getting an ID
                return newSpecialty;
            });

            // ACT
            Doctor doctor = modelMapper.map(dto, Doctor.class);

            // ASSERT
            assertNotNull(doctor.getSpecialties());
            assertEquals(1, doctor.getSpecialties().size());
            Specialty mappedSpecialty = doctor.getSpecialties().iterator().next();
            assertEquals("Newcology", mappedSpecialty.getName());
            assertEquals(99L, mappedSpecialty.getId());
        }

        @Test
        void mapDoctorUpdateDTOToDoctor_WithEmptySpecialtyNames_ShouldMapToEmptySet_EdgeCase() {
            // ARRANGE
            DoctorUpdateDTO dto = new DoctorUpdateDTO();
            dto.setSpecialties(new HashSet<>());

            Doctor doctor = new Doctor();
            doctor.setSpecialties(Set.of(new Specialty())); // Existing specialties

            // ACT
            modelMapper.map(dto, doctor);

            // ASSERT
            assertNotNull(doctor.getSpecialties());
            assertTrue(doctor.getSpecialties().isEmpty());
        }

        @Test
        void mapDoctorCreateDTOToDoctor_WithEmptySpecialtyNames_ShouldMapToEmptySet_EdgeCase() {
            // ARRANGE
            DoctorCreateDTO dto = new DoctorCreateDTO();
            dto.setSpecialties(new HashSet<>());

            // ACT
            Doctor doctor = modelMapper.map(dto, Doctor.class);

            // ASSERT
            assertNotNull(doctor.getSpecialties());
            assertTrue(doctor.getSpecialties().isEmpty());
        }

        @Test
        void mapDoctorUpdateDTOToDoctor_PartialUpdateName_ShouldUpdateOnlyName_EdgeCase() {
            // ARRANGE
            DoctorUpdateDTO dto = new DoctorUpdateDTO();
            dto.setName("Updated Name Only");

            Doctor existingDoctor = new Doctor();
            existingDoctor.setId(1L);
            existingDoctor.setName("Original Name");
            existingDoctor.setUniqueIdNumber("ORIGINAL_UID");
            existingDoctor.setGeneralPractitioner(true);

            // ACT
            modelMapper.map(dto, existingDoctor);

            // ASSERT
            assertEquals("Updated Name Only", existingDoctor.getName());
            assertEquals("ORIGINAL_UID", existingDoctor.getUniqueIdNumber()); // Should remain unchanged
            assertTrue(existingDoctor.isGeneralPractitioner()); // Should remain unchanged
        }

        @Test
        void mapDoctorUpdateDTOToDoctor_PartialUpdateGeneralPractitionerStatus_ShouldUpdateOnlyStatus_EdgeCase() {
            // ARRANGE
            DoctorUpdateDTO dto = new DoctorUpdateDTO();
            dto.setGeneralPractitioner(false);

            Doctor existingDoctor = new Doctor();
            existingDoctor.setId(1L);
            existingDoctor.setName("Original Name");
            existingDoctor.setUniqueIdNumber("ORIGINAL_UID");
            existingDoctor.setGeneralPractitioner(true);

            // ACT
            modelMapper.map(dto, existingDoctor);

            // ASSERT
            assertEquals("Original Name", existingDoctor.getName()); // Should remain unchanged
            assertEquals("ORIGINAL_UID", existingDoctor.getUniqueIdNumber()); // Should remain unchanged
            assertFalse(existingDoctor.isGeneralPractitioner()); // Should be updated
        }
    }

    @Nested
    @DisplayName("Patient Mappings")
    class PatientMappings {
        private Doctor mockGp;

        @BeforeEach
        void setupPatientMocks() {
            mockGp = new Doctor();
            mockGp.setId(10L);
            mockGp.setName("Dr. General");
            mockGp.setGeneralPractitioner(true);
        }

        @Test
        void mapPatientToPatientViewDTO_ShouldMapAllFieldsCorrectly_HappyPath() {
            // ARRANGE
            Patient patient = new Patient();
            patient.setId(1L);
            patient.setName("John Doe");
            patient.setEgn("1234567890");
            patient.setKeycloakId("kc-patient-1");
            patient.setLastInsurancePaymentDate(LocalDate.of(2023, 1, 15));
            patient.setGeneralPractitioner(mockGp);

            // ACT
            PatientViewDTO dto = modelMapper.map(patient, PatientViewDTO.class);

            // ASSERT
            assertEquals(patient.getId(), dto.getId());
            assertEquals(patient.getName(), dto.getName());
            assertEquals(patient.getEgn(), dto.getEgn());
            assertEquals(patient.getKeycloakId(), dto.getKeycloakId());
            assertEquals(patient.getLastInsurancePaymentDate(), dto.getLastInsurancePaymentDate());
            assertEquals(mockGp.getId(), dto.getGeneralPractitionerId());
            assertEquals(mockGp.getName(), dto.getGeneralPractitionerName());
        }

        @Test
        void mapPatientToPatientViewDTO_WithNullGP_ShouldMapCorrectly_EdgeCase() {
            // ARRANGE
            Patient patient = new Patient();
            patient.setId(1L);
            patient.setName("John Doe");
            patient.setEgn("1234567890");
            patient.setKeycloakId("kc-patient-1");
            patient.setLastInsurancePaymentDate(LocalDate.of(2023, 1, 15));
            patient.setGeneralPractitioner(null);

            // ACT
            PatientViewDTO dto = modelMapper.map(patient, PatientViewDTO.class);

            // ASSERT
            assertEquals(patient.getId(), dto.getId());
            assertEquals(patient.getName(), dto.getName());
            assertEquals(patient.getEgn(), dto.getEgn());
            assertEquals(patient.getKeycloakId(), dto.getKeycloakId());
            assertEquals(patient.getLastInsurancePaymentDate(), dto.getLastInsurancePaymentDate());
            assertNull(dto.getGeneralPractitionerId());
            assertNull(dto.getGeneralPractitionerName());
        }

        @Test
        void mapPatientCreateDTOToPatient_ShouldMapAllFieldsCorrectly_HappyPath() {
            // ARRANGE
            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setName("New Patient");
            dto.setEgn("0987654321");
            dto.setKeycloakId("kc-new-patient");
            dto.setGeneralPractitionerId(mockGp.getId());
            dto.setLastInsurancePaymentDate(LocalDate.of(2023, 2, 1));

            when(doctorRepository.findById(mockGp.getId())).thenReturn(Optional.of(mockGp));

            // ACT
            Patient patient = modelMapper.map(dto, Patient.class);

            // ASSERT
            assertNull(patient.getId()); // ID should be skipped
            assertEquals(dto.getName(), patient.getName());
            assertEquals(dto.getEgn(), patient.getEgn());
            assertEquals(dto.getKeycloakId(), patient.getKeycloakId());
            assertEquals(dto.getGeneralPractitionerId(), patient.getGeneralPractitioner().getId());
            assertEquals(dto.getLastInsurancePaymentDate(), patient.getLastInsurancePaymentDate());
        }

        @Test
        void mapPatientCreateDTOToPatient_WithNullGeneralPractitionerId_ShouldMapCorrectly_EdgeCase() {
            // ARRANGE
            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setName("New Patient");
            dto.setEgn("0987654321");
            dto.setKeycloakId("kc-new-patient");
            dto.setGeneralPractitionerId(null);
            dto.setLastInsurancePaymentDate(LocalDate.of(2023, 2, 1));

            // ACT
            Patient patient = modelMapper.map(dto, Patient.class);

            // ASSERT
            assertNull(patient.getGeneralPractitioner());
        }

        @Test
        void mapPatientCreateDTOToPatient_WithNonExistentGeneralPractitionerId_ShouldMapToNullGP_EdgeCase() {
            // ARRANGE
            PatientCreateDTO dto = new PatientCreateDTO();
            dto.setName("New Patient");
            dto.setEgn("0987654321");
            dto.setKeycloakId("kc-new-patient");
            dto.setGeneralPractitionerId(99L); // Non-existent GP ID
            dto.setLastInsurancePaymentDate(LocalDate.of(2023, 2, 1));

            when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

            // ACT
            Patient patient = modelMapper.map(dto, Patient.class);

            // ASSERT
            assertNull(patient.getGeneralPractitioner());
        }

        @Test
        void mapPatientUpdateDTOToPatient_ShouldMapAllFieldsCorrectly_HappyPath() {
            // ARRANGE
            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(1L); // Should be skipped
            dto.setName("Updated Patient");
            dto.setEgn("0987654322");
            dto.setKeycloakId("kc-updated-patient");
            dto.setGeneralPractitionerId(mockGp.getId());
            dto.setLastInsurancePaymentDate(LocalDate.of(2023, 3, 1));

            Patient existingPatient = new Patient(); // Destination object
            existingPatient.setId(100L); // Original ID, should remain unchanged

            when(doctorRepository.findById(mockGp.getId())).thenReturn(Optional.of(mockGp));

            // ACT
            modelMapper.map(dto, existingPatient);

            // ASSERT
            assertEquals(100L, existingPatient.getId()); // ID should NOT be mapped/changed
            assertEquals(dto.getName(), existingPatient.getName());
            assertEquals(dto.getEgn(), existingPatient.getEgn());
            assertEquals(dto.getKeycloakId(), existingPatient.getKeycloakId());
            assertEquals(dto.getGeneralPractitionerId(), existingPatient.getGeneralPractitioner().getId());
            assertEquals(dto.getLastInsurancePaymentDate(), existingPatient.getLastInsurancePaymentDate());
        }

        @Test
        void mapPatientUpdateDTOToPatient_WithNullGeneralPractitionerId_ShouldMapCorrectly_EdgeCase() {
            // ARRANGE
            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(1L);
            dto.setName("Updated Patient");
            dto.setEgn("0987654322");
            dto.setKeycloakId("kc-updated-patient");
            dto.setGeneralPractitionerId(null);
            dto.setLastInsurancePaymentDate(LocalDate.of(2023, 3, 1));

            Patient existingPatient = new Patient();
            existingPatient.setId(100L);
            existingPatient.setGeneralPractitioner(mockGp); // Existing GP

            // ACT
            modelMapper.map(dto, existingPatient);

            // ASSERT
            assertNull(existingPatient.getGeneralPractitioner());
        }

        @Test
        void mapPatientUpdateDTOToPatient_WithNonExistentGeneralPractitionerId_ShouldMapToNullGP_EdgeCase() {
            // ARRANGE
            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setId(1L);
            dto.setName("Updated Patient");
            dto.setEgn("0987654322");
            dto.setKeycloakId("kc-updated-patient");
            dto.setGeneralPractitionerId(99L); // Non-existent GP ID
            dto.setLastInsurancePaymentDate(LocalDate.of(2023, 3, 1));

            Patient existingPatient = new Patient();
            existingPatient.setId(100L);
            existingPatient.setGeneralPractitioner(mockGp); // Existing GP

            when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

            // ACT
            modelMapper.map(dto, existingPatient);

            // ASSERT
            assertNull(existingPatient.getGeneralPractitioner());
        }

        @Test
        void mapPatientUpdateDTOToPatient_PartialUpdateName_ShouldUpdateOnlyName_EdgeCase() {
            // ARRANGE
            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setName("Updated Name Only");

            Patient existingPatient = new Patient();
            existingPatient.setId(100L);
            existingPatient.setName("Original Name");
            existingPatient.setEgn("ORIGINAL_EGN");
            existingPatient.setKeycloakId("ORIGINAL_KC_ID");
            existingPatient.setLastInsurancePaymentDate(LocalDate.of(2022, 1, 1));
            existingPatient.setGeneralPractitioner(mockGp);

            // ACT
            modelMapper.map(dto, existingPatient);

            // ASSERT
            assertEquals("Updated Name Only", existingPatient.getName());
            assertEquals("ORIGINAL_EGN", existingPatient.getEgn());
            assertEquals("ORIGINAL_KC_ID", existingPatient.getKeycloakId());
            assertEquals(LocalDate.of(2022, 1, 1), existingPatient.getLastInsurancePaymentDate());
            assertEquals(mockGp.getId(), existingPatient.getGeneralPractitioner().getId());
        }

        @Test
        void mapPatientUpdateDTOToPatient_PartialUpdateInsuranceDate_ShouldUpdateOnlyDate_EdgeCase() {
            // ARRANGE
            LocalDate newInsuranceDate = LocalDate.of(2024, 5, 10);
            PatientUpdateDTO dto = new PatientUpdateDTO();
            dto.setLastInsurancePaymentDate(newInsuranceDate);

            Patient existingPatient = new Patient();
            existingPatient.setId(100L);
            existingPatient.setName("Original Name");
            existingPatient.setEgn("ORIGINAL_EGN");
            existingPatient.setKeycloakId("ORIGINAL_KC_ID");
            existingPatient.setLastInsurancePaymentDate(LocalDate.of(2022, 1, 1));
            existingPatient.setGeneralPractitioner(mockGp);

            // ACT
            modelMapper.map(dto, existingPatient);

            // ASSERT
            assertEquals("Original Name", existingPatient.getName()); // Should remain unchanged
            assertEquals("ORIGINAL_EGN", existingPatient.getEgn()); // Should remain unchanged
            assertEquals("ORIGINAL_KC_ID", existingPatient.getKeycloakId()); // Should remain unchanged
            assertEquals(newInsuranceDate, existingPatient.getLastInsurancePaymentDate()); // Should be updated
            assertEquals(mockGp.getId(), existingPatient.getGeneralPractitioner().getId()); // Should remain unchanged
        }
    }

    @Nested
    @DisplayName("Visit Mappings")
    class VisitMappings {
        @Test
        void mapVisitViewDTOToVisitUpdateDTO_ShouldMapCorrectly() {
            // ARRANGE
            PatientViewDTO patientViewDTO = new PatientViewDTO();
            patientViewDTO.setId(1L);
            DoctorViewDTO doctorViewDTO = new DoctorViewDTO();
            doctorViewDTO.setId(2L);
            DiagnosisViewDTO diagnosisViewDTO = new DiagnosisViewDTO();
            diagnosisViewDTO.setId(3L);

            VisitViewDTO source = new VisitViewDTO();
            source.setId(10L);
            source.setVisitDate(LocalDate.now());
            source.setVisitTime(LocalTime.of(10, 0));
            source.setNotes("Some notes");
            source.setStatus(VisitStatus.SCHEDULED);
            source.setPatient(patientViewDTO);
            source.setDoctor(doctorViewDTO);
            source.setDiagnosis(diagnosisViewDTO);

            // ACT
            VisitUpdateDTO destination = modelMapper.map(source, VisitUpdateDTO.class);

            // ASSERT
            assertEquals(source.getId(), destination.getId());
            assertEquals(source.getVisitDate(), destination.getVisitDate());
            assertEquals(source.getVisitTime(), destination.getVisitTime());
            assertEquals(source.getNotes(), destination.getNotes());
            assertEquals(source.getStatus(), destination.getStatus());
            assertEquals(patientViewDTO.getId(), destination.getPatientId());
            assertEquals(doctorViewDTO.getId(), destination.getDoctorId());
            assertEquals(diagnosisViewDTO.getId(), destination.getDiagnosisId());
        }
    }

    @Nested
    @DisplayName("Treatment and Medicine Mappings")
    class TreatmentAndMedicineMappings {
        @Test
        void mapTreatmentToTreatmentViewDTO_ShouldMapCorrectly() {
            // ARRANGE
            Visit visit = new Visit();
            visit.setId(100L);
            Treatment treatment = new Treatment();
            treatment.setId(1L);
            treatment.setVisit(visit);

            // ACT
            TreatmentViewDTO dto = modelMapper.map(treatment, TreatmentViewDTO.class);

            // ASSERT
            assertEquals(treatment.getId(), dto.getId());
            assertEquals(visit.getId(), dto.getVisitId());
        }

        @Test
        void mapMedicineToMedicineViewDTO_ShouldMapCorrectly() {
            // ARRANGE
            Treatment treatment = new Treatment();
            treatment.setId(200L);
            Medicine medicine = new Medicine();
            medicine.setId(5L);
            medicine.setTreatment(treatment);

            // ACT
            MedicineViewDTO dto = modelMapper.map(medicine, MedicineViewDTO.class);

            // ASSERT
            assertEquals(medicine.getId(), dto.getId());
            assertEquals(treatment.getId(), dto.getTreatmentId());
        }
    }

    @Nested
    @DisplayName("SickLeave Mappings")
    class SickLeaveMappings {
        @Test
        void mapSickLeaveToSickLeaveViewDTO_ShouldMapCorrectly() {
            // ARRANGE
            Visit visit = new Visit();
            visit.setId(300L);
            SickLeave sickLeave = new SickLeave();
            sickLeave.setId(15L);
            sickLeave.setStartDate(LocalDate.now());
            sickLeave.setDurationDays(5);
            sickLeave.setVisit(visit);

            // ACT
            SickLeaveViewDTO dto = modelMapper.map(sickLeave, SickLeaveViewDTO.class);

            // ASSERT
            assertEquals(sickLeave.getId(), dto.getId());
            assertEquals(sickLeave.getStartDate(), dto.getStartDate());
            assertEquals(sickLeave.getDurationDays(), dto.getDurationDays());
            assertEquals(visit.getId(), dto.getVisitId());
        }
    }

    @Nested
    @DisplayName("Doctor Create/Update Mappings")
    class DoctorCreateUpdateMappings {
        @Test
        void mapDoctorCreateDTOToDoctor_ShouldConvertSpecialtyNames() {
            // ARRANGE
            DoctorCreateDTO dto = new DoctorCreateDTO();
            dto.setSpecialties(Set.of("Cardiology"));
            Specialty cardio = new Specialty();
            cardio.setName("Cardiology");

            when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(cardio));

            // ACT
            Doctor doctor = modelMapper.map(dto, Doctor.class);

            // ASSERT
            assertNotNull(doctor.getSpecialties());
            assertEquals(1, doctor.getSpecialties().size());
            assertEquals("Cardiology", doctor.getSpecialties().iterator().next().getName());
        }

        @Test
        void mapDoctorCreateDTOToDoctor_WithNewSpecialty_ShouldCreateAndMap() {
            // ARRANGE
            DoctorCreateDTO dto = new DoctorCreateDTO();
            dto.setSpecialties(Set.of("Newcology"));

            // Mock repository returning empty for the new specialty
            when(specialtyRepository.findByName("Newcology")).thenReturn(Optional.empty());

            // Mock the save operation that would be triggered by the converter
            when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> {
                Specialty newSpecialty = invocation.getArgument(0);
                newSpecialty.setId(99L); // Simulate saving and getting an ID
                return newSpecialty;
            });

            // ACT
            Doctor doctor = modelMapper.map(dto, Doctor.class);

            // ASSERT
            assertNotNull(doctor.getSpecialties());
            assertEquals(1, doctor.getSpecialties().size());
            Specialty mappedSpecialty = doctor.getSpecialties().iterator().next();
            assertEquals("Newcology", mappedSpecialty.getName());
            assertEquals(99L, mappedSpecialty.getId());
        }

        @Test
        void mapDoctorUpdateDTOToDoctor_WithEmptySpecialtyNames_ShouldMapToEmptySet_EdgeCase() {
            // ARRANGE
            DoctorUpdateDTO dto = new DoctorUpdateDTO();
            dto.setSpecialties(new HashSet<>());

            Doctor doctor = new Doctor();
            doctor.setSpecialties(Set.of(new Specialty())); // Existing specialties

            // ACT
            modelMapper.map(dto, doctor);

            // ASSERT
            assertNotNull(doctor.getSpecialties());
            assertTrue(doctor.getSpecialties().isEmpty());
        }

        @Test
        void mapDoctorCreateDTOToDoctor_WithEmptySpecialtyNames_ShouldMapToEmptySet_EdgeCase() {
            // ARRANGE
            DoctorCreateDTO dto = new DoctorCreateDTO();
            dto.setSpecialties(new HashSet<>());

            // ACT
            Doctor doctor = modelMapper.map(dto, Doctor.class);

            // ASSERT
            assertNotNull(doctor.getSpecialties());
            assertTrue(doctor.getSpecialties().isEmpty());
        }

        @Test
        void mapDoctorUpdateDTOToDoctor_PartialUpdateName_ShouldUpdateOnlyName_EdgeCase() {
            // ARRANGE
            DoctorUpdateDTO dto = new DoctorUpdateDTO();
            dto.setName("Updated Name Only");

            Doctor existingDoctor = new Doctor();
            existingDoctor.setId(1L);
            existingDoctor.setName("Original Name");
            existingDoctor.setUniqueIdNumber("ORIGINAL_UID");
            existingDoctor.setGeneralPractitioner(true);

            // ACT
            modelMapper.map(dto, existingDoctor);

            // ASSERT
            assertEquals("Updated Name Only", existingDoctor.getName());
            assertEquals("ORIGINAL_UID", existingDoctor.getUniqueIdNumber());
            assertTrue(existingDoctor.isGeneralPractitioner());
        }

        @Test
        void mapDoctorUpdateDTOToDoctor_PartialUpdateGeneralPractitionerStatus_ShouldUpdateOnlyStatus_EdgeCase() {
            // ARRANGE
            DoctorUpdateDTO dto = new DoctorUpdateDTO();
            dto.setGeneralPractitioner(false);

            Doctor existingDoctor = new Doctor();
            existingDoctor.setId(1L);
            existingDoctor.setName("Original Name");
            existingDoctor.setUniqueIdNumber("ORIGINAL_UID");
            existingDoctor.setGeneralPractitioner(true);

            // ACT
            modelMapper.map(dto, existingDoctor);

            // ASSERT
            assertEquals("Original Name", existingDoctor.getName()); // Should remain unchanged
            assertEquals("ORIGINAL_UID", existingDoctor.getUniqueIdNumber()); // Should remain unchanged
            assertFalse(existingDoctor.isGeneralPractitioner()); // Should be updated
        }
    }
}
