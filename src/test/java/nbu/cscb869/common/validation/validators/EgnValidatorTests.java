package nbu.cscb869.common.validation.validators;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EgnValidatorTests {
    private EgnValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new EgnValidator();
    }

    // Happy Path
    @Test
    void isValid_ValidEgn1975_ReturnsTrue() {
        String egn = "7501017945"; // January 1, 1975, checksum=5
        assertTrue(validator.isValid(egn, context));
    }

    @Test
    void isValid_ValidEgn2000_ReturnsTrue() {
        String egn = "0041013188"; // January 1, 2000, MM=41, checksum=8
        assertTrue(validator.isValid(egn, context));
    }

    @Test
    void isValid_February29LeapYear_ReturnsTrue() {
        String egn = "8002294275"; // February 29, 1980, checksum=5
        assertTrue(validator.isValid(egn, context));
    }

    @Test
    void isValid_Egn1800_ReturnsTrue() {
        String egn = "0021012464"; // January 1, 1800, MM=21, checksum=4
        assertTrue(validator.isValid(egn, context));
    }

    // Error Cases
    @Test
    void isValid_NullEgn_ReturnsFalse() {
        assertFalse(validator.isValid(null, context));
    }

    @Test
    void isValid_WrongLengthEgn_ReturnsFalse() {
        String egn = "750101650"; // 9 digits
        assertFalse(validator.isValid(egn, context));
    }

    @Test
    void isValid_NonNumericEgn_ReturnsFalse() {
        String egn = "75010165AB"; // Contains letters
        assertFalse(validator.isValid(egn, context));
    }

    @Test
    void isValid_InvalidChecksum_ReturnsFalse() {
        String egn = "7501016509"; // Correct date, wrong checksum (should be 5)
        assertFalse(validator.isValid(egn, context));
    }

    @Test
    void isValid_InvalidDate_ReturnsFalse() {
        String egn = "7513016506"; // Invalid date: January 31, 1975 (MM=13)
        assertFalse(validator.isValid(egn, context));
    }

    // Edge Cases
    @Test
    void isValid_InvalidDay_ReturnsFalse() {
        String egn = "7501311238"; // Invalid day: January 31, 1975 (checksum fails)
        assertFalse(validator.isValid(egn, context));
    }

    @Test
    void isValid_February30_ReturnsFalse() {
        String egn = "7502301234"; // Invalid: February 30, 1975
        assertFalse(validator.isValid(egn, context));
    }

    @Test
    void isValid_InvalidMonthRange_ReturnsFalse() {
        String egn = "7515011234"; // Invalid month range: MM=15
        assertFalse(validator.isValid(egn, context));
    }
}