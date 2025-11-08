package nbu.cscb869.common.validation;

import java.text.MessageFormat;

public class ErrorMessages {


    public static final String UNIQUE_ID_NOT_BLANK = "Unique ID number cannot be blank";
    public static final String DOCTOR_UNIQUE_ID_INVALID_FORMAT = "Unique ID must be between 5 and 20 alphanumeric characters.";
    public static final String UNIQUE_ID_PATTERN = "Unique ID number must be {0}-{1} alphanumeric characters";
    public static final String EGN_NOT_BLANK = "EGN cannot be blank";
    public static final String EGN_INVALID = "Invalid EGN format or checksum";
    public static final String DATE_NOT_NULL = "{0} cannot be null";
    public static final String TIME_NOT_NULL = "Time cannot be null";
    public static final String DATE_PAST_OR_PRESENT = "{0} must be in the past or present";
    public static final String DATE_FUTURE_OR_PRESENT = "{0} must be today or in the future";
    public static final String SPECIALTIES_NOT_EMPTY = "Specialties cannot be empty";
    public static final String GP_NOT_NULL = "General practitioner status cannot be null";
    public static final String GP_ID_NOT_BLANK = "General practitioner unique ID number cannot be blank";
    public static final String ID_NOT_NULL = "ID cannot be null";
    public static final String DIAGNOSIS_NAME_NOT_BLANK = "Diagnosis name cannot be blank";
    public static final String DESCRIPTION_SIZE = "Description cannot exceed {0} characters";
    public static final String DESCRIPTION_NOT_BLANK = "Description cannot be blank";
    public static final String PATIENT_EGN_NOT_BLANK = "Patient EGN cannot be blank";
    public static final String DOCTOR_ID_NOT_BLANK = "Doctor unique ID number cannot be blank";
    public static final String SICK_LEAVE_NOT_NULL = "Sick leave issued status cannot be null";
    public static final String DURATION_NOT_NULL = "Duration days cannot be null";
    public static final String DURATION_MIN = "Duration must be at least {0} day";
    public static final String DURATION_MAX = "Duration cannot exceed {0} days";
    public static final String VISIT_ID_NOT_NULL = "Visit ID cannot be null";
    public static final String INSTRUCTIONS_NOT_BLANK = "Instructions cannot be blank";
    public static final String INSTRUCTIONS_SIZE = "Instructions cannot exceed {0} characters";
    public static final String DOSAGE_NOT_BLANK = "Dosage cannot be blank";
    public static final String DOSAGE_SIZE = "Dosage cannot exceed {0} characters";
    public static final String FREQUENCY_NOT_BLANK = "Frequency cannot be blank";
    public static final String FREQUENCY_SIZE = "Frequency cannot exceed {0} characters";
    public static final String TREATMENT_ID_NOT_NULL = "Treatment ID cannot be null";
    public static final String KEYCLOAK_ID_NOT_BLANK = "Keycloak ID cannot be blank";


    public static final String ENTITY_TYPE_NOT_BLANK = "Entity type cannot be blank";
    public static final String ENTITY_ID_NOT_NULL = "Entity ID cannot be null";
    public static final String ROLE_NOT_NULL = "Role cannot be null";


    // Roles
    public static final String ROLE_PATIENT = "PATIENT";
    public static final String ROLE_DOCTOR = "DOCTOR";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_NAME_NOT_BLANK = "Role name cannot be blank";

    // Error messages
    public static final String USER_NOT_FOUND = "User not found with ID: {0}";
    public static final String USER_NOT_NULL = "User cannot be null";
    public static final String USER_ALREADY_EXISTS = "User already exists with email: {0}";
    public static final String INVALID_ROLE = "Invalid role: {0}";
    public static final String AUTHENTICATION_FAILED = "Authentication failed: {0}";

    // Validation messages
    public static final String EMAIL_NOT_BLANK = "Email cannot be blank";
    public static final String EMAIL_INVALID = "Invalid email format";
    public static final String EMAIL_SIZE = "Email must be between {0} and {1} characters";
    public static final String PASSWORD_NOT_BLANK = "Password cannot be blank";
    public static final String PASSWORD_SIZE = "Password must be between {6} and {100} characters";
    public static final String PASSWORD_PATTERN = "Password must contain at least one uppercase, one lowercase, and one digit";
    public static final String NAME_NOT_BLANK = "Name cannot be blank";
    public static final String NAME_NOT_NULL = "Name must not be null";
    public static final String NAME_SIZE = "Name must be between {0} and {1} characters";

    // Keycloak constants
    public static final String KEYCLOAK_REALM = "medical-realm";

    // New image processing error messages
    public static final String IMAGE_FILE_NULL_OR_EMPTY = "Image file must not be null or empty";
    public static final String IMAGE_FILE_SIZE_EXCEEDED = "Image file must be less than {0}MB";
    public static final String IMAGE_FILE_INVALID_TYPE = "File must be an image";
    public static final String IMAGE_UPLOAD_FAILED = "Failed to upload image: {0}";
    public static final String IMAGE_DELETE_FAILED = "Failed to delete image: {0}";


    // Utility method for formatting parameterized messages
    public static String format(String message, Object... arguments) {
        return MessageFormat.format(message, arguments);
    }

    private ErrorMessages() {
        throw new IllegalStateException("Utility class");
    }
}