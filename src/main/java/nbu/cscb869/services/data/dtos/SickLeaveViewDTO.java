package nbu.cscb869.services.data.dtos;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SickLeaveViewDTO {
    private Long id;
    private LocalDate startDate;
    private Integer durationDays;
    private Long visitId;
}