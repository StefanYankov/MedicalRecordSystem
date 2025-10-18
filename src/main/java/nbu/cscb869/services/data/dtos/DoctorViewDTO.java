package nbu.cscb869.services.data.dtos;

import lombok.*;

import java.util.Set;

@Data
public class DoctorViewDTO {
    private Long id;
    private String name;
    private String uniqueIdNumber;
    private Set<String> specialties;
    private boolean isGeneralPractitioner;
    private String imageUrl;
}