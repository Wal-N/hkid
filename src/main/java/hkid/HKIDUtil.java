package hkid;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for generating complete HKID card data.
 */
public final class HKIDUtil {
    private HKIDUtil() {
        throw new AssertionError("HKIDUtil cannot be instantiated");
    }

    /**
     * Generates a complete HKID object with sample personal information.
     */
    public static HKID genRandomHkid() {
        HKID hkid = new HKID();
        // Keep sample card dates in a realistic order: birth, then registration, then issue month.
        LocalDate dateOfBirth = generateRandomDateInRange(LocalDate.of(1900, 1, 1), LocalDate.now().minusYears(11));
        LocalDate earliestRegistrationDate = laterDate(LocalDate.of(2003, 6, 23), dateOfBirth.plusYears(11));
        LocalDate dateOfRegistration = generateRandomDateInRange(earliestRegistrationDate);

        hkid.setHkidNum(HkidNumUtil.genRandomHkidNum());
        hkid.setChiName(new ChiName("\u9673", "\u5927\u6587", Arrays.asList("1234", "5678", "9999")));
        hkid.setEngName(new EngName("Chan", "Tai Man"));
        hkid.setSex(Sex.MALE);
        hkid.setDateOfBirth(dateOfBirth);
        hkid.setSymbols(Arrays.asList(DefinedSymbol.ThreeStars, DefinedSymbol.A, DefinedSymbol.Z));
        hkid.setDateOfRegistration(dateOfRegistration);
        hkid.setDateOfIssue(YearMonth.from(dateOfRegistration));
        return hkid;
    }

    public static LocalDate generateRandomDateInRange(LocalDate startDate) {
        return generateRandomDateInRange(startDate, LocalDate.now());
    }

    public static LocalDate generateRandomDateInRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }

        long startEpochDay = startDate.toEpochDay();
        long endEpochDay = endDate.toEpochDay();
        if (startEpochDay >= endEpochDay) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        return LocalDate.ofEpochDay(
                ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay));
    }

    private static LocalDate laterDate(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }
}
