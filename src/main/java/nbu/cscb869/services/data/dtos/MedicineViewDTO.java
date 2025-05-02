package nbu.cscb869.services.data.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicineViewDTO {
    private Long id;
    private String name;
    private String dosage;
    private String frequency;
    private String treatmentDetails;
}