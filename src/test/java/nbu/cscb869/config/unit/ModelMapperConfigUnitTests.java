package nbu.cscb869.config.unit;

import nbu.cscb869.config.ModelMapperConfig;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.services.data.dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelMapperConfigUnitTests {

    @Mock
    private SpecialtyRepository specialtyRepository;

    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        ModelMapperConfig modelMapperConfig = new ModelMapperConfig(specialtyRepository);
        modelMapper = modelMapperConfig.modelMapper();
    }

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
    void mapPatientToPatientViewDTO_ShouldMapCorrectly() {
        // ARRANGE
        Doctor gp = new Doctor();
        gp.setId(10L);
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setGeneralPractitioner(gp);

        // ACT
        PatientViewDTO dto = modelMapper.map(patient, PatientViewDTO.class);

        // ASSERT
        assertEquals(patient.getId(), dto.getId());
        assertEquals(gp.getId(), dto.getGeneralPractitionerId());
    }

    @Test
    void mapPatientToPatientViewDTO_WithNullGP_ShouldMapToNull() {
        // ARRANGE
        Patient patient = new Patient();
        patient.setGeneralPractitioner(null);

        // ACT
        PatientViewDTO dto = modelMapper.map(patient, PatientViewDTO.class);

        // ASSERT
        assertNull(dto.getGeneralPractitionerId());
    }

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
}
