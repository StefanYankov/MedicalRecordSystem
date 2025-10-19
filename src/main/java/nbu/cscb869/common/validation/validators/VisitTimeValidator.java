package nbu.cscb869.common.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import nbu.cscb869.common.exceptions.ExceptionMessages;
import nbu.cscb869.common.validation.ValidationConfig;
import nbu.cscb869.common.validation.annotations.ValidVisitTime;

import java.time.LocalTime;

public class VisitTimeValidator implements ConstraintValidator<ValidVisitTime, LocalTime> {

    @Override
    public boolean isValid(LocalTime visitTime, ConstraintValidatorContext context) {
        if (visitTime == null) {
            return true; // Let @NotNull handle this case
        }

        // Check for working hours and build a dynamic message if invalid
        if (visitTime.isBefore(ValidationConfig.VISIT_START_TIME) || visitTime.isAfter(ValidationConfig.VISIT_END_TIME)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                ExceptionMessages.formatVisitOutsideWorkingHours(
                    ValidationConfig.VISIT_START_TIME,
                    ValidationConfig.VISIT_END_TIME
                )
            ).addConstraintViolation();
            return false;
        }

        // Check for 30-minute slots. If this fails, the default message from the annotation will be used.
        int minutes = visitTime.getMinute();
        return minutes == 0 || minutes == 30;
    }
}