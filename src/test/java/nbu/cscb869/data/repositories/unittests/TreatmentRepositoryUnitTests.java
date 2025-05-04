package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.TreatmentRepository;
import nbu.cscb869.data.repositories.MedicineRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentRepositoryUnitTests {

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DiagnosisRepository diagnosisRepository;

    private Treatment treatment;
    private Medicine medicine;
    private Visit visit;
    private Doctor doctor;
    private Patient patient;
    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        // Setup test data (not persisted, just for mocking)
        diagnosis = new Diagnosis();
        diagnosis.setId(1L);
        diagnosis.setName("Flu");
        diagnosis.setDescription("Viral infection");

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Smith");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);

        patient = new Patient();
        patient.setId(1L);
        patient.setName("Jane Doe");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setGeneralPractitioner(doctor);
        patient.setLastInsurancePaymentDate(LocalDate.now());

        visit = new Visit();
        visit.setId(1L);
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setSickLeaveIssued(false);

        medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("Aspirin");
        medicine.setDosage("500mg");
        medicine.setFrequency("Once daily");

        treatment = new Treatment();
        treatment.setId(1L);
        treatment.setDescription("Antibiotic therapy");
        treatment.setVisit(visit);
        treatment.setMedicines(Collections.singletonList(medicine));
        medicine.setTreatment(treatment);
    }

    // Happy Path
    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(treatmentRepository.findAllActive()).thenReturn(Collections.singletonList(treatment));

        List<Treatment> result = treatmentRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Antibiotic therapy", result.getFirst().getDescription());
        verify(treatmentRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Treatment> page = new PageImpl<>(Collections.singletonList(treatment));
        when(treatmentRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        Page<Treatment> result = treatmentRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Antibiotic therapy", result.getContent().getFirst().getDescription());
        verify(treatmentRepository).findAllActive(any(Pageable.class));
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(treatmentRepository).hardDeleteById(1L);

        treatmentRepository.hardDeleteById(1L);

        verify(treatmentRepository).hardDeleteById(1L);
    }

    // Error Cases
    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(treatmentRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Treatment> result = treatmentRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(treatmentRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Treatment> emptyPage = new PageImpl<>(Collections.emptyList());
        when(treatmentRepository.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

        Page<Treatment> result = treatmentRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(treatmentRepository).findAllActive(any(Pageable.class));
    }

    // Edge Cases
    @Test
    void FindAllActive_WithSoftDeletedTreatment_ReturnsEmpty() {
        Treatment deletedTreatment = new Treatment();
        deletedTreatment.setId(2L);
        deletedTreatment.setDescription("Deleted therapy");
        deletedTreatment.setVisit(visit);
        deletedTreatment.setIsDeleted(true);

        when(treatmentRepository.findAllActive()).thenReturn(Collections.singletonList(treatment));

        List<Treatment> result = treatmentRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Antibiotic therapy", result.getFirst().getDescription());
        assertFalse(result.contains(deletedTreatment));
        verify(treatmentRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithLastPageFewerElements_ReturnsCorrectPage() {
        Treatment treatment2 = new Treatment();
        treatment2.setId(2L);
        treatment2.setDescription("Pain relief therapy");
        treatment2.setVisit(visit);
        treatment2.setMedicines(Collections.singletonList(medicine));

        Treatment treatment3 = new Treatment();
        treatment3.setId(3L);
        treatment3.setDescription("Antiviral therapy");
        treatment3.setVisit(visit);
        treatment3.setMedicines(Collections.singletonList(medicine));

        Page<Treatment> page = new PageImpl<>(Collections.singletonList(treatment3), PageRequest.of(1, 2), 3);
        when(treatmentRepository.findAllActive(PageRequest.of(1, 2))).thenReturn(page);

        Page<Treatment> result = treatmentRepository.findAllActive(PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Antiviral therapy", result.getContent().getFirst().getDescription());
        assertEquals(2, result.getTotalPages());
        verify(treatmentRepository).findAllActive(PageRequest.of(1, 2));
    }
}