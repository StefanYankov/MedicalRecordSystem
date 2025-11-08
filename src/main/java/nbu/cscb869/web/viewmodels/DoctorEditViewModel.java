package nbu.cscb869.web.viewmodels;

import lombok.Data;

import java.util.Set;

/**
 * A view model specifically for binding to the doctor edit form.
 * It contains all the necessary fields and ensures standard getter/setter conventions.
 */
@Data
public class DoctorEditViewModel {
    private Long id;
    private String name;
    private String uniqueIdNumber;
    private Set<String> specialties;
    private boolean generalPractitioner;
    private String imageUrl;
}
