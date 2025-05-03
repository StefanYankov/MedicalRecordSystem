package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a doctor with soft delete and indexing on {@code is_deleted}.
 */
@Getter
@Setter
@Entity
@Table(name = "doctors", indexes = {
        @Index(columnList = "uniqueIdNumber"),
        @Index(columnList = "is_deleted")
})
@SQLDelete(sql = "UPDATE doctors SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
public class Doctor extends BaseEntity {
    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH)
    @Column(nullable = false)
    private String name;

    @NotBlank(message = ErrorMessages.UNIQUE_ID_NOT_BLANK)
    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX, message = ErrorMessages.UNIQUE_ID_PATTERN)
    @Column(nullable = false, unique = true)
    private String uniqueIdNumber;

    @Column(name = "is_general_practitioner", nullable = false)
    private boolean isGeneralPractitioner;

    @ManyToMany
    @JoinTable(
            name = "doctor_specialties",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_id")
    )
    private Set<Specialty> specialties = new HashSet<>();

    @OneToMany(mappedBy = "generalPractitioner")
    private List<Patient> patients = new ArrayList<>();

    @OneToMany(mappedBy = "doctor")
    private List<Visit> visits = new ArrayList<>();

    // TODO: need to rework it if I am going to use cloudinary
    @Column
    private String imageUrl;
}