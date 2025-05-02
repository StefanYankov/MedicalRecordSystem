package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.common.validation.annotations.Egn;

import java.time.LocalDate;

@Getter
@Setter
public class VisitCreateDTO {
    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    @FutureOrPresent(message = ErrorMessages.DATE_FUTURE_OR_PRESENT)
    private LocalDate visitDate;

    @NotNull(message = ErrorMessages.SICK_LEAVE_NOT_NULL)
    private Boolean sickLeaveIssued;

    @NotBlank(message = ErrorMessages.PATIENT_EGN_NOT_BLANK)
    @Egn(message = ErrorMessages.EGN_INVALID)
    private String patientEgn;

    @NotBlank(message = ErrorMessages.DOCTOR_ID_NOT_BLANK)
    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX,
            message = ErrorMessages.UNIQUE_ID_PATTERN)
    private String doctorUniqueIdNumber;

    @NotBlank(message = ErrorMessages.DIAGNOSIS_NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.DIAGNOSIS_NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String diagnosisName;
}