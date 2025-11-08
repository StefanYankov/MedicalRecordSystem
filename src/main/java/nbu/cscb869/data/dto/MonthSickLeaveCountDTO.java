package nbu.cscb869.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthSickLeaveCountDTO {
    private Integer month;
    private Long sickLeaveCount;
}
