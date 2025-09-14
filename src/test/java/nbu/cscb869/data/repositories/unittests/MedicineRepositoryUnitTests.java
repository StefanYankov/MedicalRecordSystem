// nbu.cscb869.data.repositories.unittests/MedicineRepositoryUnitTests.java
package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.MedicineRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicineRepositoryUnitTests {

    @Mock
    private MedicineRepository medicineRepository;

    private Medicine createMedicine(String name, String dosage, String frequency, Treatment treatment) {
        return Medicine.builder()
                .name(name)
                .dosage(dosage)
                .frequency(frequency)
                .treatment(treatment)
                .build();
    }

    private Treatment createTreatment(String description, Visit visit) {
        return Treatment.builder()
                .description(description)
                .visit(visit)
                .build();
    }

    private Visit createVisit(Patient patient, Doctor doctor, Diagnosis diagnosis, LocalDate visitDate, LocalTime visitTime, SickLeave sickLeave) {
        Visit visit = Visit.builder()
                .patient(patient)
                .doctor(doctor)
                .diagnosis(diagnosis)
                .visitDate(visitDate)
                .visitTime(visitTime)
                .build();
        if (sickLeave != null) {
            visit.setSickLeave(sickLeave);
            sickLeave.setVisit(visit);
        }
        return visit;
    }

    private Diagnosis createDiagnosis(String name, String description) {
        return Diagnosis.builder()
                .name(name)
                .description(description)
                .build();
    }

    private Doctor createDoctor(String uniqueIdNumber, boolean isGeneralPractitioner, String name) {
        return Doctor.builder()
                .uniqueIdNumber(uniqueIdNumber)
                .isGeneralPractitioner(isGeneralPractitioner)
                .name(name)
                .build();
    }

    private Patient createPatient(String egn, Doctor generalPractitioner, LocalDate lastInsurancePaymentDate) {
        return Patient.builder()
                .egn(egn)
                .generalPractitioner(generalPractitioner)
                .lastInsurancePaymentDate(lastInsurancePaymentDate)
                .build();
    }

    @Test
    void findAll_WithData_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Treatment treatment = createTreatment("Antibiotic therapy", visit);
        Medicine medicine = createMedicine("Aspirin", "500mg", "Once daily", treatment);
        Page<Medicine> page = new PageImpl<>(List.of(medicine));
        when(medicineRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Medicine> result = medicineRepository.findAll(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Aspirin", result.getContent().getFirst().getName());
        verify(medicineRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAll_NoData_ReturnsEmptyPage_ErrorCase() {
        Page<Medicine> emptyPage = new PageImpl<>(List.of());
        when(medicineRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<Medicine> result = medicineRepository.findAll(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(medicineRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAll_WithLargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Diagnosis diagnosis = createDiagnosis("Flu", "Viral infection");
        Visit visit = createVisit(patient, doctor, diagnosis, LocalDate.now(), LocalTime.of(10, 30), null);
        Treatment treatment = createTreatment("Antibiotic therapy", visit);
        List<Medicine> medicines = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            medicines.add(createMedicine("Medicine" + i, "10mg", "Once daily", treatment));
        }
        Page<Medicine> page = new PageImpl<>(medicines);
        when(medicineRepository.findAll(eq(PageRequest.of(0, 10)))).thenReturn(page);

        Page<Medicine> result = medicineRepository.findAll(PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
        verify(medicineRepository).findAll(eq(PageRequest.of(0, 10)));
    }
}