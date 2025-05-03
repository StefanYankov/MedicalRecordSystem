package nbu.cscb869.data.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class YearMonthSickLeaveCountDTO {
    private final int year;
    private final int month;
    private final long count;
}