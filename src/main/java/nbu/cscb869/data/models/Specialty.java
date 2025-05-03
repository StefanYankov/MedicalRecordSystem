package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "specialties", indexes = {
        @Index(columnList = "name"),
        @Index(columnList = "is_deleted")
})
@SQLDelete(sql = "UPDATE specialties SET is_deleted = true WHERE id = ?")
public class Specialty extends BaseEntity {
    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(max = ValidationConfig.SPECIALTY_NAME_MAX_LENGTH)
    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "specialties")
    private Set<Doctor> doctors = new HashSet<>();
}