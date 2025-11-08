package nbu.cscb869.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class PatientInsuranceException extends RuntimeException {
    public PatientInsuranceException(String message) {
        super(message);
    }
}
