package nbu.cscb869.services.data.dtos;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SickLeaveViewDTO {
    private Long id;

    private LocalDate startDate;

    private int durationDays;

    private Long visitId;
}