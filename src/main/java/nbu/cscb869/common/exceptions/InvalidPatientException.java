package nbu.cscb869.common.exceptions;

public class InvalidPatientException extends RuntimeException{
    public InvalidPatientException() {
    }

    public InvalidPatientException(String message) {
        super(message);
    }

    public InvalidPatientException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPatientException(Throwable cause) {
        super(cause);
    }

    public InvalidPatientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
