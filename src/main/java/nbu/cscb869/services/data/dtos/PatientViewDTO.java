package nbu.cscb869.services.data.dtos;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientViewDTO {
    private Long id;

    private String name;

    private String egn;

    private LocalDate lastInsurancePaymentDate;

    private DoctorViewDTO generalPractitioner;
}