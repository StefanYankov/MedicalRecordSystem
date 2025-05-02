package nbu.cscb869.services.data.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PatientViewDTO {
    private Long id;
    private String name;
    private String egn;
    private LocalDate lastInsurancePaymentDate;
    private String generalPractitionerName;
}