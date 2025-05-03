package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitCreateDTO {
    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    private LocalDate visitDate;

    @NotNull(message = ErrorMessages.SICK_LEAVE_NOT_NULL)
    private Boolean sickLeaveIssued;

    @NotNull(message = ErrorMessages.PATIENT_EGN_NOT_BLANK)
    private Long patientId;

    @NotNull(message = ErrorMessages.DOCTOR_ID_NOT_BLANK)
    private Long doctorId;

    @NotNull(message = ErrorMessages.DIAGNOSIS_NAME_NOT_BLANK)
    private Long diagnosisId;
}