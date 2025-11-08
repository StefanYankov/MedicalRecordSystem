package nbu.cscb869.web.viewmodels;

import lombok.Data;
import nbu.cscb869.data.models.enums.VisitStatus;

import java.time.LocalDate;

/**
 * A lean ViewModel specifically for displaying a single row in the patient's medical history table.
 * It contains only the fields necessary for the view, decoupling the template from the service-layer DTOs.
 */
@Data
public class MedicalHistoryVisitViewModel {
    private Long visitId;
    private LocalDate visitDate;
    private String doctorName;
    private String diagnosisName;
    private VisitStatus status;
}
