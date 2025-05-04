package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.models.Specialty;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Visit;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.repositories.SpecialtyRepository;
import nbu.cscb869.data.repositories.DoctorRepository;
import nbu.cscb869.data.repositories.VisitRepository;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyRepositoryUnitTests {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DiagnosisRepository diagnosisRepository;

    private Specialty specialty;
    private Doctor doctor;
    private Visit visit;
    private Patient patient;
    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        // Setup test data (not persisted, just for mocking)
        diagnosis = new Diagnosis();
        diagnosis.setId(1L);
        diagnosis.setName("Flu");
        diagnosis.setDescription("Viral infection");

        patient = new Patient();
        patient.setId(1L);
        patient.setName("Jane Doe");
        patient.setEgn(TestDataUtils.generateValidEgn());
        patient.setLastInsurancePaymentDate(LocalDate.now());

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Smith");
        doctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        doctor.setGeneralPractitioner(true);

        patient.setGeneralPractitioner(doctor);

        visit = new Visit();
        visit.setId(1L);
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(LocalDate.now());
        visit.setSickLeaveIssued(false);

        specialty = new Specialty();
        specialty.setId(1L);
        specialty.setName("Cardiology");
        specialty.setDescription("Heart-related specialties");
        specialty.setDoctors(Set.of(doctor));
    }

    // Happy Path
    @Test
    void FindByName_WithValidName_ReturnsSpecialty() {
        when(specialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(specialty));

        Optional<Specialty> foundSpecialty = specialtyRepository.findByName("Cardiology");

        assertTrue(foundSpecialty.isPresent());
        assertEquals("Cardiology", foundSpecialty.get().getName());
        verify(specialtyRepository).findByName("Cardiology");
    }

    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(specialtyRepository.findAllActive()).thenReturn(Collections.singletonList(specialty));

        List<Specialty> result = specialtyRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Cardiology", result.getFirst().getName());
        verify(specialtyRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Specialty> page = new PageImpl<>(Collections.singletonList(specialty));
        when(specialtyRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        Page<Specialty> result = specialtyRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Cardiology", result.getContent().getFirst().getName());
        verify(specialtyRepository).findAllActive(any(Pageable.class));
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(specialtyRepository).hardDeleteById(1L);

        specialtyRepository.hardDeleteById(1L);

        verify(specialtyRepository).hardDeleteById(1L);
    }

    // Error Cases
    @Test
    void FindByName_WithNonExistentName_ReturnsEmpty() {
        when(specialtyRepository.findByName("Neurology")).thenReturn(Optional.empty());

        Optional<Specialty> foundSpecialty = specialtyRepository.findByName("Neurology");

        assertFalse(foundSpecialty.isPresent());
        verify(specialtyRepository).findByName("Neurology");
    }

    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(specialtyRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Specialty> result = specialtyRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(specialtyRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Specialty> emptyPage = new PageImpl<>(Collections.emptyList());
        when(specialtyRepository.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

        Page<Specialty> result = specialtyRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(specialtyRepository).findAllActive(any(Pageable.class));
    }

    // Edge Cases
    @Test
    void FindByName_WithSoftDeletedSpecialty_ReturnsEmpty() {
        Specialty deletedSpecialty = new Specialty();
        deletedSpecialty.setId(2L);
        deletedSpecialty.setName("Deleted");
        deletedSpecialty.setDescription("Deleted specialty");
        deletedSpecialty.setIsDeleted(true);

        when(specialtyRepository.findByName("Deleted")).thenReturn(Optional.empty());

        Optional<Specialty> foundSpecialty = specialtyRepository.findByName("Deleted");

        assertFalse(foundSpecialty.isPresent());
        verify(specialtyRepository).findByName("Deleted");
    }

    @Test
    void FindAllActivePaged_WithLastPageFewerElements_ReturnsCorrectPage() {
        Specialty specialty2 = new Specialty();
        specialty2.setId(2L);
        specialty2.setName("Neurology");
        specialty2.setDescription("Brain-related specialties");
        specialty2.setDoctors(Set.of(doctor));

        Specialty specialty3 = new Specialty();
        specialty3.setId(3L);
        specialty3.setName("Pediatrics");
        specialty3.setDescription("Child-related specialties");
        specialty3.setDoctors(Set.of(doctor));

        Page<Specialty> page = new PageImpl<>(Collections.singletonList(specialty3), PageRequest.of(1, 2), 3);
        when(specialtyRepository.findAllActive(PageRequest.of(1, 2))).thenReturn(page);

        Page<Specialty> result = specialtyRepository.findAllActive(PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Pediatrics", result.getContent().getFirst().getName());
        assertEquals(2, result.getTotalPages());
        verify(specialtyRepository).findAllActive(PageRequest.of(1, 2));
    }
}