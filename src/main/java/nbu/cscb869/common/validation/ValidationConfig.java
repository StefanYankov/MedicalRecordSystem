package nbu.cscb869.common.validation;

public class ValidationConfig {
    // Name lengths
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 100;
    public static final int SPECIALTY_NAME_MAX_LENGTH = 50;
    public static final int DIAGNOSIS_NAME_MAX_LENGTH = 100;

    // Unique ID number
    public static final int UNIQUE_ID_MIN_LENGTH = 5;
    public static final int UNIQUE_ID_MAX_LENGTH = 10;
    public static final String UNIQUE_ID_REGEX = "^[A-Za-z0-9]{" + UNIQUE_ID_MIN_LENGTH + "," + UNIQUE_ID_MAX_LENGTH + "}$";

    // Description
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    // Dosage and frequency
    public static final int DOSAGE_MAX_LENGTH = 50;
    public static final int FREQUENCY_MAX_LENGTH = 100;

    // Instructions
    public static final int INSTRUCTIONS_MAX_LENGTH = 500;

    // Sick leave duration
    public static final int DURATION_MIN_DAYS = 1;
    public static final int DURATION_MAX_DAYS = 30;

    // EGN
    public static final int EGN_LENGTH = 10;
    public static final String EGN_REGEX = "^\\d{" + EGN_LENGTH + "}$";

    private ValidationConfig() {
        throw new IllegalStateException("Utility class");
    }
}