package nbu.cscb869.services.data.dtos;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentViewDTO {
    private Long id;
    private String description;
    private Long visitId;
    private List<MedicineViewDTO> medicines;
}