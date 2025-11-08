package nbu.cscb869.services.data.dtos;

import lombok.Data;
import nbu.cscb869.data.models.enums.VisitStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class VisitViewDTO {
    private Long id;
    private LocalDate visitDate;
    private LocalTime visitTime;
    private String notes;
    private VisitStatus status;
    private PatientViewDTO patient;
    private DoctorViewDTO doctor;
    private DiagnosisViewDTO diagnosis;
    private SickLeaveViewDTO sickLeave;
    private TreatmentViewDTO treatment;
}
