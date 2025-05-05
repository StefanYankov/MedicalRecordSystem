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

@Getter
@Setter
@Entity
@Table(name = "medicines", indexes = @Index(columnList = "is_deleted"))
@SQLDelete(sql = "UPDATE medicines SET is_deleted = true, deleted_on = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
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