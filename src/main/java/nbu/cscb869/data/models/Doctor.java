package nbu.cscb869.data.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "doctor")
@SQLDelete(sql = "UPDATE doctor SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "is_deleted = false")
public class Doctor extends BaseEntity {
    private String name;
    private String uniqueIdNumber;
    private boolean isGeneralPractitioner;

    @ManyToMany
    @JoinTable(
            name = "doctor_specialty",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_id")
    )
    private Set<Specialty> specialties;

    @OneToMany(mappedBy = "generalPractitioner", cascade = CascadeType.ALL)
    private List<Patient> patients;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL)
    private List<Visit> visits;
}