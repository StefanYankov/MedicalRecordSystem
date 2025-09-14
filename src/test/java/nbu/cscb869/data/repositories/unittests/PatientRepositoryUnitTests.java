package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.PatientRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientRepositoryUnitTests {

    @Mock
    private PatientRepository patientRepository;

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
    void findByEgn_ExistingEgn_ReturnsPatient_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        when(patientRepository.findByEgn(patient.getEgn())).thenReturn(Optional.of(patient));

        Optional<Patient> found = patientRepository.findByEgn(patient.getEgn());

        assertTrue(found.isPresent());
        assertEquals(patient.getEgn(), found.get().getEgn());
        verify(patientRepository).findByEgn(patient.getEgn());
    }

    @Test
    void findByGeneralPractitioner_ValidDoctor_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Page<Patient> page = new PageImpl<>(List.of(patient));
        when(patientRepository.findByGeneralPractitioner(eq(doctor), any(Pageable.class))).thenReturn(page);

        Page<Patient> result = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(patient.getEgn(), result.getContent().getFirst().getEgn());
        verify(patientRepository).findByGeneralPractitioner(eq(doctor), any(Pageable.class));
    }

    @Test
    void countPatientsByGeneralPractitioner_WithData_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        DoctorPatientCountDTO dto = DoctorPatientCountDTO.builder()
                .doctor(doctor)
                .patientCount(1L)
                .build();
        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(List.of(dto));

        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();

        assertEquals(1, result.size());
        assertEquals(doctor.getUniqueIdNumber(), result.getFirst().getDoctor().getUniqueIdNumber());
        assertEquals(1L, result.getFirst().getPatientCount());
        verify(patientRepository).countPatientsByGeneralPractitioner();
    }

    @Test
    void findByEgnContaining_Filter_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Patient patient = createPatient("1234567890", doctor, LocalDate.now());
        Page<Patient> page = new PageImpl<>(List.of(patient));
        when(patientRepository.findByEgnContaining(eq("123"), any(Pageable.class))).thenReturn(page);

        Page<Patient> result = patientRepository.findByEgnContaining("123", PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("1234567890", result.getContent().getFirst().getEgn());
        verify(patientRepository).findByEgnContaining(eq("123"), any(Pageable.class));
    }

    @Test
    void findByEgn_NonExistentEgn_ReturnsEmpty_ErrorCase() {
        when(patientRepository.findByEgn("1234567890")).thenReturn(Optional.empty());

        Optional<Patient> found = patientRepository.findByEgn("1234567890");

        assertFalse(found.isPresent());
        verify(patientRepository).findByEgn("1234567890");
    }

    @Test
    void findByGeneralPractitioner_NoPatients_ReturnsEmptyPage_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Charlie Green");
        Page<Patient> emptyPage = new PageImpl<>(List.of());
        when(patientRepository.findByGeneralPractitioner(eq(doctor), any(Pageable.class))).thenReturn(emptyPage);

        Page<Patient> result = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(patientRepository).findByGeneralPractitioner(eq(doctor), any(Pageable.class));
    }

    @Test
    void countPatientsByGeneralPractitioner_NoData_ReturnsEmptyList_ErrorCase() {
        when(patientRepository.countPatientsByGeneralPractitioner()).thenReturn(List.of());

        List<DoctorPatientCountDTO> result = patientRepository.countPatientsByGeneralPractitioner();

        assertTrue(result.isEmpty());
        verify(patientRepository).countPatientsByGeneralPractitioner();
    }

    @Test
    void findByEgnContaining_NonExistentFilter_ReturnsEmptyPage_ErrorCase() {
        Page<Patient> emptyPage = new PageImpl<>(List.of());
        when(patientRepository.findByEgnContaining(eq("NONEXISTENT"), any(Pageable.class))).thenReturn(emptyPage);

        Page<Patient> result = patientRepository.findByEgnContaining("NONEXISTENT", PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(patientRepository).findByEgnContaining(eq("NONEXISTENT"), any(Pageable.class));
    }

    @Test
    void findByGeneralPractitioner_WithLargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. David Black");
        List<Patient> patients = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            patients.add(createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now()));
        }
        Page<Patient> page = new PageImpl<>(patients);
        when(patientRepository.findByGeneralPractitioner(eq(doctor), any(Pageable.class))).thenReturn(page);

        Page<Patient> result = patientRepository.findByGeneralPractitioner(doctor, PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
        verify(patientRepository).findByGeneralPractitioner(eq(doctor), any(Pageable.class));
    }

    @Test
    void findByEgnContaining_EmptyFilter_ReturnsAllPatients_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Eve White");
        Patient patient1 = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Patient patient2 = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Page<Patient> page = new PageImpl<>(List.of(patient1, patient2));
        when(patientRepository.findByEgnContaining(eq(""), any(Pageable.class))).thenReturn(page);

        Page<Patient> result = patientRepository.findByEgnContaining("", PageRequest.of(0, 2));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(patientRepository).findByEgnContaining(eq(""), any(Pageable.class));
    }
}