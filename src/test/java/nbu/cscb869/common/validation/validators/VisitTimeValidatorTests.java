package nbu.cscb869.common.validation.validators;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VisitTimeValidatorTests {
    private VisitTimeValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new VisitTimeValidator();
    }

    // Happy Path
    @Test
    void IsValid_StartOfDay_ReturnsTrue() {
        LocalTime time = LocalTime.of(9, 0);
        assertTrue(validator.isValid(time, context));
    }

    @Test
    void IsValid_HalfHourSlot_ReturnsTrue() {
        LocalTime time = LocalTime.of(14, 30);
        assertTrue(validator.isValid(time, context));
    }

    @Test
    void IsValid_EndOfDay_ReturnsTrue() {
        LocalTime time = LocalTime.of(17, 0);
        assertTrue(validator.isValid(time, context));
    }

    // Error Cases
    @Test
    void IsValid_NullTime_ReturnsFalse() {
        assertFalse(validator.isValid(null, context));
    }

    @Test
    void IsValid_BeforeStartTime_ReturnsFalse() {
        LocalTime time = LocalTime.of(8, 59);
        assertFalse(validator.isValid(time, context));
    }

    @Test
    void IsValid_AfterEndTime_ReturnsFalse() {
        LocalTime time = LocalTime.of(17, 1);
        assertFalse(validator.isValid(time, context));
    }

    @Test
    void IsValid_NonHalfHourSlot_ReturnsFalse() {
        LocalTime time = LocalTime.of(9, 15);
        assertFalse(validator.isValid(time, context));
    }

    // Edge Cases
    @Test
    void IsValid_Midnight_ReturnsFalse() {
        LocalTime time = LocalTime.of(0, 0);
        assertFalse(validator.isValid(time, context));
    }

    @Test
    void IsValid_OneMinuteBeforeHalfHour_ReturnsFalse() {
        LocalTime time = LocalTime.of(9, 29);
        assertFalse(validator.isValid(time, context));
    }

    @Test
    void IsValid_OneMinuteAfterHalfHour_ReturnsFalse() {
        LocalTime time = LocalTime.of(9, 31);
        assertFalse(validator.isValid(time, context));
    }
}