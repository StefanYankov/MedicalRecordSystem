package nbu.cscb869.services.data.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineViewDTO {
    private Long id;

    private String name;

    private String dosage;

    private String frequency;

    private Long treatmentId;
}