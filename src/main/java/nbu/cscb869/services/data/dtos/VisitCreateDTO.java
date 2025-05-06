package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitCreateDTO {
    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    private LocalDate visitDate;

    @NotNull(message = ErrorMessages.TIME_NOT_NULL)
    private LocalTime visitTime;

    @NotNull(message = ErrorMessages.SICK_LEAVE_NOT_NULL)
    private boolean sickLeaveIssued;

    @NotNull
    private Long patientId;

    @NotNull
    private Long doctorId;

    @NotNull
    private Long diagnosisId;
}