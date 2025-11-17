package nbu.cscb869.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nbu.cscb869.data.models.Doctor;

@Getter
@Builder
@AllArgsConstructor
public class DoctorVisitCountDTO {
    private final Doctor doctor;
    private final long visitCount;
}
