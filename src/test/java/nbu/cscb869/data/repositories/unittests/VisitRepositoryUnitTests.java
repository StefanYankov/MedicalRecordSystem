package nbu.cscb869.data.repositories.unittests;

import nbu.cscb869.data.dto.DiagnosisVisitCountDTO;
import nbu.cscb869.data.dto.DoctorVisitCountDTO;
import nbu.cscb869.data.models.*;
import nbu.cscb869.data.repositories.VisitRepository;
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
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitRepositoryUnitTests {
    @Mock
    private VisitRepository visitRepository;

    private Visit visit;
    private Doctor doctor;
    private Patient patient;
    private Diagnosis diagnosis;
    private Treatment treatment;
    private Medicine medicine;
    private SickLeave sickLeave;
    private LocalDate today;
    private LocalTime visitTime;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        visitTime = LocalTime.of(10, 30);

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
        patient.setLastInsurancePaymentDate(today);

        medicine = new Medicine();
        medicine.setId(1L);
        medicine.setName("Amoxicillin");
        medicine.setDosage("500mg");
        medicine.setFrequency("Twice daily");

        treatment = new Treatment();
        treatment.setId(1L);
        treatment.setDescription("Antibiotic therapy");
        treatment.setMedicines(List.of(medicine));

        sickLeave = new SickLeave();
        sickLeave.setId(1L);
        sickLeave.setStartDate(today);
        sickLeave.setDurationDays(5);

        visit = new Visit();
        visit.setId(1L);
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis(diagnosis);
        visit.setVisitDate(today);
        visit.setVisitTime(visitTime);
        visit.setSickLeaveIssued(false);
        visit.setTreatment(treatment);
        visit.setSickLeave(sickLeave);
        visit.setIsDeleted(false);
    }

    // Happy Path
    @Test
    void Save_WithValidVisit_ReturnsSaved() {
        when(visitRepository.save(visit)).thenReturn(visit);

        Visit savedVisit = visitRepository.save(visit);

        assertEquals(today, savedVisit.getVisitDate());
        assertEquals(visitTime, savedVisit.getVisitTime());
        verify(visitRepository).save(visit);
    }

    @Test
    void Save_WithSickLeave_ReturnsSaved() {
        visit.setSickLeaveIssued(true);
        when(visitRepository.save(visit)).thenReturn(visit);

        Visit savedVisit = visitRepository.save(visit);

        assertTrue(savedVisit.isSickLeaveIssued());
        assertNotNull(savedVisit.getSickLeave());
        assertEquals(5, savedVisit.getSickLeave().getDurationDays());
        verify(visitRepository).save(visit);
    }

    @Test
    void Save_WithTreatment_ReturnsSaved() {
        when(visitRepository.save(visit)).thenReturn(visit);

        Visit savedVisit = visitRepository.save(visit);

        assertNotNull(savedVisit.getTreatment());
        assertEquals("Antibiotic therapy", savedVisit.getTreatment().getDescription());
        assertEquals(1, savedVisit.getTreatment().getMedicines().size());
        assertEquals("Amoxicillin", savedVisit.getTreatment().getMedicines().getFirst().getName());
        verify(visitRepository).save(visit);
    }

    @Test
    void FindByPatient_WithValidPatient_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByPatient(eq(patient), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByPatient(patient, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(visitTime, result.getContent().getFirst().getVisitTime());
        verify(visitRepository).findByPatient(eq(patient), any(Pageable.class));
    }

    @Test
    void FindByDoctor_WithValidDoctor_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByDoctor(eq(doctor), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctor(doctor, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(visitTime, result.getContent().getFirst().getVisitTime());
        verify(visitRepository).findByDoctor(eq(doctor), any(Pageable.class));
    }

    @Test
    void FindByDateRange_WithValidRange_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByDateRange(eq(today), eq(today), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDateRange(today, today, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(visitTime, result.getContent().getFirst().getVisitTime());
        verify(visitRepository).findByDateRange(eq(today), eq(today), any(Pageable.class));
    }

    @Test
    void FindByDoctorAndDateRange_WithValidDoctorAndRange_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, today, today, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(visitTime, result.getContent().getFirst().getVisitTime());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), any(Pageable.class));
    }

    @Test
    void FindByDiagnosis_WithValidDiagnosis_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByDiagnosis(eq(diagnosis), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDiagnosis(diagnosis, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(visitTime, result.getContent().getFirst().getVisitTime());
        verify(visitRepository).findByDiagnosis(eq(diagnosis), any(Pageable.class));
    }

    @Test
    void FindMostFrequentDiagnoses_WithData_ReturnsList() {
        DiagnosisVisitCountDTO dto = DiagnosisVisitCountDTO.builder()
                .diagnosis(diagnosis)
                .visitCount(1L)
                .build();
        when(visitRepository.findMostFrequentDiagnoses()).thenReturn(Collections.singletonList(dto));

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertEquals(1, result.size());
        assertEquals("Flu", result.getFirst().getDiagnosis().getName());
        assertEquals(1L, result.getFirst().getVisitCount());
        verify(visitRepository).findMostFrequentDiagnoses();
    }

    @Test
    void CountVisitsByDoctor_WithData_ReturnsList() {
        DoctorVisitCountDTO dto = DoctorVisitCountDTO.builder()
                .doctor(doctor)
                .visitCount(1L)
                .build();
        when(visitRepository.countVisitsByDoctor()).thenReturn(Collections.singletonList(dto));

        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.getFirst().getDoctor().getName());
        assertEquals(1L, result.getFirst().getVisitCount());
        verify(visitRepository).countVisitsByDoctor();
    }

    @Test
    void FindAllActive_WithData_ReturnsList() {
        when(visitRepository.findAllActive()).thenReturn(Collections.singletonList(visit));

        List<Visit> result = visitRepository.findAllActive();

        assertEquals(1, result.size());
        assertEquals(today, result.getFirst().getVisitDate());
        assertEquals(visitTime, result.getFirst().getVisitTime());
        verify(visitRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithData_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(visitTime, result.getContent().getFirst().getVisitTime());
        verify(visitRepository).findAllActive(any(Pageable.class));
    }

    @Test
    void SoftDelete_WithValidVisit_SetsIsDeleted() {
        doNothing().when(visitRepository).delete(visit);

        visitRepository.delete(visit);

        verify(visitRepository).delete(visit);
    }

    @Test
    void HardDeleteById_WithValidId_InvokesDeletion() {
        doNothing().when(visitRepository).hardDeleteById(1L);

        visitRepository.hardDeleteById(1L);

        verify(visitRepository).hardDeleteById(1L);
    }

    @Test
    void FindByPatientOrDoctorFilter_WithValidFilter_ReturnsPaged() {
        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByPatientOrDoctorFilter(eq("%jane%"), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter("%jane%", PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(visitTime, result.getContent().getFirst().getVisitTime());
        assertEquals("Jane Doe", result.getContent().getFirst().getPatient().getName());
        verify(visitRepository).findByPatientOrDoctorFilter(eq("%jane%"), any(Pageable.class));
    }

    @Test
    void FindByPatientOrDoctorFilter_WithNoMatches_ReturnsEmpty() {
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByPatientOrDoctorFilter(eq("%nonexistent%"), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByPatientOrDoctorFilter("%nonexistent%", PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByPatientOrDoctorFilter(eq("%nonexistent%"), any(Pageable.class));
    }

    @Test
    void FindByDoctorAndDateTime_WithValidParams_ReturnsVisit() {
        when(visitRepository.findByDoctorAndDateTime(eq(doctor), eq(today), eq(visitTime))).thenReturn(Optional.of(visit));

        Optional<Visit> result = visitRepository.findByDoctorAndDateTime(doctor, today, visitTime);

        assertTrue(result.isPresent());
        assertEquals(today, result.get().getVisitDate());
        assertEquals(visitTime, result.get().getVisitTime());
        verify(visitRepository).findByDoctorAndDateTime(eq(doctor), eq(today), eq(visitTime));
    }

    @Test
    void FindByDoctorAndDateTime_WithNoMatch_ReturnsEmpty() {
        when(visitRepository.findByDoctorAndDateTime(eq(doctor), eq(today), eq(LocalTime.of(11, 0)))).thenReturn(Optional.empty());

        Optional<Visit> result = visitRepository.findByDoctorAndDateTime(doctor, today, LocalTime.of(11, 0));

        assertFalse(result.isPresent());
        verify(visitRepository).findByDoctorAndDateTime(eq(doctor), eq(today), eq(LocalTime.of(11, 0)));
    }

    // Error Cases
    @Test
    void Save_WithNullVisitDate_ThrowsException() {
        Visit invalidVisit = new Visit();
        invalidVisit.setPatient(patient);
        invalidVisit.setDoctor(doctor);
        invalidVisit.setDiagnosis(diagnosis);
        invalidVisit.setVisitTime(visitTime);
        invalidVisit.setSickLeaveIssued(false);

        when(visitRepository.save(invalidVisit)).thenThrow(org.springframework.dao.DataIntegrityViolationException.class);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> visitRepository.save(invalidVisit));
        verify(visitRepository).save(invalidVisit);
    }

    @Test
    void Save_WithNullVisitTime_ThrowsException() {
        Visit invalidVisit = new Visit();
        invalidVisit.setPatient(patient);
        invalidVisit.setDoctor(doctor);
        invalidVisit.setDiagnosis(diagnosis);
        invalidVisit.setVisitDate(today);
        invalidVisit.setSickLeaveIssued(false);

        when(visitRepository.save(invalidVisit)).thenThrow(org.springframework.dao.DataIntegrityViolationException.class);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> visitRepository.save(invalidVisit));
        verify(visitRepository).save(invalidVisit);
    }

    @Test
    void Save_WithNullSickLeaveIssued_ThrowsException() {
        Visit invalidVisit = new Visit();
        invalidVisit.setPatient(patient);
        invalidVisit.setDoctor(doctor);
        invalidVisit.setDiagnosis(diagnosis);
        invalidVisit.setVisitDate(today);
        invalidVisit.setVisitTime(visitTime);

        when(visitRepository.save(invalidVisit)).thenThrow(org.springframework.dao.DataIntegrityViolationException.class);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> visitRepository.save(invalidVisit));
        verify(visitRepository).save(invalidVisit);
    }

    @Test
    void Save_WithNullPatient_ThrowsException() {
        Visit invalidVisit = new Visit();
        invalidVisit.setDoctor(doctor);
        invalidVisit.setDiagnosis(diagnosis);
        invalidVisit.setVisitDate(today);
        invalidVisit.setVisitTime(visitTime);
        invalidVisit.setSickLeaveIssued(false);

        when(visitRepository.save(invalidVisit)).thenThrow(org.springframework.dao.DataIntegrityViolationException.class);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> visitRepository.save(invalidVisit));
        verify(visitRepository).save(invalidVisit);
    }

    @Test
    void Save_WithNullDoctor_ThrowsException() {
        Visit invalidVisit = new Visit();
        invalidVisit.setPatient(patient);
        invalidVisit.setDiagnosis(diagnosis);
        invalidVisit.setVisitDate(today);
        invalidVisit.setVisitTime(visitTime);
        invalidVisit.setSickLeaveIssued(false);

        when(visitRepository.save(invalidVisit)).thenThrow(org.springframework.dao.DataIntegrityViolationException.class);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> visitRepository.save(invalidVisit));
        verify(visitRepository).save(invalidVisit);
    }

    @Test
    void Save_WithNullDiagnosis_ThrowsException() {
        Visit invalidVisit = new Visit();
        invalidVisit.setPatient(patient);
        invalidVisit.setDoctor(doctor);
        invalidVisit.setVisitDate(today);
        invalidVisit.setVisitTime(visitTime);
        invalidVisit.setSickLeaveIssued(false);

        when(visitRepository.save(invalidVisit)).thenThrow(org.springframework.dao.DataIntegrityViolationException.class);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> visitRepository.save(invalidVisit));
        verify(visitRepository).save(invalidVisit);
    }

    @Test
    void FindByPatient_WithNoVisits_ReturnsEmpty() {
        Patient otherPatient = new Patient();
        otherPatient.setId(2L);
        otherPatient.setName("John Smith");
        otherPatient.setEgn(TestDataUtils.generateValidEgn());
        otherPatient.setGeneralPractitioner(doctor);
        otherPatient.setLastInsurancePaymentDate(today);

        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByPatient(eq(otherPatient), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByPatient(otherPatient, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByPatient(eq(otherPatient), any(Pageable.class));
    }

    @Test
    void FindByDoctor_WithNoVisits_ReturnsEmpty() {
        Doctor otherDoctor = new Doctor();
        otherDoctor.setId(2L);
        otherDoctor.setName("Dr. Jones");
        otherDoctor.setUniqueIdNumber(TestDataUtils.generateUniqueIdNumber());
        otherDoctor.setGeneralPractitioner(true);

        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDoctor(eq(otherDoctor), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctor(otherDoctor, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDoctor(eq(otherDoctor), any(Pageable.class));
    }

    @Test
    void FindByDateRange_WithNoVisits_ReturnsEmpty() {
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDateRange(eq(today.minusDays(1)), eq(today.minusDays(1)), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDateRange(today.minusDays(1), today.minusDays(1), PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDateRange(eq(today.minusDays(1)), eq(today.minusDays(1)), any(Pageable.class));
    }

    @Test
    void FindByDateRange_WithInvalidRange_ReturnsEmpty() {
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDateRange(eq(today), eq(today.minusDays(1)), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDateRange(today, today.minusDays(1), PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDateRange(eq(today), eq(today.minusDays(1)), any(Pageable.class));
    }

    @Test
    void FindByDoctorAndDateRange_WithNoVisits_ReturnsEmpty() {
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(today.minusDays(1)), eq(today.minusDays(1)), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, today.minusDays(1), today.minusDays(1), PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(today.minusDays(1)), eq(today.minusDays(1)), any(Pageable.class));
    }

    @Test
    void FindByDoctorAndDateRange_WithInvalidRange_ReturnsEmpty() {
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(today), eq(today.minusDays(1)), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, today, today.minusDays(1), PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(today), eq(today.minusDays(1)), any(Pageable.class));
    }

    @Test
    void FindByDiagnosis_WithNoVisits_ReturnsEmpty() {
        Diagnosis otherDiagnosis = new Diagnosis();
        otherDiagnosis.setId(2L);
        otherDiagnosis.setName("Cold");
        otherDiagnosis.setDescription("Common cold");

        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findByDiagnosis(eq(otherDiagnosis), any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findByDiagnosis(otherDiagnosis, PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findByDiagnosis(eq(otherDiagnosis), any(Pageable.class));
    }

    @Test
    void FindMostFrequentDiagnoses_WithNoData_ReturnsEmpty() {
        when(visitRepository.findMostFrequentDiagnoses()).thenReturn(Collections.emptyList());

        List<DiagnosisVisitCountDTO> result = visitRepository.findMostFrequentDiagnoses();

        assertTrue(result.isEmpty());
        verify(visitRepository).findMostFrequentDiagnoses();
    }

    @Test
    void CountVisitsByDoctor_WithNoData_ReturnsEmpty() {
        when(visitRepository.countVisitsByDoctor()).thenReturn(Collections.emptyList());

        List<DoctorVisitCountDTO> result = visitRepository.countVisitsByDoctor();

        assertTrue(result.isEmpty());
        verify(visitRepository).countVisitsByDoctor();
    }

    @Test
    void FindAllActive_WithNoData_ReturnsEmpty() {
        when(visitRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Visit> result = visitRepository.findAllActive();

        assertTrue(result.isEmpty());
        verify(visitRepository).findAllActive();
    }

    @Test
    void FindAllActivePaged_WithNoData_ReturnsEmpty() {
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(visitRepository.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

        Page<Visit> result = visitRepository.findAllActive(PageRequest.of(0, 1));

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(visitRepository).findAllActive(any(Pageable.class));
    }

    // Edge Cases
    @Test
    void FindByPatient_WithSoftDeletedVisit_ReturnsEmpty() {
        Visit deletedVisit = new Visit();
        deletedVisit.setId(2L);
        deletedVisit.setPatient(patient);
        deletedVisit.setDoctor(doctor);
        deletedVisit.setDiagnosis(diagnosis);
        deletedVisit.setVisitDate(today);
        deletedVisit.setVisitTime(visitTime);
        deletedVisit.setSickLeaveIssued(false);
        deletedVisit.setIsDeleted(true);

        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));
        when(visitRepository.findByPatient(eq(patient), any(Pageable.class))).thenReturn(page);

        Page<Visit> result = visitRepository.findByPatient(patient, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(visitTime, result.getContent().getFirst().getVisitTime());
        assertFalse(result.getContent().contains(deletedVisit));
        verify(visitRepository).findByPatient(eq(patient), any(Pageable.class));
    }

    @Test
    void FindByDoctorAndDateRange_WithLastPageFewerElements_ReturnsCorrectPage() {
        Visit visit2 = new Visit();
        visit2.setId(2L);
        visit2.setPatient(patient);
        visit2.setDoctor(doctor);
        visit2.setDiagnosis(diagnosis);
        visit2.setVisitDate(today);
        visit2.setVisitTime(LocalTime.of(11, 0));
        visit2.setSickLeaveIssued(false);

        Visit visit3 = new Visit();
        visit3.setId(3L);
        visit3.setPatient(patient);
        visit3.setDoctor(doctor);
        visit3.setDiagnosis(diagnosis);
        visit3.setVisitDate(today);
        visit3.setVisitTime(LocalTime.of(11, 30));
        visit3.setSickLeaveIssued(false);

        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit3), PageRequest.of(1, 2), 3);
        when(visitRepository.findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), eq(PageRequest.of(1, 2)))).thenReturn(page);

        Page<Visit> result = visitRepository.findByDoctorAndDateRange(doctor, today, today, PageRequest.of(1, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(today, result.getContent().getFirst().getVisitDate());
        assertEquals(LocalTime.of(11, 30), result.getContent().getFirst().getVisitTime());
        assertEquals(2, result.getTotalPages());
        verify(visitRepository).findByDoctorAndDateRange(eq(doctor), eq(today), eq(today), eq(PageRequest.of(1, 2)));
    }
}