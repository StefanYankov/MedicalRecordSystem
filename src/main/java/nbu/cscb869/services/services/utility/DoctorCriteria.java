package nbu.cscb869.services.services.utility;

/**
 * Utility class defining constants for doctor search criteria fields.
 */
public final class DoctorCriteria {
    public static final String NAME = "name";
    public static final String IS_GENERAL_PRACTITIONER = "isGeneralPractitioner";
    public static final String SPECIALTY_ID = "specialtyId";

    private DoctorCriteria() {
        throw new IllegalStateException("Utility class");
    }
}