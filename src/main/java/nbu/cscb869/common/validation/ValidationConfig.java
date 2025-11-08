package nbu.cscb869.common.validation;

import java.time.LocalTime;

public final class ValidationConfig {

    // Name validation
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 100;

    // Unique ID validation
    public static final int UNIQUE_ID_MIN_LENGTH = 5;
    public static final int UNIQUE_ID_MAX_LENGTH = 20;
    public static final String UNIQUE_ID_REGEX = "[a-zA-Z0-9]+";

    // Specialty validation
    public static final int SPECIALTY_NAME_MAX_LENGTH = 100;

    // Diagnosis validation
    public static final int DIAGNOSIS_NAME_MAX_LENGTH = 100;

    // Description validation
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    // Dosage and Frequency validation
    public static final int DOSAGE_MAX_LENGTH = 50;
    public static final int FREQUENCY_MAX_LENGTH = 50;

    // Sick Leave validation
    public static final int DURATION_MIN_DAYS = 1;

    // Visit validation
    public static final LocalTime VISIT_START_TIME = LocalTime.of(9, 0);
    public static final LocalTime VISIT_END_TIME = LocalTime.of(17, 0);

    private ValidationConfig() {
        throw new IllegalStateException("Utility class");
    }
}