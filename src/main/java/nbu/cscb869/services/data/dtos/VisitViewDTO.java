package nbu.cscb869.services.data.dtos;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitViewDTO {
    private Long id;
    private LocalDate visitDate;
    private Boolean sickLeaveIssued;
    private String patientName;
    private Long patientId;
    private String doctorName;
    private Long doctorId;
    private String diagnosisName;
    private Long diagnosisId;
    private SickLeaveViewDTO sickLeave;
    private TreatmentViewDTO treatment;
}