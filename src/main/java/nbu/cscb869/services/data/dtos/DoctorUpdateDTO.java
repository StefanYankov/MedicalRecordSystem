package nbu.cscb869.services.data.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.common.validation.ValidationConfig;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoctorUpdateDTO {
    private Long id;

    @Size(min = ValidationConfig.NAME_MIN_LENGTH, max = ValidationConfig.NAME_MAX_LENGTH,
            message = ErrorMessages.NAME_SIZE)
    private String name;

    @Size(min = ValidationConfig.UNIQUE_ID_MIN_LENGTH, max = ValidationConfig.UNIQUE_ID_MAX_LENGTH)
    @Pattern(regexp = ValidationConfig.UNIQUE_ID_REGEX, message = ErrorMessages.UNIQUE_ID_PATTERN)
    private String uniqueIdNumber;

    private Set<String> specialties;
    private Boolean isGeneralPractitioner;
    private Boolean isApproved;
    private boolean deleteImage;
}
