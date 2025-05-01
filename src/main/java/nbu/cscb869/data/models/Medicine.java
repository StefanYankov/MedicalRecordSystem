package nbu.cscb869.data.models;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import nbu.cscb869.data.models.base.BaseEntity;

/**
 * Represents a medication prescribed as part of a treatment.
 */
@Getter
@Setter
@Entity
@Table(name = "medicines")
public class Medicine extends BaseEntity {
    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column
    private String dosage; // e.g., "200mg"

    @Column
    private String frequency; // e.g., "Twice daily"

    @ManyToOne
    @JoinColumn(name = "treatment_id")
    private Treatment treatment;
}