package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.annotations.ValidVisitTime;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class VisitCreateDTO {
    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    private LocalDate visitDate;

    @NotNull(message = ErrorMessages.TIME_NOT_NULL)
    @ValidVisitTime
    private LocalTime visitTime;

    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long patientId;

    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long doctorId;

    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long diagnosisId;

    private SickLeaveCreateDTO sickLeave;
    private TreatmentCreateDTO treatment;
}