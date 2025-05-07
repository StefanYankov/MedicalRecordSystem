package nbu.cscb869.common.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import nbu.cscb869.common.validation.validators.VisitTimeValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = VisitTimeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVisitTime {
    String message() default "Visit time must be between 9:00 and 17:00 in 30-minute slots";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}