package nbu.cscb869.common.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import nbu.cscb869.common.validation.annotations.Egn;

import java.time.LocalDate;

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
        int year = digits[0] * 10 + digits[1]; // YY from EGN
        int month = digits[2] * 10 + digits[3]; // MM from EGN
        int day = digits[4] * 10 + digits[5]; // DD from EGN

        // Determine full year and adjust month based on month range
        int fullYear;
        int adjustedMonth;

        if (month >= 1 && month <= 12) {
            // 1900–1999
            fullYear = 1900 + year;
            adjustedMonth = month;
        } else if (month >= 21 && month <= 32) {
            // 1800–1899
            fullYear = 1800 + year;
            adjustedMonth = month - 20;
        } else if (month >= 41 && month <= 52) {
            // 2000–2099
            fullYear = 2000 + year;
            adjustedMonth = month - 40;
        } else {
            return false; // Invalid month range
        }

        // Validate adjusted month
        if (adjustedMonth < 1 || adjustedMonth > 12) {
            return false;
        }

        // Validate day (basic check before LocalDate)
        if (day < 1 || day > 31) {
            return false;
        }

        // Validate date with LocalDate
        try {
            LocalDate date = LocalDate.of(fullYear, adjustedMonth, day);
            return true; // Valid date
        } catch (Exception e) {
            return false; // Invalid date (e.g., February 30, non-leap year February 29)
        }
    }
}