package nbu.cscb869.services.common.exceptions;

/**
 * Exception thrown when patient-specific validation fails, such as duplicate EGN
 * or invalid general practitioner.
 */
public class InvalidPatientException extends RuntimeException {

    public InvalidPatientException(String message) {
        super(message);
    }

    public InvalidPatientException(String message, Throwable cause) {
        super(message, cause);
    }
}