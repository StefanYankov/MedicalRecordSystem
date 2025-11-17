package nbu.cscb869.services.data.dtos;

import lombok.Data;

@Data
public class VisitDocumentationDTO {
    private Long visitId;
    private Long diagnosisId;
    private String notes;
    private String status;

    private TreatmentUpdateDTO treatment;
    private SickLeaveUpdateDTO sickLeave;
}
