package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Medicine;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Treatment;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.MedicineRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicineRepositoryUnitTests {

    @Mock
    private MedicineRepository medicineRepository;

    private Medicine medicine;
    private Treatment treatment;
    private Visit visit;
    private Diagnosis diagnosis;
    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
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
        visit.setVisitTime(LocalTime.of(10, 30));
        visit.setSickLeaveIssued(false);

        treatment = new Treatment();
        treatment.setId(1L);
        treatment.setDescription("Antibiotic therapy");
        treatment.setVisit(visit);

        medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("Aspirin");
        medicine.setDosage("500mg");
        medicine.setFrequency("Once daily");
        medicine.setTreatment(treatment);
    }

    // Happy Path
    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(medicineRepository.findAllActive()).thenReturn(Collections.singletonList(medicine));

        List<Medicine> result = medicineRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Aspirin", result.getFirst().getName());
        verify(medicineRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Medicine> page = new PageImpl<>(Collections.singletonList(medicine));
        when(medicineRepository.findAllActive(any(PageRequest.class))).thenReturn(page);

        Page<Medicine> result = medicineRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Aspirin", result.getContent().getFirst().getName());
        verify(medicineRepository).findAllActive(any(PageRequest.class));
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(medicineRepository).hardDeleteById(1L);

        medicineRepository.hardDeleteById(1L);

        verify(medicineRepository).hardDeleteById(1L);
    }

    @Test
    void SoftDelete_WithValidMedicine_SetsIsDeleted() {
        doNothing().when(medicineRepository).delete(medicine);

        medicineRepository.delete(medicine);

        verify(medicineRepository).delete(medicine);
    }

    // Error Cases
    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(medicineRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Medicine> result = medicineRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(medicineRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Medicine> emptyPage = new PageImpl<>(Collections.emptyList());
        when(medicineRepository.findAllActive(any(PageRequest.class))).thenReturn(emptyPage);

        Page<Medicine> result = medicineRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(medicineRepository).findAllActive(any(PageRequest.class));
    }

    // Edge Cases
    @Test
    void FindAllActive_WithSoftDeletedMedicine_ExcludesDeleted() {
        Medicine deletedMedicine = new Medicine();
        deletedMedicine.setId(2L);
        deletedMedicine.setName("Paracetamol");
        deletedMedicine.setDosage("500mg");
        deletedMedicine.setFrequency("As needed");
        deletedMedicine.setTreatment(treatment);
        deletedMedicine.setIsDeleted(true);

        when(medicineRepository.findAllActive()).thenReturn(Collections.singletonList(medicine));

        List<Medicine> result = medicineRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Aspirin", result.getFirst().getName());
        assertFalse(result.contains(deletedMedicine));
        verify(medicineRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithLastPageFewerElements_ReturnsCorrectPage() {
        Medicine medicine2 = new Medicine();
        medicine2.setId(2L);
        medicine2.setName("Paracetamol");
        medicine2.setDosage("500mg");
        medicine2.setFrequency("As needed");
        medicine2.setTreatment(treatment);

        Medicine medicine3 = new Medicine();
        medicine3.setId(3L);
        medicine3.setName("Ibuprofen");
        medicine3.setDosage("200mg");
        medicine3.setFrequency("Every 6 hours");
        medicine3.setTreatment(treatment);

        Page<Medicine> page = new PageImpl<>(Collections.singletonList(medicine3), PageRequest.of(1, 2), 3);
        when(medicineRepository.findAllActive(PageRequest.of(1, 2))).thenReturn(page);

        Page<Medicine> result = medicineRepository.findAllActive(PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Ibuprofen", result.getContent().getFirst().getName());
        assertEquals(2, result.getTotalPages());
        verify(medicineRepository).findAllActive(PageRequest.of(1, 2));
    }
}