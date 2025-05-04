package nbu.cscb869.services.common.exceptions;

public class DoctorUniqueIdExistsException extends RuntimeException {
    public DoctorUniqueIdExistsException(String message) {
        super(message);
    }
}