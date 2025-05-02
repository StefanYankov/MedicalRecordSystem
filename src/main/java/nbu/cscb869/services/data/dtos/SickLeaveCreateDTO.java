package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

import java.time.LocalDate;

@Getter
@Setter
public class SickLeaveCreateDTO {
    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    @FutureOrPresent(message = ErrorMessages.DATE_FUTURE_OR_PRESENT)
    private LocalDate startDate;

    @NotNull(message = ErrorMessages.DURATION_NOT_NULL)
    @Min(value = ValidationConfig.DURATION_MIN_DAYS, message = ErrorMessages.DURATION_MIN)
    @Max(value = ValidationConfig.DURATION_MAX_DAYS, message = ErrorMessages.DURATION_MAX)
    private Integer durationDays;

    @NotNull(message = ErrorMessages.VISIT_ID_NOT_NULL)
    private Long visitId;
}