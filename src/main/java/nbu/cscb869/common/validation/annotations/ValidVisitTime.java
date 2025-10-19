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
    // This message now only applies if the 30-minute slot rule fails.
    // The working hours rule is handled dynamically inside the validator.
    String message() default "Visit time must be in 30-minute slots (e.g., 09:00, 09:30).";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}