package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.annotations.ValidVisitTime;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class PatientVisitScheduleDTO {
    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long doctorId;

    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    private LocalDate visitDate;

    @NotNull(message = ErrorMessages.TIME_NOT_NULL)
    @ValidVisitTime
    private LocalTime visitTime;
}
