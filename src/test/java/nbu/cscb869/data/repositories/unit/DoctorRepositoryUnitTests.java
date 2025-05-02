package nbu.cscb869.data.repositories.unit;

import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.repositories.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DoctorRepositoryUnitTests {
    @Mock
    private DoctorRepository doctorRepository;

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Test");
        doctor.setUniqueIdNumber("12345");
        doctor.setGeneralPractitioner(true);
    }

    @Test
    void FindByUniqueIdNumber_ValidId_ReturnsDoctor() {
        when(doctorRepository.findByUniqueIdNumber("12345")).thenReturn(Optional.of(doctor));

        Optional<Doctor> result = doctorRepository.findByUniqueIdNumber("12345");

        assertThat(result).isPresent();
        assertThat(result.get().getUniqueIdNumber()).isEqualTo("12345");
    }

    @Test
    void FindByUniqueIdNumber_NonExistentId_ReturnsEmpty() {
        when(doctorRepository.findByUniqueIdNumber("99999")).thenReturn(Optional.empty());

        Optional<Doctor> result = doctorRepository.findByUniqueIdNumber("99999");

        assertThat(result).isEmpty();
    }

    @Test
    void FindByUniqueIdNumber_NullId_ThrowsIllegalArgumentException() {
        when(doctorRepository.findByUniqueIdNumber(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> doctorRepository.findByUniqueIdNumber(null));
    }

    @Test
    void FindByIsGeneralPractitionerTrue_ValidPageable_ReturnsPagedDoctors() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Doctor> page = new PageImpl<>(Collections.singletonList(doctor), pageable, 1);
        when(doctorRepository.findByIsGeneralPractitionerTrue(pageable)).thenReturn(page);

        Page<Doctor> result = doctorRepository.findByIsGeneralPractitionerTrue(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().isGeneralPractitioner()).isTrue();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByIsGeneralPractitionerTrue_NoGeneralPractitioners_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Doctor> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(doctorRepository.findByIsGeneralPractitionerTrue(pageable)).thenReturn(emptyPage);

        Page<Doctor> result = doctorRepository.findByIsGeneralPractitionerTrue(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByIsGeneralPractitionerTrue_NullPageable_ThrowsIllegalArgumentException() {
        when(doctorRepository.findByIsGeneralPractitionerTrue(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> doctorRepository.findByIsGeneralPractitionerTrue(null));
    }

    @Test
    void CountPatientsByGeneralPractitioner_ValidDoctor_ReturnsPatientCount() {
        when(doctorRepository.countPatientsByGeneralPractitioner(doctor)).thenReturn(5L);

        long result = doctorRepository.countPatientsByGeneralPractitioner(doctor);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void CountPatientsByGeneralPractitioner_NullDoctor_ThrowsIllegalArgumentException() {
        when(doctorRepository.countPatientsByGeneralPractitioner(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> doctorRepository.countPatientsByGeneralPractitioner(null));
    }

    @Test
    void CountPatientsByGeneralPractitioner_NoPatients_ReturnsZero() {
        when(doctorRepository.countPatientsByGeneralPractitioner(doctor)).thenReturn(0L);

        long result = doctorRepository.countPatientsByGeneralPractitioner(doctor);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void CountVisitsByDoctor_ValidDoctor_ReturnsVisitCount() {
        when(doctorRepository.countVisitsByDoctor(doctor)).thenReturn(10L);

        long result = doctorRepository.countVisitsByDoctor(doctor);

        assertThat(result).isEqualTo(10);
    }

    @Test
    void CountVisitsByDoctor_NullDoctor_ThrowsIllegalArgumentException() {
        when(doctorRepository.countVisitsByDoctor(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> doctorRepository.countVisitsByDoctor(null));
    }

    @Test
    void CountVisitsByDoctor_NoVisits_ReturnsZero() {
        when(doctorRepository.countVisitsByDoctor(doctor)).thenReturn(0L);

        long result = doctorRepository.countVisitsByDoctor(doctor);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void FindDoctorsBySickLeaveCount_ValidPageable_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Object[] resultRow = new Object[]{doctor, 15L};
        Page<Object[]> page = new PageImpl<>(Collections.singletonList(resultRow), pageable, 1);
        when(doctorRepository.findDoctorsBySickLeaveCount(pageable)).thenReturn(page);

        Page<Object[]> result = doctorRepository.findDoctorsBySickLeaveCount(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()[0]).isEqualTo(doctor);
        assertThat(result.getContent().getFirst()[1]).isEqualTo(15L);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindDoctorsBySickLeaveCount_NoSickLeaves_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(doctorRepository.findDoctorsBySickLeaveCount(pageable)).thenReturn(emptyPage);

        Page<Object[]> result = doctorRepository.findDoctorsBySickLeaveCount(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindDoctorsBySickLeaveCount_NullPageable_ThrowsIllegalArgumentException() {
        when(doctorRepository.findDoctorsBySickLeaveCount(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> doctorRepository.findDoctorsBySickLeaveCount(null));
    }
}