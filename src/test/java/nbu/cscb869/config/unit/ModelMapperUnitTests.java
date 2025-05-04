package nbu.cscb869.config.unit;

import nbu.cscb869.config.ModelMapperConfig;
import nbu.cscb869.data.models.*;
import nbu.cscb869.services.data.dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ModelMapperUnitTests {

    private ModelMapper modelMapper;

    private Doctor doctor;
    private Specialty specialty;
    private Patient patient;
    private Diagnosis diagnosis;
    private Visit visit;
    private SickLeave sickLeave;
    private Treatment treatment;
    private Medicine medicine;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapperConfig().modelMapper();

        specialty = new Specialty();
        specialty.setId(1L);
        specialty.setName("Cardiology");
        specialty.setDescription("Heart-related specialties");

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Smith");
        doctor.setUniqueIdNumber("DOC123");
        doctor.setGeneralPractitioner(true);
        doctor.setImageUrl("http://example.com/image.jpg");
        doctor.setSpecialties(Set.of(specialty));
        doctor.setPatients(Collections.emptyList());
        doctor.setVisits(Collections.emptyList());

        patient = new Patient();
        patient.setId(1L);
        patient.setName("John Doe");
        patient.setEgn("1234567890");
        patient.setLastInsurancePaymentDate(LocalDate.now());
        patient.setGeneralPractitioner(doctor);
        patient.setVisits(Collections.emptyList());

        diagnosis = new Diagnosis();
        diagnosis.setId(1L);
        diagnosis.setName("Hypertension");
        diagnosis.setDescription("High blood pressure");

        visit = new Visit();
        visit.setId(1L);
        visit.setVisitDate(LocalDate.now());
        visit.setSickLeaveIssued(false);
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);

        sickLeave = new SickLeave();
        sickLeave.setId(1L);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
        sickLeave.setVisit(visit);

        treatment = new Treatment();
        treatment.setId(1L);
        treatment.setDescription("Prescribed medication");
        treatment.setVisit(visit);

        medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("Aspirin");
        medicine.setDosage("100mg");
        medicine.setFrequency("Daily");
        medicine.setTreatment(treatment);
        treatment.setMedicines(List.of(medicine));
    }

    @Test
    void testPatientCreateDTOToPatient() {
        PatientCreateDTO createDTO = PatientCreateDTO.builder()
                .name("John Doe")
                .egn("1234567890")
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(1L)
                .build();

        Patient patient = modelMapper.map(createDTO, Patient.class);

        assertEquals("John Doe", patient.getName());
        assertEquals("1234567890", patient.getEgn());
        assertEquals(LocalDate.now(), patient.getLastInsurancePaymentDate());
        assertNotNull(patient.getGeneralPractitioner());
        assertEquals(1L, patient.getGeneralPractitioner().getId());
        assertEquals(Collections.emptyList(), patient.getVisits()); // Fixed assertion
    }

    @Test
    void testPatientUpdateDTOToPatient() {
        PatientUpdateDTO updateDTO = PatientUpdateDTO.builder()
                .id(1L)
                .name("Jane Doe")
                .lastInsurancePaymentDate(LocalDate.now())
                .generalPractitionerId(1L)
                .build();

        Patient patient = modelMapper.map(updateDTO, Patient.class);

        assertEquals(1L, patient.getId());
        assertEquals("Jane Doe", patient.getName());
        assertEquals(LocalDate.now(), patient.getLastInsurancePaymentDate());
        assertNotNull(patient.getGeneralPractitioner());
        assertEquals(1L, patient.getGeneralPractitioner().getId());
        assertEquals(Collections.emptyList(), patient.getVisits()); // Fixed assertion
    }

    @Test
    void testPatientToViewDTO() {
        PatientViewDTO viewDTO = modelMapper.map(patient, PatientViewDTO.class);

        assertEquals(1L, viewDTO.getId());
        assertEquals("John Doe", viewDTO.getName());
        assertEquals("1234567890", viewDTO.getEgn());
        assertEquals(LocalDate.now(), viewDTO.getLastInsurancePaymentDate());
        assertEquals("Dr. Smith", viewDTO.getGeneralPractitioner().getName());
    }

    @Test
    void testPatientToViewDTOWithNullGeneralPractitioner() {
        patient.setGeneralPractitioner(null);
        PatientViewDTO viewDTO = modelMapper.map(patient, PatientViewDTO.class);

        assertEquals(1L, viewDTO.getId());
        assertEquals("John Doe", viewDTO.getName());
        assertEquals("1234567890", viewDTO.getEgn());
        assertEquals(LocalDate.now(), viewDTO.getLastInsurancePaymentDate());
        assertNull(viewDTO.getGeneralPractitioner());
    }

    @Test
    void testDoctorCreateDTOToDoctor() {
        DoctorCreateDTO createDTO = DoctorCreateDTO.builder()
                .name("Dr. Smith")
                .uniqueIdNumber("DOC123")
                .isGeneralPractitioner(true)
                .imageUrl("http://example.com/image.jpg")
                .specialtyIds(Set.of(1L))
                .build();

        Doctor doctor = modelMapper.map(createDTO, Doctor.class);

        assertEquals("Dr. Smith", doctor.getName());
        assertEquals("DOC123", doctor.getUniqueIdNumber());
        assertTrue(doctor.isGeneralPractitioner());
        assertEquals("http://example.com/image.jpg", doctor.getImageUrl());
        assertEquals(Collections.emptySet(), doctor.getSpecialties()); // Fixed assertion
        assertEquals(Collections.emptyList(), doctor.getPatients()); // Fixed assertion
        assertEquals(Collections.emptyList(), doctor.getVisits()); // Fixed assertion
    }

    @Test
    void testDoctorToViewDTO() {
        DoctorViewDTO viewDTO = modelMapper.map(doctor, DoctorViewDTO.class);

        assertEquals(1L, viewDTO.getId());
        assertEquals("Dr. Smith", viewDTO.getName());
        assertEquals("DOC123", viewDTO.getUniqueIdNumber());
        assertTrue(viewDTO.isGeneralPractitioner());
        assertEquals("http://example.com/image.jpg", viewDTO.getImageUrl());
        assertEquals(1, viewDTO.getSpecialties().size());
        assertEquals("Cardiology", viewDTO.getSpecialties().iterator().next().getName());
    }

    @Test
    void testDoctorToViewDTOWithEmptySpecialties() {
        doctor.setSpecialties(Collections.emptySet());
        DoctorViewDTO viewDTO = modelMapper.map(doctor, DoctorViewDTO.class);

        assertEquals(0, viewDTO.getSpecialties().size());
    }

    @Test
    void testDiagnosisCreateDTOToDiagnosis() {
        DiagnosisCreateDTO createDTO = DiagnosisCreateDTO.builder()
                .name("Hypertension")
                .description("High blood pressure")
                .build();

        Diagnosis diagnosis = modelMapper.map(createDTO, Diagnosis.class);

        assertEquals("Hypertension", diagnosis.getName());
        assertEquals("High blood pressure", diagnosis.getDescription());
    }

    @Test
    void testDiagnosisToViewDTO() {
        DiagnosisViewDTO viewDTO = modelMapper.map(diagnosis, DiagnosisViewDTO.class);

        assertEquals(1L, viewDTO.getId());
        assertEquals("Hypertension", viewDTO.getName());
        assertEquals("High blood pressure", viewDTO.getDescription());
    }

    @Test
    void testSickLeaveCreateDTOToSickLeave() {
        SickLeaveCreateDTO createDTO = SickLeaveCreateDTO.builder()
                .startDate(LocalDate.now())
                .durationDays(5)
                .visitId(1L)
                .build();

        SickLeave sickLeave = modelMapper.map(createDTO, SickLeave.class);

        assertEquals(LocalDate.now(), sickLeave.getStartDate());
        assertEquals(5, sickLeave.getDurationDays());
        assertNotNull(sickLeave.getVisit());
        assertEquals(1L, sickLeave.getVisit().getId());
    }

    @Test
    void testSickLeaveToViewDTO() {
        SickLeaveViewDTO viewDTO = modelMapper.map(sickLeave, SickLeaveViewDTO.class);

        assertEquals(1L, viewDTO.getId());
        assertEquals(LocalDate.now(), viewDTO.getStartDate());
        assertEquals(5, viewDTO.getDurationDays());
        assertEquals(1L, viewDTO.getVisitId());
    }

    @Test
    void testVisitCreateDTOToVisit() {
        VisitCreateDTO createDTO = VisitCreateDTO.builder()
                .visitDate(LocalDate.now())
                .sickLeaveIssued(false)
                .patientId(1L)
                .doctorId(1L)
                .diagnosisId(1L)
                .build();

        Visit visit = modelMapper.map(createDTO, Visit.class);

        assertEquals(LocalDate.now(), visit.getVisitDate());
        assertFalse(visit.isSickLeaveIssued());
        assertNotNull(visit.getPatient());
        assertEquals(1L, visit.getPatient().getId());
        assertNotNull(visit.getDoctor());
        assertEquals(1L, visit.getDoctor().getId());
        assertNotNull(visit.getDiagnosis());
        assertEquals(1L, visit.getDiagnosis().getId());
        assertNull(visit.getSickLeave());
        assertNull(visit.getTreatment());
    }

    @Test
    void testVisitToViewDTO() {
        VisitViewDTO viewDTO = modelMapper.map(visit, VisitViewDTO.class);

        assertEquals(1L, viewDTO.getId());
        assertEquals(LocalDate.now(), viewDTO.getVisitDate());
        assertFalse(viewDTO.isSickLeaveIssued());
        assertEquals("John Doe", viewDTO.getPatient().getName());
        assertEquals("Dr. Smith", viewDTO.getDoctor().getName());
        assertEquals("Hypertension", viewDTO.getDiagnosis().getName());
        assertNull(viewDTO.getSickLeave());
        assertNull(viewDTO.getTreatment());
    }

    @Test
    void testVisitToViewDTOWithSickLeaveAndTreatment() {
        visit.setSickLeave(sickLeave);
        visit.setTreatment(treatment);
        VisitViewDTO viewDTO = modelMapper.map(visit, VisitViewDTO.class);

        assertNotNull(viewDTO.getSickLeave());
        assertEquals(1L, viewDTO.getSickLeave().getId());
        assertEquals(1L, viewDTO.getSickLeave().getVisitId()); // Updated assertion
        assertNotNull(viewDTO.getTreatment());
        assertEquals(1L, viewDTO.getTreatment().getId());
        assertEquals(1L, viewDTO.getTreatment().getVisitId()); // Updated assertion
    }

    @Test
    void testVisitToViewDTOWithNullSickLeave() {
        visit.setSickLeave(null);
        VisitViewDTO viewDTO = modelMapper.map(visit, VisitViewDTO.class);

        assertNull(viewDTO.getSickLeave());
    }

    @Test
    void testTreatmentCreateDTOToTreatment() {
        TreatmentCreateDTO createDTO = TreatmentCreateDTO.builder()
                .description("Prescribed medication")
                .visitId(1L)
                .medicineIds(List.of(1L))
                .build();

        Treatment treatment = modelMapper.map(createDTO, Treatment.class);

        assertEquals("Prescribed medication", treatment.getDescription());
        assertNotNull(treatment.getVisit());
        assertEquals(1L, treatment.getVisit().getId());
        assertEquals(Collections.emptyList(), treatment.getMedicines());
    }

    @Test
    void testTreatmentToViewDTO() {
        TreatmentViewDTO viewDTO = modelMapper.map(treatment, TreatmentViewDTO.class);

        assertEquals(1L, viewDTO.getId());
        assertEquals("Prescribed medication", viewDTO.getDescription());
        assertEquals(1L, viewDTO.getVisitId());
        assertEquals(1, viewDTO.getMedicines().size());
        assertEquals("Aspirin", viewDTO.getMedicines().get(0).getName());
    }

    @Test
    void testMedicineCreateDTOToMedicine() {
        MedicineCreateDTO createDTO = MedicineCreateDTO.builder()
                .name("Aspirin")
                .dosage("100mg")
                .frequency("Daily")
                .treatmentId(1L)
                .build();

        Medicine medicine = modelMapper.map(createDTO, Medicine.class);

        assertEquals("Aspirin", medicine.getName());
        assertEquals("100mg", medicine.getDosage());
        assertEquals("Daily", medicine.getFrequency());
        assertNotNull(medicine.getTreatment());
        assertEquals(1L, medicine.getTreatment().getId());
    }

    @Test
    void testMedicineToViewDTO() {
        MedicineViewDTO viewDTO = modelMapper.map(medicine, MedicineViewDTO.class);

        assertEquals(1L, viewDTO.getId());
        assertEquals("Aspirin", viewDTO.getName());
        assertEquals("100mg", viewDTO.getDosage());
        assertEquals("Daily", viewDTO.getFrequency());
        assertEquals(1L, viewDTO.getTreatmentId());
    }

    @Test
    void testSpecialtyCreateDTOToSpecialty() {
        SpecialtyCreateDTO createDTO = SpecialtyCreateDTO.builder()
                .name("Cardiology")
                .description("Heart-related specialties")
                .build();

        Specialty specialty = modelMapper.map(createDTO, Specialty.class);

        assertEquals("Cardiology", specialty.getName());
        assertEquals("Heart-related specialties", specialty.getDescription());
    }

    @Test
    void testSpecialtyToViewDTO() {
        SpecialtyViewDTO viewDTO = modelMapper.map(specialty, SpecialtyViewDTO.class);

        assertEquals(1L, viewDTO.getId());
        assertEquals("Cardiology", viewDTO.getName());
        assertEquals("Heart-related specialties", viewDTO.getDescription());
    }
}