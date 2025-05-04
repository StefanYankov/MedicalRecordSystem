package nbu.cscb869.services.data.dtos;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorViewDTO {
    private Long id;

    private String name;

    private String uniqueIdNumber;

    private boolean isGeneralPractitioner;

    private String imageUrl;

    private Set<SpecialtyViewDTO> specialties;
}