package nbu.cscb869.web.viewmodels;

import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.services.data.dtos.DoctorViewDTO;
import nbu.cscb869.services.data.dtos.VisitViewDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * ViewModel for displaying patient history information to doctors.
 * Excludes sensitive information like keycloakId.
 */
@Getter
@Setter
public class PatientHistoryViewModel {
    private Long id;
    private String name;
    private String egn;
    private LocalDate lastInsurancePaymentDate;
    private DoctorViewDTO generalPractitioner;
    private List<VisitViewDTO> visits;
}
