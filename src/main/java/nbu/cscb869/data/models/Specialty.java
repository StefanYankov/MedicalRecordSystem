package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.data.base.BaseEntity;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@Entity
@Table(name = "specialties", indexes = {
        @Index(columnList = "name")
})
@NoArgsConstructor
@AllArgsConstructor
public class Specialty extends BaseEntity {

    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(max = ValidationConfig.SPECIALTY_NAME_MAX_LENGTH)
    @Column(nullable = false, unique = true)
    private String name;

    @Size(max = ValidationConfig.DESCRIPTION_MAX_LENGTH)
    private String description;

    @ManyToMany(mappedBy = "specialties")
    private Set<Doctor> doctors = new HashSet<>();
}