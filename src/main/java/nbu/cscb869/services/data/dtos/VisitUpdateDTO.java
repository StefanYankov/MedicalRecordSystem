package nbu.cscb869.services.data.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.annotations.ValidVisitTime;
import nbu.cscb869.data.models.enums.VisitStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitUpdateDTO {
    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long id;

    @NotNull(message = ErrorMessages.DATE_NOT_NULL)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate visitDate;

    @NotNull(message = ErrorMessages.TIME_NOT_NULL)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @ValidVisitTime
    private LocalTime visitTime;

    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long patientId;

    @NotNull(message = ErrorMessages.ID_NOT_NULL)
    private Long doctorId;

    private Long diagnosisId;

    private String notes;

    private VisitStatus status;

    // FIX: Use UpdateDTOs to handle existing child entities
    private SickLeaveUpdateDTO sickLeave;
    private TreatmentUpdateDTO treatment;
}