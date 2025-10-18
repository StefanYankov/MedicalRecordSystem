package nbu.cscb869.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempt is made to delete an entity that is still
 * referenced by other entities in the database.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class EntityInUseException extends RuntimeException {
    public EntityInUseException(String message) {
        super(message);
    }
}
