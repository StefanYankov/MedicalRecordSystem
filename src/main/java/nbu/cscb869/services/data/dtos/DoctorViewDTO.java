package nbu.cscb869.services.data.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class DoctorViewDTO {
    private Long id;
    private String name;
    private String uniqueIdNumber;
    private boolean isGeneralPractitioner;
    private Set<String> specialtyNames;
}