package nbu.cscb869.common.exceptions;

import java.text.MessageFormat;
import java.time.LocalDate;

public final class ExceptionMessages {
    public static final String DOCTOR_NOT_FOUND_BY_ID = "Doctor not found with ID: {0}";
    public static final String DOCTOR_NOT_FOUND_BY_UNIQUE_ID = "Doctor not found with unique ID: {0}";
    public static final String SPECIALTY_NOT_FOUND_BY_NAME = "Specialty not found with name: {0}";
    public static final String DOCTOR_UNIQUE_ID_EXISTS = "A doctor with unique ID {0} already exists";
    public static final String INVALID_DTO_NULL = "{0} must not be null";
    public static final String INVALID_FIELD_NULL = "{0} must not be null";
    public static final String INVALID_FIELD_EMPTY = "{0} must not be empty";
    public static final String INVALID_DATE_RANGE = "Start date {0} must not be after end date {1}";
    public static final String DOCTOR_HAS_ACTIVE_PATIENTS = "Cannot delete doctor who is a general practitioner for active patients";
    public static final String FAILED_TO_CREATE_DOCTOR = "Failed to create doctor: {0}";
    public static final String FAILED_TO_UPDATE_DOCTOR = "Failed to update doctor with ID: {0}";
    public static final String FAILED_TO_DELETE_DOCTOR = "Failed to delete doctor with ID: {0}";
    public static final String FAILED_TO_RETRIEVE_DOCTOR = "Failed to retrieve doctor: {0}";
    public static final String FAILED_TO_RETRIEVE_DOCTORS = "Failed to retrieve doctors: {0}";
    public static final String FAILED_TO_RETRIEVE_PATIENTS = "Failed to retrieve patients for doctor with ID: {0}";
    public static final String FAILED_TO_COUNT_PATIENTS = "Failed to count patients for doctor with ID: {0}";
    public static final String FAILED_TO_COUNT_VISITS = "Failed to count visits for doctor with ID: {0}";
    public static final String FAILED_TO_RETRIEVE_VISITS = "Failed to retrieve visits for doctor with ID: {0} from {1} to {2}";
    public static final String FAILED_TO_RETRIEVE_SICK_LEAVE_COUNTS = "Failed to retrieve doctors with most sick leaves: {0}";

    public static final String PATIENT_EGN_EXISTS = "A patient with EGN {0} already exists";
    public static final String INVALID_GENERAL_PRACTITIONER = "Selected doctor with ID {0} is not a general practitioner";
    public static final String PATIENT_NOT_FOUND_BY_ID = "Patient not found with ID: {0}";
    public static final String PATIENT_NOT_FOUND_BY_EGN = "Patient not found with EGN: {0}";

    private ExceptionMessages() {
        throw new IllegalStateException("Utility class");
    }

    // Helper methods for MessageFormat

    public static String formatDoctorNotFoundById(Long id) {
        return MessageFormat.format(DOCTOR_NOT_FOUND_BY_ID, id);
    }

    public static String formatInvalidDTONull(String dtoName) {
        return MessageFormat.format(INVALID_DTO_NULL, dtoName);
    }

    public static String formatInvalidFieldNull(String fieldName) {
        return MessageFormat.format(INVALID_FIELD_NULL, fieldName);
    }

    public static String formatInvalidFieldEmpty(String fieldName) {
        return MessageFormat.format(INVALID_FIELD_EMPTY, fieldName);
    }

    public static String formatInvalidDateRange(LocalDate startDate, LocalDate endDate) {
        return MessageFormat.format(INVALID_DATE_RANGE, startDate, endDate);
    }

    public static String formatFailedToCreateDoctor(String cause) {
        return MessageFormat.format(FAILED_TO_CREATE_DOCTOR, cause);
    }

    public static String formatFailedToUpdateDoctor(Long id) {
        return MessageFormat.format(FAILED_TO_UPDATE_DOCTOR, id);
    }

    public static String formatFailedToDeleteDoctor(Long id) {
        return MessageFormat.format(FAILED_TO_DELETE_DOCTOR, id);
    }

    public static String formatFailedToRetrieveDoctor(String identifier) {
        return MessageFormat.format(FAILED_TO_RETRIEVE_DOCTOR, identifier);
    }

    public static String formatFailedToRetrieveDoctors(String cause) {
        return MessageFormat.format(FAILED_TO_RETRIEVE_DOCTORS, cause);
    }

    public static String formatFailedToRetrievePatients(Long doctorId) {
        return MessageFormat.format(FAILED_TO_RETRIEVE_PATIENTS, doctorId);
    }

    public static String formatFailedToCountPatients(Long doctorId) {
        return MessageFormat.format(FAILED_TO_COUNT_PATIENTS, doctorId);
    }

    public static String formatFailedToCountVisits(Long doctorId) {
        return MessageFormat.format(FAILED_TO_COUNT_VISITS, doctorId);
    }

    public static String formatFailedToRetrieveVisits(Long doctorId, LocalDate startDate, LocalDate endDate) {
        return MessageFormat.format(FAILED_TO_RETRIEVE_VISITS, doctorId, startDate, endDate);
    }

    public static String formatFailedToRetrieveSickLeaveCounts(String cause) {
        return MessageFormat.format(FAILED_TO_RETRIEVE_SICK_LEAVE_COUNTS, cause);
    }

    public static String formatPatientEgnExists(String egn) {
        return MessageFormat.format(PATIENT_EGN_EXISTS, egn);
    }

    public static String formatInvalidGeneralPractitioner(Long id) {
        return MessageFormat.format(INVALID_GENERAL_PRACTITIONER, id);
    }

    public static String formatPatientNotFoundById(Long id) {
        return MessageFormat.format(PATIENT_NOT_FOUND_BY_ID, id);
    }

    public static String formatPatientNotFoundByEgn(String egn) {
        return MessageFormat.format(PATIENT_NOT_FOUND_BY_EGN, egn);
    }
}