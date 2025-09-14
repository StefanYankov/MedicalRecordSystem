package nbu.cscb869.data.utils;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

public class TestDataUtils {
    private static final int[] EGN_WEIGHTS = {2, 4, 8, 5, 10, 9, 7, 3, 6};
    private static final Random RANDOM = new Random();

    public static String generateValidEgn() {
        int year = 2000 + RANDOM.nextInt(26);
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28);
        LocalDate date = LocalDate.of(year, month, day);

        int egnMonth = month + 40;
        String yy = String.format("%02d", year % 100);
        String mm = String.format("%02d", egnMonth);
        String dd = String.format("%02d", day);

        String region = String.format("%03d", RANDOM.nextInt(1000));

        String baseEgn = yy + mm + dd + region;
        int[] digits = baseEgn.chars().map(c -> c - '0').toArray();
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += digits[i] * EGN_WEIGHTS[i];
        }
        int checksum = sum % 11;
        if (checksum == 10) {
            checksum = 0;
        }

        return baseEgn + checksum;
    }

    public static String generateUniqueIdNumber() {
        return "DOC" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5);
    }

    public static String generateEmail() {
        return "user" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    public static String generateKeycloakId() {
        return UUID.randomUUID().toString();
    }
}