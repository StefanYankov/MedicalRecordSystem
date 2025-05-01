package nbu.cscb869.common.validation.validators;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


public class EgnValidatorTests {
    private EgnValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new EgnValidator();
        context = Mockito.mock(ConstraintValidatorContext.class);
    }

    // Happy Path
    @Test
    void isValid_ValidEgn1975_ReturnsTrue() {
        String validEgn = "7501010018"; // 01-Jan-1975, valid checksum
        assertTrue(validator.isValid(validEgn, context));
    }

    @Test
    void isValid_ValidEgn2000_ReturnsTrue() {
        String validEgn = "0041010010"; // 01-Jan-2000 (month 41), valid checksum
        assertTrue(validator.isValid(validEgn, context));
    }

    // Error Cases
    @Test
    void isValid_InvalidLength_ThrowsFalse() {
        String invalidEgn = "123456789"; // 9 digits
        assertFalse(validator.isValid(invalidEgn, context));
    }

    @Test
    void isValid_NonNumeric_ThrowsFalse() {
        String invalidEgn = "750101001a"; // Contains letter
        assertFalse(validator.isValid(invalidEgn, context));
    }

    @Test
    void isValid_InvalidChecksum_ThrowsFalse() {
        String invalidEgn = "7501010019"; // Wrong checksum
        assertFalse(validator.isValid(invalidEgn, context));
    }

    @Test
    void isValid_InvalidMonth_ThrowsFalse() {
        String invalidEgn = "7513010014"; // Invalid month 13
        assertFalse(validator.isValid(invalidEgn, context));
    }

    @Test
    void isValid_InvalidDay_ThrowsFalse() {
        String invalidEgn = "7501310012"; // Invalid day 31 for February
        assertFalse(validator.isValid(invalidEgn, context));
    }

    // Edge Cases
    @Test
    void isValid_NullEgn_ThrowsFalse() {
        assertFalse(validator.isValid(null, context));
    }

    @Test
    void isValid_February29NonLeapYear_ThrowsFalse() {
        String invalidEgn = "7502290016"; // 29-Feb-1975 (not a leap year)
        assertFalse(validator.isValid(invalidEgn, context));
    }

    @Test
    void isValid_February29LeapYear_ReturnsTrue() {
        String validEgn = "7602290014"; // 29-Feb-1976 (leap year), valid checksum
        assertTrue(validator.isValid(validEgn, context));
    }

    @Test
    void isValid_Egn1800_ReturnsTrue() {
        String validEgn = "7521010010"; // 01-Jan-1875 (month 21), valid checksum
        assertTrue(validator.isValid(validEgn, context));
    }
}