package nbu.cscb869.data.dto;

import lombok.Getter;

@Getter
public class SickLeaveMonthCountDTO {
    private final Integer year;
    private final Integer month;
    private final Long sickLeaveCount;

    public SickLeaveMonthCountDTO(Integer year, Integer month, Long sickLeaveCount) {
        this.year = year;
        this.month = month;
        this.sickLeaveCount = sickLeaveCount;
    }

}
