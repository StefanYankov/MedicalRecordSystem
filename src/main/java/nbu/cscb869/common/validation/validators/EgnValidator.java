package nbu.cscb869.common.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import nbu.cscb869.common.validation.annotations.Egn;

import java.time.LocalDate;
import java.time.Year;

/**
 * Validator for Bulgarian EGN (Personal Number).
 * Validates length, numeric format, checksum, and date components (year, month, day).
 */
public class EgnValidator implements ConstraintValidator<Egn, String> {
    private static final int[] WEIGHTS = {2, 4, 8, 5, 10, 9, 7, 3, 6};

    @Override
    public boolean isValid(String egn, ConstraintValidatorContext context) {
        if (egn == null || egn.length() != 10 || !egn.matches("\\d{10}")) {
            return false;
        }

        try {
            int[] digits = egn.chars().map(c -> c - '0').toArray();

            // Validate date components
            if (!isValidDate(digits)) {
                return false;
            }

            // Validate checksum
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += digits[i] * WEIGHTS[i];
            }
            int checksum = sum % 11;
            if (checksum == 10) {
                checksum = 0;
            }

            return checksum == digits[9];
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidDate(int[] digits) {
        int yearPrefix = Year.now().getValue() / 100; // e.g., 20 for 2025
        int year = digits[0] * 10 + digits[1]; // YY from EGN
        int month = digits[2] * 10 + digits[3]; // MM from EGN
        int day = digits[4] * 10 + digits[5]; // DD from EGN

        // Determine full year based on month range
        int fullYear;
        if (month >= 1 && month <= 12) {
            // 1900–1999
            fullYear = year + (yearPrefix - 1) * 100; // e.g., 75 → 1975
        } else if (month >= 21 && month <= 32) {
            // 1800–1899
            fullYear = year + (yearPrefix - 2) * 100; // e.g., 75 → 1875
            month -= 20;
        } else if (month >= 41 && month <= 52) {
            // 2000–2099
            fullYear = year + yearPrefix * 100; // e.g., 25 → 2025
            month -= 40;
        } else {
            return false; // Invalid month range
        }

        // Validate month
        if (month < 1 || month > 12) {
            return false;
        }

        // Validate day
        try {
            LocalDate date = LocalDate.of(fullYear, month, day);
            return true; // If LocalDate creation succeeds, the date is valid
        } catch (Exception e) {
            return false; // Invalid day for the given month/year
        }
    }
}