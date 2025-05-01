package nbu.cscb869.common.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import nbu.cscb869.common.validation.validators.EgnValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to validate Bulgarian EGN (Personal Number).
 */
@Constraint(validatedBy = EgnValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Egn {
    String message() default "Invalid EGN format or checksum";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}