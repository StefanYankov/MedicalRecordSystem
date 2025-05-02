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
public class VisitUpdateDTO {
    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long id;

    @FutureOrPresent(message = ErrorMessages.DATE_FUTURE_OR_PRESENT)
    private LocalDate visitDate;

    private Boolean sickLeaveIssued;

    @Egn(message = ErrorMessages.EGN_INVALID)
    private String patientEgn;

    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX,
            message = ErrorMessages.UNIQUE_ID_PATTERN)
    private String doctorUniqueIdNumber;

    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.DIAGNOSIS_NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String diagnosisName;
}