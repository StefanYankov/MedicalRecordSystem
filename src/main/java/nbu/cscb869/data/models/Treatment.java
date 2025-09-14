package nbu.cscb869.data.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.data.base.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@Entity
@Table(name = "treatments", indexes = {
        @Index(columnList = "visit_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class Treatment extends BaseEntity {

    @Size(max = ValidationConfig.DESCRIPTION_MAX_LENGTH)
    private String description;

    @OneToOne
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Medicine> medicines = new ArrayList<>();
}