package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.data.base.BaseEntity;

@Getter
@Setter
@Builder
@Entity
@Table(name = "medicines", indexes = {
        @Index(columnList = "name")
})
@NoArgsConstructor
@AllArgsConstructor
public class Medicine extends BaseEntity {

    @NotBlank(message = ErrorMessages.NAME_NOT_BLANK)
    @Size(max = ValidationConfig.NAME_MAX_LENGTH)
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = ErrorMessages.DOSAGE_NOT_BLANK)
    @Size(max = ValidationConfig.DOSAGE_MAX_LENGTH)
    @Column
    private String dosage;

    @NotBlank(message = ErrorMessages.FREQUENCY_NOT_BLANK)
    @Size(max = ValidationConfig.FREQUENCY_MAX_LENGTH)
    @Column
    private String frequency;

    @ManyToOne
    @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;
}