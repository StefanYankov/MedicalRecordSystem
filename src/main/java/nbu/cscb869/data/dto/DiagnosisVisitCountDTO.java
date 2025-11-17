package nbu.cscb869.data.dto;

import lombok.Getter;

@Getter
public class DiagnosisVisitCountDTO {
    private final Long diagnosisId;
    private final String diagnosisName;
    private final long visitCount;

    public DiagnosisVisitCountDTO(Long diagnosisId, String diagnosisName, long visitCount) {
        this.diagnosisId = diagnosisId;
        this.diagnosisName = diagnosisName;
        this.visitCount = visitCount;
    }
}
