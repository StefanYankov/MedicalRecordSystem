package nbu.cscb869.data.dto;

import lombok.Getter;
import nbu.cscb869.data.models.Diagnosis;

@Getter
public class DiagnosisVisitCountDTO {
    private final Diagnosis diagnosis;
    private final Long visitCount;

    public DiagnosisVisitCountDTO(Diagnosis diagnosis, Long visitCount) {
        this.diagnosis = diagnosis;
        this.visitCount = visitCount;
    }

}
