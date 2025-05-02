package nbu.cscb869.services.data.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class VisitViewDTO {
    private Long id;
    private LocalDate visitDate;
    private boolean sickLeaveIssued;
    private String patientName;
    private String doctorName;
    private String diagnosisName;
}