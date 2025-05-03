package nbu.cscb869.services.data.dtos;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorViewDTO {
    private Long id;
    private String name;
    private String uniqueIdNumber;
    private Boolean isGeneralPractitioner;
    private List<String> specialtyNames;
    private String imageUrl;
}