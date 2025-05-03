package nbu.cscb869.data.dto;

import lombok.Builder;
import lombok.Getter;
import nbu.cscb869.data.models.Patient;

@Getter
@Builder
public class PatientDiagnosisDTO {
    private final Patient patient;
    private final String diagnosisName;
}