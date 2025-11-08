package nbu.cscb869.services.data.dtos;

import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
public class DoctorViewDTO {
    private Long id;
    private String name;
    private String uniqueIdNumber;
    private String keycloakId;
    private Set<String> specialties = new HashSet<>();
    private boolean isGeneralPractitioner;
    private boolean isApproved;
    private String imageUrl;
}