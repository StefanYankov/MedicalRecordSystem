package nbu.cscb869.data.dto;

import lombok.Builder;
import lombok.Getter;
import nbu.cscb869.data.models.Doctor;

@Getter
@Builder
public class DoctorSickLeaveCountDTO {
    private final Doctor doctor;
    private final long sickLeaveCount;
}