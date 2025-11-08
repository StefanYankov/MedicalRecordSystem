package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VisitDocumentationDTO {
    // The ID of the visit being documented
    private Long visitId;

    // The ID of the selected diagnosis. Can be null if it's a wellness check-up.
    private Long diagnosisId;

    @NotBlank(message = "Visit notes cannot be empty.")
    private String notes;

    // Nested DTOs for treatment and sick leave
    private TreatmentCreateDTO treatment;
    private SickLeaveCreateDTO sickLeave;
}
