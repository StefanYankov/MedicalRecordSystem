package nbu.cscb869.services.data.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorVisitCountReportDTO {
    private DoctorViewDTO doctor;
    private long visitCount;
}
