package nbu.cscb869.data.repositories.unit;

import nbu.cscb869.data.models.Diagnosis;
import nbu.cscb869.data.models.Doctor;
import nbu.cscb869.data.models.Patient;
import nbu.cscb869.data.repositories.PatientRepository;
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

/**
 * Unit tests for {@link PatientRepository}, focusing on query methods and behavior.
 */
@ExtendWith(MockitoExtension.class)
class PatientRepositoryUnitTests {

    @Mock
    private PatientRepository patientRepository;

    private Patient patient;
    private Doctor gp;
    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setName("John Doe");
        patient.setEgn("1234567890");

        gp = new Doctor();
        gp.setId(2L);
        gp.setName("Dr. GP");
        patient.setGeneralPractitioner(gp);

        diagnosis = new Diagnosis();
        diagnosis.setId(3L);
        diagnosis.setName("Flu");
    }

    @Test
    void FindByEgn_ValidEgn_ReturnsPatient() {
        when(patientRepository.findByEgn("1234567890")).thenReturn(Optional.of(patient));

        Optional<Patient> result = patientRepository.findByEgn("1234567890");

        assertThat(result).isPresent();
        assertThat(result.get().getEgn()).isEqualTo("1234567890");
    }

    @Test
    void FindByEgn_NonExistentEgn_ReturnsEmpty() {
        when(patientRepository.findByEgn("9999999999")).thenReturn(Optional.empty());

        Optional<Patient> result = patientRepository.findByEgn("9999999999");

        assertThat(result).isEmpty();
    }

    @Test
    void FindByEgn_NullEgn_ThrowsIllegalArgumentException() {
        when(patientRepository.findByEgn(null)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> patientRepository.findByEgn(null));
    }

    @Test
    void FindByGeneralPractitionerAndIsDeletedFalse_ValidDoctor_ReturnsPagedPatients() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> page = new PageImpl<>(Collections.singletonList(patient), pageable, 1);
        when(patientRepository.findByGeneralPractitionerAndIsDeletedFalse(gp, pageable)).thenReturn(page);

        Page<Patient> result = patientRepository.findByGeneralPractitionerAndIsDeletedFalse(gp, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getGeneralPractitioner()).isEqualTo(gp);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByGeneralPractitionerAndIsDeletedFalse_NoPatients_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(patientRepository.findByGeneralPractitionerAndIsDeletedFalse(gp, pageable)).thenReturn(emptyPage);

        Page<Patient> result = patientRepository.findByGeneralPractitionerAndIsDeletedFalse(gp, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByGeneralPractitionerAndIsDeletedFalse_NullDoctor_ThrowsIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(patientRepository.findByGeneralPractitionerAndIsDeletedFalse(null, pageable))
                .thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class,
                () -> patientRepository.findByGeneralPractitionerAndIsDeletedFalse(null, pageable));
    }

    @Test
    void FindByDiagnosis_ValidDiagnosis_ReturnsPagedPatients() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> page = new PageImpl<>(Collections.singletonList(patient), pageable, 1);
        when(patientRepository.findByDiagnosis(diagnosis, pageable)).thenReturn(page);

        Page<Patient> result = patientRepository.findByDiagnosis(diagnosis, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void FindByDiagnosis_NoPatients_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(patientRepository.findByDiagnosis(diagnosis, pageable)).thenReturn(emptyPage);

        Page<Patient> result = patientRepository.findByDiagnosis(diagnosis, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void FindByDiagnosis_NullDiagnosis_ThrowsIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(patientRepository.findByDiagnosis(null, pageable)).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class,
                () -> patientRepository.findByDiagnosis(null, pageable));
    }

    @Test
    void FindByEgnWithValidInsurance_NullThresholdDate_ThrowsIllegalArgumentException() {
        when(patientRepository.findByEgnWithValidInsurance("1234567890"))
                .thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class,
                () -> patientRepository.findByEgnWithValidInsurance("1234567890"));
    }
}