package nbu.cscb869.services.data.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SickLeaveViewDTO {
    private Long id;
    private LocalDate startDate;
    private int durationDays;
    private String visitDetails;
}