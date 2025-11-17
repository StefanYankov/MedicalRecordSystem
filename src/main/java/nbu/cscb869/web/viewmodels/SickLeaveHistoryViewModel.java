package nbu.cscb869.web.viewmodels;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SickLeaveHistoryViewModel {
    private LocalDate startDate;
    private int durationDays;
    private String doctorName;
    private String diagnosisName;

    public LocalDate getEndDate() {
        if (startDate == null) {
            return null;
        }
        return startDate.plusDays(durationDays - 1);
    }
}
