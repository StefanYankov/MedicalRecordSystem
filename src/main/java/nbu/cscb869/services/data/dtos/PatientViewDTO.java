package nbu.cscb869.services.data.dtos;

import lombok.*;

import java.time.LocalDate;

@Data
public class PatientViewDTO {
    private Long id;
    private String name;
    private String egn;
    private LocalDate lastInsurancePaymentDate;
    private Long generalPractitionerId;
    private String generalPractitionerName;
    private String keycloakId;
}