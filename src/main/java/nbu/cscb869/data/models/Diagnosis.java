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
@Table(name = "diagnoses", indexes = {
        @Index(columnList = "name"),
        @Index(columnList = "is_deleted")
})
@SQLDelete(sql = "UPDATE diagnoses SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
public class Diagnosis extends BaseEntity {
    @NotBlank(message = ErrorMessages.DIAGNOSIS_NAME_NOT_BLANK)
    @Size(max = ValidationConfig.DIAGNOSIS_NAME_MAX_LENGTH)
    @Column(nullable = false, unique = true)
    private String name;

    @Size(max = ValidationConfig.DESCRIPTION_MAX_LENGTH)
    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "diagnosis")
    private Set<Visit> visits = new HashSet<>();
}