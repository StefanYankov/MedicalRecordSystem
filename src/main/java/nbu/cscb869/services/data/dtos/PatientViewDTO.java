package nbu.cscb869.services.data.dtos;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientViewDTO {
    private Long id;
    private String name;
    private String egn;
    private String generalPractitionerName;
    private LocalDate lastInsurancePaymentDate;
}