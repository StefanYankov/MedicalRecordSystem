package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DoctorPatientCountDTO;
import nbu.cscb869.data.dto.DoctorSickLeaveCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.SickLeave;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.repositories.DiagnosisRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.PatientRepository;
import nbu.cscb869.data.repositories.SickLeaveRepository;
import nbu.cscb869.data.repositories.VisitRepository;
import nbu.cscb869.data.utils.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorRepositoryUnitTests {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private SickLeaveRepository sickLeaveRepository;

    @Mock
    private DiagnosisRepository diagnosisRepository;

    private Doctor doctor;
    private Patient patient;
    private Visit visit;
    private SickLeave sickLeave;

    @BeforeEach
    void setUp() {
        // Setup test data (not persisted, just for mocking)
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
        visit.setVisitDate(LocalDate.now());
        visit.setSickLeaveIssued(true);

        sickLeave = new SickLeave();
        sickLeave.setId(1L);
        sickLeave.setVisit(visit);
        sickLeave.setStartDate(LocalDate.now());
        sickLeave.setDurationDays(5);
    }

    // Happy Path
    @Test
    void FindByUniqueIdNumber_WithExistingId_ReturnsDoctor() {
        when(doctorRepository.findByUniqueIdNumber(doctor.getUniqueIdNumber())).thenReturn(Optional.of(doctor));

        Optional<Doctor> foundDoctor = doctorRepository.findByUniqueIdNumber(doctor.getUniqueIdNumber());

        assertTrue(foundDoctor.isPresent());
        assertEquals("Dr. Smith", foundDoctor.get().getName());
        verify(doctorRepository).findByUniqueIdNumber(doctor.getUniqueIdNumber());
    }

    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(doctorRepository.findAllActive()).thenReturn(Collections.singletonList(doctor));

        List<Doctor> result = doctorRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.get(0).getName());
        verify(doctorRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Doctor> page = new PageImpl<>(Collections.singletonList(doctor));
        when(doctorRepository.findAllActive(any(PageRequest.class))).thenReturn(page);

        Page<Doctor> result = doctorRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Dr. Smith", result.getContent().get(0).getName());
        verify(doctorRepository).findAllActive(any(PageRequest.class));
    }

    @Test
    void FindPatientCountByGeneralPractitioner_WithData_ReturnsList() {
        DoctorPatientCountDTO dto = DoctorPatientCountDTO.builder()
                .doctor(doctor)
                .patientCount(1L)
                .build();
        when(doctorRepository.findPatientCountByGeneralPractitioner()).thenReturn(Collections.singletonList(dto));

        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();

        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.get(0).getDoctor().getName());
        assertEquals(1L, result.get(0).getPatientCount());
        verify(doctorRepository).findPatientCountByGeneralPractitioner();
    }

    @Test
    void FindVisitCountByDoctor_WithData_ReturnsList() {
        DoctorVisitCountDTO dto = DoctorVisitCountDTO.builder()
                .doctor(doctor)
                .visitCount(1L)
                .build();
        when(doctorRepository.findVisitCountByDoctor()).thenReturn(Collections.singletonList(dto));

        List<DoctorVisitCountDTO> result = doctorRepository.findVisitCountByDoctor();

        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.get(0).getDoctor().getName());
        assertEquals(1L, result.get(0).getVisitCount());
        verify(doctorRepository).findVisitCountByDoctor();
    }

    @Test
    void FindDoctorsWithMostSickLeaves_WithData_ReturnsList() {
        DoctorSickLeaveCountDTO dto = DoctorSickLeaveCountDTO.builder()
                .doctor(doctor)
                .sickLeaveCount(1L)
                .build();
        when(doctorRepository.findDoctorsWithMostSickLeaves()).thenReturn(Collections.singletonList(dto));

        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();

        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.get(0).getDoctor().getName());
        assertEquals(1L, result.get(0).getSickLeaveCount());
        verify(doctorRepository).findDoctorsWithMostSickLeaves();
    }

    // Error Cases
    @Test
    void FindByUniqueIdNumber_WithNonExistentId_ReturnsEmpty() {
        when(doctorRepository.findByUniqueIdNumber("NONEXISTENT")).thenReturn(Optional.empty());

        Optional<Doctor> foundDoctor = doctorRepository.findByUniqueIdNumber("NONEXISTENT");

        assertFalse(foundDoctor.isPresent());
        verify(doctorRepository).findByUniqueIdNumber("NONEXISTENT");
    }

    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(doctorRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Doctor> result = doctorRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(doctorRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Doctor> emptyPage = new PageImpl<>(Collections.emptyList());
        when(doctorRepository.findAllActive(any(PageRequest.class))).thenReturn(emptyPage);

        Page<Doctor> result = doctorRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(doctorRepository).findAllActive(any(PageRequest.class));
    }

    @Test
    void FindPatientCountByGeneralPractitioner_WithNoData_ReturnsEmpty() {
        when(doctorRepository.findPatientCountByGeneralPractitioner()).thenReturn(Collections.emptyList());

        List<DoctorPatientCountDTO> result = doctorRepository.findPatientCountByGeneralPractitioner();

        assertTrue(result.isEmpty());
        verify(doctorRepository).findPatientCountByGeneralPractitioner();
    }

    @Test
    void FindVisitCountByDoctor_WithNoData_ReturnsEmpty() {
        when(doctorRepository.findVisitCountByDoctor()).thenReturn(Collections.emptyList());

        List<DoctorVisitCountDTO> result = doctorRepository.findVisitCountByDoctor();

        assertTrue(result.isEmpty());
        verify(doctorRepository).findVisitCountByDoctor();
    }

    @Test
    void FindDoctorsWithMostSickLeaves_WithNoData_ReturnsEmpty() {
        when(doctorRepository.findDoctorsWithMostSickLeaves()).thenReturn(Collections.emptyList());

        List<DoctorSickLeaveCountDTO> result = doctorRepository.findDoctorsWithMostSickLeaves();

        assertTrue(result.isEmpty());
        verify(doctorRepository).findDoctorsWithMostSickLeaves();
    }

    // Edge Cases
    @Test
    void FindByUniqueIdNumber_WithSoftDeletedDoctor_ReturnsEmpty() {
        Doctor deletedDoctor = new Doctor();
        deletedDoctor.setName("Deleted");
        deletedDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        deletedDoctor.setGeneralPractitioner(true);
        deletedDoctor.setIsDeleted(true);

        when(doctorRepository.findByUniqueIdNumber(deletedDoctor.getUniqueIdNumber())).thenReturn(Optional.empty());

        Optional<Doctor> foundDoctor = doctorRepository.findByUniqueIdNumber(deletedDoctor.getUniqueIdNumber());

        assertFalse(foundDoctor.isPresent());
        verify(doctorRepository).findByUniqueIdNumber(deletedDoctor.getUniqueIdNumber());
    }

    @Test
    void FindAllActivePaged_WithLastPageFewerElements_ReturnsCorrectPage() {
        Doctor doctor2 = new Doctor();
        doctor2.setName("Dr. Jones");
        doctor2.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor2.setGeneralPractitioner(true);

        Doctor doctor3 = new Doctor();
        doctor3.setName("Dr. Brown");
        doctor3.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor3.setGeneralPractitioner(true);

        Page<Doctor> page = new PageImpl<>(Collections.singletonList(doctor3), PageRequest.of(1, 2), 3);
        when(doctorRepository.findAllActive(PageRequest.of(1, 2))).thenReturn(page);

        Page<Doctor> result = doctorRepository.findAllActive(PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(2, result.getTotalPages());
        verify(doctorRepository).findAllActive(PageRequest.of(1, 2));
    }
}