package nbu.cscb869.common.exceptions;

public final class ExceptionMessages {
    public static final String DOCTOR_NOT_FOUND_BY_ID = "Doctor not found with ID: %d";
    public static final String DOCTOR_NOT_FOUND_BY_UNIQUE_ID = "Doctor not found with unique ID: %s";
    public static final String SPECIALTY_NOT_FOUND_BY_NAME = "Specialty not found with name: %s";
    public static final String DOCTOR_UNIQUE_ID_EXISTS = "A doctor with unique ID %s already exists";
    public static final String INVALID_INPUT_NULL = "%s must not be null";
    public static final String INVALID_DATE_RANGE = "Start date must not be after end date";

    private ExceptionMessages() {
        throw new IllegalStateException("Utility class");
    }
}