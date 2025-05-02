package nbu.cscb869.services.data.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TreatmentViewDTO {
    private Long id;
    private String instructions;
    private String visitDetails;
    private List<String> medicineNames;
}