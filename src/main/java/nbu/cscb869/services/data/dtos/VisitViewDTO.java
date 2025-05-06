package nbu.cscb869.services.data.dtos;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitViewDTO {
    private Long id;

    private LocalDate visitDate;

    private LocalTime visitTime;

    private boolean sickLeaveIssued;

    private PatientViewDTO patient;

    private DoctorViewDTO doctor;

    private DiagnosisViewDTO diagnosis;

    private SickLeaveViewDTO sickLeave;

    private TreatmentViewDTO treatment;
}