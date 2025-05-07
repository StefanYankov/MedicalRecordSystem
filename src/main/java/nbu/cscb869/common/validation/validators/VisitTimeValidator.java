package nbu.cscb869.common.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import nbu.cscb869.common.validation.annotations.ValidVisitTime;

import java.time.LocalTime;

public class VisitTimeValidator implements ConstraintValidator<ValidVisitTime, LocalTime> {
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(17, 0);

    @Override
    public boolean isValid(LocalTime visitTime, ConstraintValidatorContext context) {
        if (visitTime == null) {
            return false;
        }
        if (visitTime.isBefore(START_TIME) || visitTime.isAfter(END_TIME)) {
            return false;
        }
        int minutes = visitTime.getMinute();
        return minutes == 0 || minutes == 30;
    }
}