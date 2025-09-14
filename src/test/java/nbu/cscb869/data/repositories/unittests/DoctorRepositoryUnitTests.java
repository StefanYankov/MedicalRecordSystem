package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorRepositoryUnitTests {

    @Mock
    private DoctorRepository doctorRepository;

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

    private SickLeave createSickLeave(LocalDate startDate, int durationDays, Visit visit) {
        return SickLeave.builder()
                .startDate(startDate)
                .durationDays(durationDays)
                .visit(visit)
                .build();
    }

    @Test
    void findByUniqueIdNumber_ExistingId_ReturnsDoctor_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        when(doctorRepository.findByUniqueIdNumber(doctor.getUniqueIdNumber())).thenReturn(Optional.of(doctor));

        Optional<Doctor> found = doctorRepository.findByUniqueIdNumber(doctor.getUniqueIdNumber());

        assertTrue(found.isPresent());
        assertEquals(doctor.getUniqueIdNumber(), found.get().getUniqueIdNumber());
        verify(doctorRepository).findByUniqueIdNumber(doctor.getUniqueIdNumber());
    }

    @Test
    void findByUniqueIdNumberContaining_Filter_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor("DOC123", true, "Dr. Jane Smith");
        Page<Doctor> page = new PageImpl<>(List.of(doctor));
        when(doctorRepository.findByUniqueIdNumberContaining(eq("123"), any(Pageable.class))).thenReturn(page);

        Page<Doctor> result = doctorRepository.findByUniqueIdNumberContaining("123", PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("DOC123", result.getContent().getFirst().getUniqueIdNumber());
        verify(doctorRepository).findByUniqueIdNumberContaining(eq("123"), any(Pageable.class));
    }

    @Test
    void findAll_WithSpecification_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        Page<Doctor> page = new PageImpl<>(List.of(doctor));
        Specification<Doctor> spec = mock(Specification.class);
        when(doctorRepository.findAll(eq(spec), any(Pageable.class))).thenReturn(page);

        Page<Doctor> result = doctorRepository.findAll(spec, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(doctor.getUniqueIdNumber(), result.getContent().getFirst().getUniqueIdNumber());
        verify(doctorRepository).findAll(eq(spec), any(Pageable.class));
    }

    @Test
    void findPatientsByGeneralPractitioner_WithPatients_ReturnsPaged_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        Patient patient = createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now());
        Page<Patient> page = new PageImpl<>(List.of(patient));
        when(doctorRepository.findPatientsByGeneralPractitioner(eq(doctor), any(Pageable.class))).thenReturn(page);

        Page<Patient> result = doctorRepository.findPatientsByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(patient.getEgn(), result.getContent().getFirst().getEgn());
        verify(doctorRepository).findPatientsByGeneralPractitioner(eq(doctor), any(Pageable.class));
    }

    @Test
    void findPatientCountByGeneralPractitioner_WithData_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. John Doe");
        DoctorPatientCountDTO dto = DoctorPatientCountDTO.builder()
                .doctor(doctor)
                .patientCount(1L)
                .build();
        when(doctorRepository.findPatientCountByGeneralPractitioner()).thenReturn(List.of(dto));

        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();

        assertEquals(1, result.size());
        assertEquals(doctor.getUniqueIdNumber(), result.getFirst().getDoctor().getUniqueIdNumber());
        assertEquals(1L, result.getFirst().getPatientCount());
        verify(doctorRepository).findPatientCountByGeneralPractitioner();
    }

    @Test
    void findVisitCountByDoctor_WithData_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Jane Smith");
        DoctorVisitCountDTO dto = DoctorVisitCountDTO.builder()
                .doctor(doctor)
                .visitCount(1L)
                .build();
        when(doctorRepository.findVisitCountByDoctor()).thenReturn(List.of(dto));

        List<DoctorVisitCountDTO> result = doctorRepository.findVisitCountByDoctor();

        assertEquals(1, result.size());
        assertEquals(doctor.getUniqueIdNumber(), result.getFirst().getDoctor().getUniqueIdNumber());
        assertEquals(1L, result.getFirst().getVisitCount());
        verify(doctorRepository).findVisitCountByDoctor();
    }

    @Test
    void findDoctorsWithMostSickLeaves_WithData_ReturnsList_HappyPath() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        DoctorSickLeaveCountDTO dto = DoctorSickLeaveCountDTO.builder()
                .doctor(doctor)
                .sickLeaveCount(1L)
                .build();
        when(doctorRepository.findDoctorsWithMostSickLeaves()).thenReturn(List.of(dto));

        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();

        assertEquals(1, result.size());
        assertEquals(doctor.getUniqueIdNumber(), result.getFirst().getDoctor().getUniqueIdNumber());
        assertEquals(1L, result.getFirst().getSickLeaveCount());
        verify(doctorRepository).findDoctorsWithMostSickLeaves();
    }

    @Test
    void findByUniqueIdNumber_NonExistentId_ReturnsEmpty_ErrorCase() {
        when(doctorRepository.findByUniqueIdNumber("NONEXISTENT")).thenReturn(Optional.empty());

        Optional<Doctor> found = doctorRepository.findByUniqueIdNumber("NONEXISTENT");

        assertFalse(found.isPresent());
        verify(doctorRepository).findByUniqueIdNumber("NONEXISTENT");
    }

    @Test
    void findByUniqueIdNumberContaining_NonExistentFilter_ReturnsEmptyPage_ErrorCase() {
        Page<Doctor> emptyPage = new PageImpl<>(List.of());
        when(doctorRepository.findByUniqueIdNumberContaining(eq("NONEXISTENT"), any(Pageable.class))).thenReturn(emptyPage);

        Page<Doctor> result = doctorRepository.findByUniqueIdNumberContaining("NONEXISTENT", PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(doctorRepository).findByUniqueIdNumberContaining(eq("NONEXISTENT"), any(Pageable.class));
    }

    @Test
    void findAll_WithSpecificationNoMatches_ReturnsEmptyPage_ErrorCase() {
        Specification<Doctor> spec = mock(Specification.class);
        Page<Doctor> emptyPage = new PageImpl<>(List.of());
        when(doctorRepository.findAll(eq(spec), any(Pageable.class))).thenReturn(emptyPage);

        Page<Doctor> result = doctorRepository.findAll(spec, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(doctorRepository).findAll(eq(spec), any(Pageable.class));
    }

    @Test
    void findPatientsByGeneralPractitioner_NoPatients_ReturnsEmptyPage_ErrorCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Bob White");
        Page<Patient> emptyPage = new PageImpl<>(List.of());
        when(doctorRepository.findPatientsByGeneralPractitioner(eq(doctor), any(Pageable.class))).thenReturn(emptyPage);

        Page<Patient> result = doctorRepository.findPatientsByGeneralPractitioner(doctor, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(doctorRepository).findPatientsByGeneralPractitioner(eq(doctor), any(Pageable.class));
    }

    @Test
    void findPatientCountByGeneralPractitioner_NoData_ReturnsEmptyList_ErrorCase() {
        when(doctorRepository.findPatientCountByGeneralPractitioner()).thenReturn(List.of());

        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();

        assertTrue(result.isEmpty());
        verify(doctorRepository).findPatientCountByGeneralPractitioner();
    }

    @Test
    void findVisitCountByDoctor_NoData_ReturnsEmptyList_ErrorCase() {
        when(doctorRepository.findVisitCountByDoctor()).thenReturn(List.of());

        List<DoctorVisitCountDTO> result = doctorRepository.findVisitCountByDoctor();

        assertTrue(result.isEmpty());
        verify(doctorRepository).findVisitCountByDoctor();
    }

    @Test
    void findDoctorsWithMostSickLeaves_NoData_ReturnsEmptyList_ErrorCase() {
        when(doctorRepository.findDoctorsWithMostSickLeaves()).thenReturn(List.of());

        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();

        assertTrue(result.isEmpty());
        verify(doctorRepository).findDoctorsWithMostSickLeaves();
    }

    @Test
    void findByUniqueIdNumberContaining_EmptyFilter_ReturnsAllDoctors_EdgeCase() {
        Doctor doctor1 = createDoctor("DOC123", true, "Dr. John Doe");
        Doctor doctor2 = createDoctor("DOC456", false, "Dr. Jane Smith");
        Page<Doctor> page = new PageImpl<>(List.of(doctor1, doctor2));
        when(doctorRepository.findByUniqueIdNumberContaining(eq(""), any(Pageable.class))).thenReturn(page);

        Page<Doctor> result = doctorRepository.findByUniqueIdNumberContaining("", PageRequest.of(0, 2));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(doctorRepository).findByUniqueIdNumberContaining(eq(""), any(Pageable.class));
    }

    @Test
    void findPatientsByGeneralPractitioner_WithLargePageSize_EdgeCase() {
        Doctor doctor = createDoctor(TestDataUtils.generateUniqueIdNumber(), true, "Dr. Alice Brown");
        List<Patient> patients = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            patients.add(createPatient(TestDataUtils.generateValidEgn(), doctor, LocalDate.now()));
        }
        Page<Patient> page = new PageImpl<>(patients);
        when(doctorRepository.findPatientsByGeneralPractitioner(eq(doctor), any(Pageable.class))).thenReturn(page);

        Page<Patient> result = doctorRepository.findPatientsByGeneralPractitioner(doctor, PageRequest.of(0, 10));

        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getContent().size());
        verify(doctorRepository).findPatientsByGeneralPractitioner(eq(doctor), any(Pageable.class));
    }
}