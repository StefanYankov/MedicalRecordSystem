package nbu.cscb869.web.viewmodels;

import lombok.Data;

import java.time.LocalDate;

/**
 * A view model specifically for displaying patient information in the admin list view.
 * It flattens the data structure to include the general practitioner's name directly.
 */
@Data
public class AdminPatientViewModel {
    private Long id;
    private String name;
    private String egn;
    private LocalDate lastInsurancePaymentDate;
    private String generalPractitionerName;
}
