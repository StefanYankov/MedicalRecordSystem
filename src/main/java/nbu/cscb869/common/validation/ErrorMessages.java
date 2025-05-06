package nbu.cscb869.common.validation;

public class ErrorMessages {
    public static final String NAME_NOT_BLANK = "Name cannot be blank";
    public static final String NAME_NOT_NULL = "Name must not be null";
    public static final String NAME_SIZE = "Name must be between %d and %d characters";
    public static final String UNIQUE_ID_NOT_BLANK = "Unique ID number cannot be blank";
    public static final String UNIQUE_ID_PATTERN = "Unique ID number must be %d-%d alphanumeric characters";
    public static final String EGN_NOT_BLANK = "EGN cannot be blank";
    public static final String EGN_INVALID = "Invalid EGN format or checksum";
    public static final String DATE_NOT_NULL = "%s cannot be null";
    public static final String TIME_NOT_NULL = "Time cannot be null";
    public static final String DATE_PAST_OR_PRESENT = "%s must be in the past or present";
    public static final String DATE_FUTURE_OR_PRESENT = "%s must be today or in the future";
    public static final String SPECIALTIES_NOT_EMPTY = "Specialties cannot be empty";
    public static final String GP_NOT_NULL = "General practitioner status cannot be null";
    public static final String GP_ID_NOT_BLANK = "General practitioner unique ID number cannot be blank";
    public static final String ID_NOT_NULL = "ID cannot be null";
    public static final String DIAGNOSIS_NAME_NOT_BLANK = "Diagnosis name cannot be blank";
    public static final String DESCRIPTION_SIZE = "Description cannot exceed %d characters";
    public static final String DESCRIPTION_NOT_BLANK = "Description cannot be blank";
    public static final String PATIENT_EGN_NOT_BLANK = "Patient EGN cannot be blank";
    public static final String DOCTOR_ID_NOT_BLANK = "Doctor unique ID number cannot be blank";
    public static final String SICK_LEAVE_NOT_NULL = "Sick leave issued status cannot be null";
    public static final String DURATION_NOT_NULL = "Duration days cannot be null";
    public static final String DURATION_MIN = "Duration must be at least %d day";
    public static final String DURATION_MAX = "Duration cannot exceed %d days";
    public static final String VISIT_ID_NOT_NULL = "Visit ID cannot be null";
    public static final String INSTRUCTIONS_NOT_BLANK = "Instructions cannot be blank";
    public static final String INSTRUCTIONS_SIZE = "Instructions cannot exceed %d characters";
    public static final String DOSAGE_NOT_BLANK = "Dosage cannot be blank";
    public static final String DOSAGE_SIZE = "Dosage cannot exceed %d characters";
    public static final String FREQUENCY_NOT_BLANK = "Frequency cannot be blank";
    public static final String FREQUENCY_SIZE = "Frequency cannot exceed %d characters";
    public static final String TREATMENT_ID_NOT_NULL = "Treatment ID cannot be null";

    private ErrorMessages() {
        throw new IllegalStateException("Utility class");
    }
}