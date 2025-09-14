package nbu.cscb869.config.unit;

import nbu.cscb869.config.ModelMapperConfig;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.utils.TestDataUtils;
import nbu.cscb869.services.data.dtos.*;
import nbu.cscb869.services.data.dtos.identity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ModelMapperUnitTests {
    private ModelMapper modelMapper;

    @BeforeEach
    void SetUp() {
        ModelMapperConfig config = new ModelMapperConfig();
        modelMapper = config.modelMapper();
    }

    //region Diagnosis Mappings
    @Test
    void MapDiagnosisCreateDTOToDiagnosis_ShouldMapCorrectly_WhenValidInput() {
        DiagnosisCreateDTO dto = DiagnosisCreateDTO.builder()
                .name("Flu")
                .description("Viral infection")
                .build();

        Diagnosis diagnosis = modelMapper.map(dto, Diagnosis.class);

        assertNotNull(diagnosis);
        assertEquals("Flu", diagnosis.getName());
        assertEquals("Viral infection", diagnosis.getDescription());
    }

    @Test
    void MapDiagnosisUpdateDTOToDiagnosis_ShouldMapCorrectly_WhenValidInput() {
        DiagnosisUpdateDTO dto = DiagnosisUpdateDTO.builder()
                .id(1L)
                .name("Flu")
                .description("Viral infection")
                .build();

        Diagnosis diagnosis = modelMapper.map(dto, Diagnosis.class);

        assertNotNull(diagnosis);
        assertEquals(1L, diagnosis.getId());
        assertEquals("Flu", diagnosis.getName());
        assertEquals("Viral infection", diagnosis.getDescription());
    }

    @Test
    void MapDiagnosisToDiagnosisViewDTO_ShouldMapCorrectly_WhenValidInput() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        diagnosis.setDescription("Viral infection");

        DiagnosisViewDTO viewDTO = modelMapper.map(diagnosis, DiagnosisViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId()); // Skipped due to mapper.skip
        assertEquals("Flu", viewDTO.getName());
        assertEquals("Viral infection", viewDTO.getDescription());
    }

    @Test
    void MapDiagnosisCreateDTOToDiagnosis_ShouldThrowException_WhenNullInput() {
        assertThrows(IllegalArgumentException.class, () -> modelMapper.map(null, Diagnosis.class));
    }

    @Test
    void MapDiagnosisToDiagnosisViewDTO_ShouldHandleNullFields_WhenDescriptionIsNull() {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        diagnosis.setDescription(null);

        DiagnosisViewDTO viewDTO = modelMapper.map(diagnosis, DiagnosisViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId());
        assertEquals("Flu", viewDTO.getName());
        assertNull(viewDTO.getDescription());
    }
    //endregion

    //region Patient Mappings
    @Test
    void MapPatientCreateDTOToPatient_ShouldMapCorrectly_WhenValidInput() {
        PatientCreateDTO dto = PatientCreateDTO.builder()
                .egn(TestDataUtils.generateValidEgn())
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(1L)
                .build();

        Patient patient = modelMapper.map(dto, Patient.class);

        assertNotNull(patient);
        assertEquals(dto.getEgn(), patient.getEgn());
        assertEquals(dto.getLastInsurancePaymentDate(), patient.getLastInsurancePaymentDate());
        assertNotNull(patient.getGeneralPractitioner());
        assertEquals(1L, patient.getGeneralPractitioner().getId());
    }

    @Test
    void MapPatientUpdateDTOToPatient_ShouldMapCorrectly_WhenValidInput() {
        PatientUpdateDTO dto = PatientUpdateDTO.builder()
                .id(1L)
                .egn(TestDataUtils.generateValidEgn())
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(2L)
                .build();

        Patient patient = modelMapper.map(dto, Patient.class);

        assertNotNull(patient);
        assertEquals(1L, patient.getId());
        assertEquals(dto.getEgn(), patient.getEgn());
        assertEquals(dto.getLastInsurancePaymentDate(), patient.getLastInsurancePaymentDate());
        assertNotNull(patient.getGeneralPractitioner());
        assertEquals(2L, patient.getGeneralPractitioner().getId());
    }

    @Test
    void MapPatientToPatientViewDTO_ShouldMapCorrectly_WhenValidInput() {
        Doctor doctor = new Doctor();
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);

        Patient patient = new Patient();
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(doctor);

        PatientViewDTO viewDTO = modelMapper.map(patient, PatientViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId()); // Skipped
        assertEquals(patient.getEgn(), viewDTO.getEgn());
        assertEquals(patient.getLastInsurancePaymentDate(), viewDTO.getLastInsurancePaymentDate());
        assertNotNull(viewDTO.getGeneralPractitioner());
        assertEquals(doctor.getUniqueIdNumber(), viewDTO.getGeneralPractitioner().getUniqueIdNumber());
    }

    @Test
    void MapPatientCreateDTOToPatient_ShouldHandleNullGeneralPractitioner_WhenIdIsNull() {
        PatientCreateDTO dto = PatientCreateDTO.builder()
                .egn(TestDataUtils.generateValidEgn())
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(null)
                .build();

        Patient patient = modelMapper.map(dto, Patient.class);

        assertNotNull(patient);
        assertEquals(dto.getEgn(), patient.getEgn());
        assertEquals(dto.getLastInsurancePaymentDate(), patient.getLastInsurancePaymentDate());
        assertNull(patient.getGeneralPractitioner());
    }

    @Test
    void MapPatientToPatientViewDTO_ShouldHandleNullFields_WhenDoctorIsNull() {
        Patient patient = new Patient();
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setLastInsurancePaymentDate(null);
        patient.setGeneralPractitioner(null);

        PatientViewDTO viewDTO = modelMapper.map(patient, PatientViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId());
        assertEquals(patient.getEgn(), viewDTO.getEgn());
        assertNull(viewDTO.getLastInsurancePaymentDate());
        assertNull(viewDTO.getGeneralPractitioner());
    }
    //endregion

    //region SickLeave Mappings
    @Test
    void MapSickLeaveCreateDTOToSickLeave_ShouldMapCorrectly_WhenValidInput() {
        SickLeaveCreateDTO dto = SickLeaveCreateDTO.builder()
                .startDate(LocalDate.now())
                .durationDays(5)
                .visitId(1L)
                .build();

        SickLeave sickLeave = modelMapper.map(dto, SickLeave.class);

        assertNotNull(sickLeave);
        assertEquals(dto.getStartDate(), sickLeave.getStartDate());
        assertEquals(dto.getDurationDays(), sickLeave.getDurationDays());
        assertNotNull(sickLeave.getVisit());
        assertEquals(1L, sickLeave.getVisit().getId());
    }

    @Test
    void MapSickLeaveUpdateDTOToSickLeave_ShouldMapCorrectly_WhenValidInput() {
        SickLeaveUpdateDTO dto = SickLeaveUpdateDTO.builder()
                .id(1L)
                .startDate(LocalDate.now())
                .durationDays(5)
                .visitId(2L)
                .build();

        SickLeave sickLeave = modelMapper.map(dto, SickLeave.class);

        assertNotNull(sickLeave);
        assertEquals(1L, sickLeave.getId());
        assertEquals(dto.getStartDate(), sickLeave.getStartDate());
        assertEquals(dto.getDurationDays(), sickLeave.getDurationDays());
        assertNotNull(sickLeave.getVisit());
        assertEquals(2L, sickLeave.getVisit().getId());
    }

    @Test
    void MapSickLeaveToSickLeaveViewDTO_ShouldMapCorrectly_WhenValidInput() {
        Visit visit = new Visit();
        visit.setId(1L);

        SickLeave sickLeave = new SickLeave();
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave.setVisit(visit);

        SickLeaveViewDTO viewDTO = modelMapper.map(sickLeave, SickLeaveViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId()); // Skipped
        assertEquals(sickLeave.getStartDate(), viewDTO.getStartDate());
        assertEquals(sickLeave.getDurationDays(), viewDTO.getDurationDays());
        assertEquals(1L, viewDTO.getVisitId());
    }

    @Test
    void MapSickLeaveCreateDTOToSickLeave_ShouldHandleNullVisit_WhenVisitIdIsNull() {
        SickLeaveCreateDTO dto = SickLeaveCreateDTO.builder()
                .startDate(LocalDate.now())
                .durationDays(5)
                .visitId(null)
                .build();

        SickLeave sickLeave = modelMapper.map(dto, SickLeave.class);

        assertNotNull(sickLeave);
        assertEquals(dto.getStartDate(), sickLeave.getStartDate());
        assertEquals(dto.getDurationDays(), sickLeave.getDurationDays());
        assertNull(sickLeave.getVisit());
    }

    @Test
    void MapSickLeaveToSickLeaveViewDTO_ShouldHandleNullVisit_WhenVisitIsNull() {
        SickLeave sickLeave = new SickLeave();
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave.setVisit(null);

        SickLeaveViewDTO viewDTO = modelMapper.map(sickLeave, SickLeaveViewDTO.class);

        assertNotNull(sickLeave);
        assertNull(viewDTO.getId());
        assertEquals(sickLeave.getStartDate(), viewDTO.getStartDate());
        assertEquals(sickLeave.getDurationDays(), viewDTO.getDurationDays());
        assertNull(viewDTO.getVisitId());
    }
    //endregion

    //region Doctor Mappings
    @Test
    void MapDoctorCreateDTOToDoctor_ShouldMapCorrectly_WhenValidInput() {
        DoctorCreateDTO dto = DoctorCreateDTO.builder()
                .uniqueIdNumber(TestDataUtils.generateUniqueIdNumber())
                .isGeneralPractitioner(true)
                .imageUrl("https://example.com/image.jpg")
                .specialtyIds(new HashSet<>(Arrays.asList(1L, 2L)))
                .build();

        Doctor doctor = modelMapper.map(dto, Doctor.class);

        assertNotNull(doctor);
        assertEquals(dto.getUniqueIdNumber(), doctor.getUniqueIdNumber());
        assertTrue(doctor.isGeneralPractitioner());
        assertEquals(dto.getImageUrl(), doctor.getImageUrl());
        assertTrue(doctor.getSpecialties().isEmpty()); // Skipped
        assertTrue(doctor.getPatients().isEmpty()); // Skipped
        assertTrue(doctor.getVisits().isEmpty()); // Skipped
    }

    @Test
    void MapDoctorUpdateDTOToDoctor_ShouldMapCorrectly_WhenValidInput() {
        DoctorUpdateDTO dto = DoctorUpdateDTO.builder()
                .id(1L)
                .uniqueIdNumber(TestDataUtils.generateUniqueIdNumber())
                .isGeneralPractitioner(true)
                .imageUrl("https://example.com/image.jpg")
                .specialtyIds(new HashSet<>(Arrays.asList(1L, 2L)))
                .build();

        Doctor doctor = modelMapper.map(dto, Doctor.class);

        assertNotNull(doctor);
        assertEquals(1L, doctor.getId());
        assertEquals(dto.getUniqueIdNumber(), doctor.getUniqueIdNumber());
        assertTrue(doctor.isGeneralPractitioner());
        assertEquals(dto.getImageUrl(), doctor.getImageUrl());
        assertTrue(doctor.getSpecialties().isEmpty()); // Skipped
        assertTrue(doctor.getPatients().isEmpty()); // Skipped
        assertTrue(doctor.getVisits().isEmpty()); // Skipped
    }

    @Test
    void MapDoctorToDoctorViewDTO_ShouldMapCorrectly_WhenValidInput() {
        Specialty specialty = new Specialty();
        specialty.setName("Cardiology");

        Doctor doctor = new Doctor();
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);
        doctor.setImageUrl("https://example.com/image.jpg");
        doctor.setSpecialties(new HashSet<>(Collections.singletonList(specialty)));

        DoctorViewDTO viewDTO = modelMapper.map(doctor, DoctorViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId());
        assertEquals(doctor.getUniqueIdNumber(), viewDTO.getUniqueIdNumber());
        assertTrue(viewDTO.isGeneralPractitioner());
        assertEquals(doctor.getImageUrl(), viewDTO.getImageUrl());
        assertEquals(1, viewDTO.getSpecialties().size());
        assertEquals("Cardiology", viewDTO.getSpecialties().iterator().next().getName());
    }

    @Test
    void MapDoctorCreateDTOToDoctor_ShouldHandleNullFields_WhenOptionalFieldsAreNull() {
        DoctorCreateDTO dto = DoctorCreateDTO.builder()
                .uniqueIdNumber(TestDataUtils.generateUniqueIdNumber())
                .isGeneralPractitioner(false)
                .imageUrl(null)
                .specialtyIds(null)
                .build();

        Doctor doctor = modelMapper.map(dto, Doctor.class);

        assertNotNull(doctor);
        assertEquals(dto.getUniqueIdNumber(), doctor.getUniqueIdNumber());
        assertFalse(doctor.isGeneralPractitioner());
        assertNull(doctor.getImageUrl());
        assertTrue(doctor.getSpecialties().isEmpty());
    }

    @Test
    void MapDoctorToDoctorViewDTO_ShouldHandleEmptySpecialties_WhenSpecialtiesAreEmpty() {
        Doctor doctor = new Doctor();
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);
        doctor.setSpecialties(new HashSet<>());

        DoctorViewDTO viewDTO = modelMapper.map(doctor, DoctorViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId());
        assertEquals(doctor.getUniqueIdNumber(), viewDTO.getUniqueIdNumber());
        assertTrue(viewDTO.isGeneralPractitioner());
        assertTrue(viewDTO.getSpecialties().isEmpty());
    }
    //endregion

    //region Visit Mappings
    @Test
    void MapVisitCreateDTOToVisit_ShouldMapCorrectly_WhenValidInput() {
        VisitCreateDTO dto = VisitCreateDTO.builder()
                .visitDate(LocalDate.now())
                .visitTime(LocalTime.of(9, 0))
                .sickLeaveIssued(true)
                .patientId(1L)
                .doctorId(2L)
                .diagnosisId(3L)
                .build();

        Visit visit = modelMapper.map(dto, Visit.class);

        assertNotNull(visit);
        assertEquals(dto.getVisitDate(), visit.getVisitDate());
        assertEquals(dto.getVisitTime(), visit.getVisitTime());
        assertTrue(visit.isSickLeaveIssued());
        assertNotNull(visit.getPatient());
        assertEquals(1L, visit.getPatient().getId());
        assertNotNull(visit.getDoctor());
        assertEquals(2L, visit.getDoctor().getId());
        assertNotNull(visit.getDiagnosis());
        assertEquals(3L, visit.getDiagnosis().getId());
        assertNull(visit.getSickLeave()); // Skipped
        assertNull(visit.getTreatment()); // Skipped
    }

    @Test
    void MapVisitUpdateDTOToVisit_ShouldMapCorrectly_WhenValidInput() {
        VisitUpdateDTO dto = VisitUpdateDTO.builder()
                .id(1L)
                .visitDate(LocalDate.now())
                .visitTime(LocalTime.of(9, 0))
                .sickLeaveIssued(true)
                .patientId(1L)
                .doctorId(2L)
                .diagnosisId(3L)
                .build();

        Visit visit = modelMapper.map(dto, Visit.class);

        assertNotNull(visit);
        assertEquals(1L, visit.getId());
        assertEquals(dto.getVisitDate(), visit.getVisitDate());
        assertEquals(dto.getVisitTime(), visit.getVisitTime());
        assertTrue(visit.isSickLeaveIssued());
        assertNotNull(visit.getPatient());
        assertEquals(1L, visit.getPatient().getId());
        assertNotNull(visit.getDoctor());
        assertEquals(2L, visit.getDoctor().getId());
        assertNotNull(visit.getDiagnosis());
        assertEquals(3L, visit.getDiagnosis().getId());
        assertNull(visit.getSickLeave()); // Skipped
        assertNull(visit.getTreatment()); // Skipped
    }

    @Test
    void MapVisitToVisitViewDTO_ShouldMapCorrectly_WhenValidInput() {
        Patient patient = new Patient();
        patient.setEgn(TestDataUtils.generateValidEgn());
        Doctor doctor = new Doctor();
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setName("Flu");
        SickLeave sickLeave = new SickLeave();
        sickLeave.setStartDate(LocalDate.now());
        Treatment treatment = new Treatment();
        treatment.setDescription("Rest");

        Visit visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(LocalTime.of(9, 0));
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setSickLeave(sickLeave);
        visit.setTreatment(treatment);

        VisitViewDTO viewDTO = modelMapper.map(visit, VisitViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId()); // Skipped
        assertEquals(visit.getVisitDate(), viewDTO.getVisitDate());
        assertEquals(visit.getVisitTime(), viewDTO.getVisitTime());
        assertTrue(viewDTO.isSickLeaveIssued());
        assertNotNull(viewDTO.getPatient());
        assertEquals(patient.getEgn(), viewDTO.getPatient().getEgn());
        assertNotNull(viewDTO.getDoctor());
        assertEquals(doctor.getUniqueIdNumber(), viewDTO.getDoctor().getUniqueIdNumber());
        assertNotNull(viewDTO.getDiagnosis());
        assertEquals(diagnosis.getName(), viewDTO.getDiagnosis().getName());
        assertNotNull(viewDTO.getSickLeave());
        assertEquals(sickLeave.getStartDate(), viewDTO.getSickLeave().getStartDate());
        assertNotNull(viewDTO.getTreatment());
        assertEquals(treatment.getDescription(), viewDTO.getTreatment().getDescription());
    }

    @Test
    void MapVisitCreateDTOToVisit_ShouldHandleNullIds_WhenIdsAreNull() {
        VisitCreateDTO dto = VisitCreateDTO.builder()
                .visitDate(LocalDate.now())
                .visitTime(LocalTime.of(9, 0))
                .sickLeaveIssued(false)
                .patientId(null)
                .doctorId(null)
                .diagnosisId(null)
                .build();

        Visit visit = modelMapper.map(dto, Visit.class);

        assertNotNull(visit);
        assertEquals(dto.getVisitDate(), visit.getVisitDate());
        assertEquals(dto.getVisitTime(), visit.getVisitTime());
        assertFalse(visit.isSickLeaveIssued());
        assertNull(visit.getPatient());
        assertNull(visit.getDoctor());
        assertNull(visit.getDiagnosis());
    }

    @Test
    void MapVisitToVisitViewDTO_ShouldHandleNullRelations_WhenRelationsAreNull() {
        Visit visit = new Visit();
        visit.setVisitDate(LocalDate.now());
        visit.setVisitTime(LocalTime.of(9, 0));
        visit.setPatient(null);
        visit.setDoctor(null);
        visit.setDiagnosis(null);
        visit.setSickLeave(null);
        visit.setTreatment(null);

        VisitViewDTO viewDTO = modelMapper.map(visit, VisitViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId());
        assertEquals(visit.getVisitDate(), viewDTO.getVisitDate());
        assertEquals(visit.getVisitTime(), viewDTO.getVisitTime());
        assertFalse(viewDTO.isSickLeaveIssued());
        assertNull(viewDTO.getPatient());
        assertNull(viewDTO.getDoctor());
        assertNull(viewDTO.getDiagnosis());
        assertNull(viewDTO.getSickLeave());
        assertNull(viewDTO.getTreatment());
    }
    //endregion

    //region Treatment Mappings
    @Test
    void MapTreatmentCreateDTOToTreatment_ShouldMapCorrectly_WhenValidInput() {
        TreatmentCreateDTO dto = TreatmentCreateDTO.builder()
                .description("Rest")
                .visitId(1L)
                .medicineIds(Arrays.asList(1L, 2L))
                .build();

        Treatment treatment = modelMapper.map(dto, Treatment.class);

        assertNotNull(treatment);
        assertEquals(dto.getDescription(), treatment.getDescription());
        assertNotNull(treatment.getVisit());
        assertEquals(1L, treatment.getVisit().getId());
        assertTrue(treatment.getMedicines().isEmpty());
    }

    @Test
    void MapTreatmentUpdateDTOToTreatment_ShouldMapCorrectly_WhenValidInput() {
        TreatmentUpdateDTO dto = TreatmentUpdateDTO.builder()
                .id(1L)
                .description("Rest")
                .visitId(2L)
                .medicineIds(Arrays.asList(1L, 2L))
                .build();

        Treatment treatment = modelMapper.map(dto, Treatment.class);

        assertNotNull(treatment);
        assertEquals(1L, treatment.getId());
        assertEquals(dto.getDescription(), treatment.getDescription());
        assertNotNull(treatment.getVisit());
        assertEquals(2L, treatment.getVisit().getId());
        assertTrue(treatment.getMedicines().isEmpty()); // Skipped
    }

    @Test
    void MapTreatmentToTreatmentViewDTO_ShouldMapCorrectly_WhenValidInput() {
        Visit visit = new Visit();
        visit.setId(1L);
        Medicine medicine = new Medicine();
        medicine.setName("Aspirin");

        Treatment treatment = new Treatment();
        treatment.setDescription("Rest");
        treatment.setVisit(visit);
        treatment.setMedicines(Collections.singletonList(medicine));

        TreatmentViewDTO viewDTO = modelMapper.map(treatment, TreatmentViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId()); // Skipped
        assertEquals(treatment.getDescription(), viewDTO.getDescription());
        assertEquals(1L, viewDTO.getVisitId());
        assertEquals(1, viewDTO.getMedicines().size());
        assertEquals("Aspirin", viewDTO.getMedicines().getFirst().getName());
    }

    @Test
    void MapTreatmentCreateDTOToTreatment_ShouldHandleNullFields_WhenOptionalFieldsAreNull() {
        TreatmentCreateDTO dto = TreatmentCreateDTO.builder()
                .description(null)
                .visitId(1L)
                .medicineIds(null)
                .build();

        Treatment treatment = modelMapper.map(dto, Treatment.class);

        assertNotNull(treatment);
        assertNull(treatment.getDescription());
        assertNotNull(treatment.getVisit());
        assertEquals(1L, treatment.getVisit().getId());
        assertTrue(treatment.getMedicines().isEmpty());
    }

    @Test
    void MapTreatmentToTreatmentViewDTO_ShouldHandleEmptyMedicines_WhenMedicinesAreEmpty() {
        Visit visit = new Visit();
        visit.setId(1L);

        Treatment treatment = new Treatment();
        treatment.setDescription("Rest");
        treatment.setVisit(visit);
        treatment.setMedicines(new ArrayList<>());

        TreatmentViewDTO viewDTO = modelMapper.map(treatment, TreatmentViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId());
        assertEquals(treatment.getDescription(), viewDTO.getDescription());
        assertEquals(1L, viewDTO.getVisitId());
        assertTrue(viewDTO.getMedicines().isEmpty());
    }
    //endregion

    //region Medicine Mappings
    @Test
    void MapMedicineCreateDTOToMedicine_ShouldMapCorrectly_WhenValidInput() {
        MedicineCreateDTO dto = MedicineCreateDTO.builder()
                .name("Aspirin")
                .dosage("500mg")
                .frequency("Once daily")
                .treatmentId(1L)
                .build();

        Medicine medicine = modelMapper.map(dto, Medicine.class);

        assertNotNull(medicine);
        assertEquals(dto.getName(), medicine.getName());
        assertEquals(dto.getDosage(), medicine.getDosage());
        assertEquals(dto.getFrequency(), medicine.getFrequency());
        assertNotNull(medicine.getTreatment());
        assertEquals(1L, medicine.getTreatment().getId());
    }

    @Test
    void MapMedicineUpdateDTOToMedicine_ShouldMapCorrectly_WhenValidInput() {
        MedicineUpdateDTO dto = MedicineUpdateDTO.builder()
                .id(1L)
                .name("Aspirin")
                .dosage("500mg")
                .frequency("Once daily")
                .treatmentId(2L)
                .build();

        Medicine medicine = modelMapper.map(dto, Medicine.class);

        assertNotNull(medicine);
        assertEquals(1L, medicine.getId());
        assertEquals(dto.getName(), medicine.getName());
        assertEquals(dto.getDosage(), medicine.getDosage());
        assertEquals(dto.getFrequency(), medicine.getFrequency());
        assertNotNull(medicine.getTreatment());
        assertEquals(2L, medicine.getTreatment().getId());
    }

    @Test
    void MapMedicineToMedicineViewDTO_ShouldMapCorrectly_WhenValidInput() {
        Treatment treatment = new Treatment();
        treatment.setId(1L);

        Medicine medicine = new Medicine();
        medicine.setName("Aspirin");
        medicine.setDosage("500mg");
        medicine.setFrequency("Once daily");
        medicine.setTreatment(treatment);

        MedicineViewDTO viewDTO = modelMapper.map(medicine, MedicineViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId()); // Skipped
        assertEquals(medicine.getName(), viewDTO.getName());
        assertEquals(medicine.getDosage(), viewDTO.getDosage());
        assertEquals(medicine.getFrequency(), viewDTO.getFrequency());
        assertEquals(1L, viewDTO.getTreatmentId());
    }

    @Test
    void MapMedicineCreateDTOToMedicine_ShouldHandleNullTreatment_WhenTreatmentIdIsNull() {
        MedicineCreateDTO dto = MedicineCreateDTO.builder()
                .name("Aspirin")
                .dosage("500mg")
                .frequency("Once daily")
                .treatmentId(null)
                .build();

        Medicine medicine = modelMapper.map(dto, Medicine.class);

        assertNotNull(medicine);
        assertEquals(dto.getName(), medicine.getName());
        assertEquals(dto.getDosage(), medicine.getDosage());
        assertEquals(dto.getFrequency(), medicine.getFrequency());
        assertNull(medicine.getTreatment());
    }

    @Test
    void MapMedicineToMedicineViewDTO_ShouldHandleNullTreatment_WhenTreatmentIsNull() {
        Medicine medicine = new Medicine();
        medicine.setName("Aspirin");
        medicine.setDosage("500mg");
        medicine.setFrequency("Once daily");
        medicine.setTreatment(null);

        MedicineViewDTO viewDTO = modelMapper.map(medicine, MedicineViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId());
        assertEquals(medicine.getName(), viewDTO.getName());
        assertEquals(medicine.getDosage(), viewDTO.getDosage());
        assertEquals(medicine.getFrequency(), viewDTO.getFrequency());
        assertNull(viewDTO.getTreatmentId());
    }
    //endregion

    //region Specialty Mappings
    @Test
    void MapSpecialtyCreateDTOToSpecialty_ShouldMapCorrectly_WhenValidInput() {
        SpecialtyCreateDTO dto = SpecialtyCreateDTO.builder()
                .name("Cardiology")
                .description("Heart diseases")
                .build();

        Specialty specialty = modelMapper.map(dto, Specialty.class);

        assertNotNull(specialty);
        assertEquals(dto.getName(), specialty.getName());
        assertEquals(dto.getDescription(), specialty.getDescription());
    }

    @Test
    void MapSpecialtyUpdateDTOToSpecialty_ShouldMapCorrectly_WhenValidInput() {
        SpecialtyUpdateDTO dto = SpecialtyUpdateDTO.builder()
                .id(1L)
                .name("Cardiology")
                .description("Heart diseases")
                .build();

        Specialty specialty = modelMapper.map(dto, Specialty.class);

        assertNotNull(specialty);
        assertEquals(1L, specialty.getId());
        assertEquals(dto.getName(), specialty.getName());
        assertEquals(dto.getDescription(), specialty.getDescription());
    }

    @Test
    void MapSpecialtyToSpecialtyViewDTO_ShouldMapCorrectly_WhenValidInput() {
        Specialty specialty = new Specialty();
        specialty.setName("Cardiology");
        specialty.setDescription("Heart diseases");

        SpecialtyViewDTO viewDTO = modelMapper.map(specialty, SpecialtyViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId()); // Skipped
        assertEquals(specialty.getName(), viewDTO.getName());
        assertEquals(specialty.getDescription(), viewDTO.getDescription());
    }

    @Test
    void MapSpecialtyCreateDTOToSpecialty_ShouldHandleNullDescription_WhenDescriptionIsNull() {
        SpecialtyCreateDTO dto = SpecialtyCreateDTO.builder()
                .name("Cardiology")
                .description(null)
                .build();

        Specialty specialty = modelMapper.map(dto, Specialty.class);

        assertNotNull(specialty);
        assertEquals(dto.getName(), specialty.getName());
        assertNull(specialty.getDescription());
    }

    @Test
    void MapSpecialtyToSpecialtyViewDTO_ShouldHandleNullDescription_WhenDescriptionIsNull() {
        Specialty specialty = new Specialty();
        specialty.setName("Cardiology");
        specialty.setDescription(null);

        SpecialtyViewDTO viewDTO = modelMapper.map(specialty, SpecialtyViewDTO.class);

        assertNotNull(viewDTO);
        assertNull(viewDTO.getId());
        assertEquals(specialty.getName(), viewDTO.getName());
        assertNull(viewDTO.getDescription());
    }
    //endregion
}