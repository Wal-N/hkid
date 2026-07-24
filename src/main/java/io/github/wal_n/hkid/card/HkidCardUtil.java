package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.GeneratedName;
import io.github.wal_n.hkid.name.HkidNameUtil;
import io.github.wal_n.hkid.number.DefinedPrefix;
import io.github.wal_n.hkid.number.HkidNumberUtil;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for generating complete HKID card data.
 */
public final class HkidCardUtil {
    private static final int MIN_AGE = 11;
    private static final int MAX_AGE = 100;
    private static final int RECENT_CARD_YEARS = 10;
    private static final int HONG_KONG_BIRTH_REGISTRATION_DAYS = 42;
    private static final YearMonth FIRST_HKID_ISSUE_MONTH = YearMonth.of(1949, 1);

    private HkidCardUtil() {
        throw new AssertionError("HkidCardUtil cannot be instantiated");
    }

    /**
     * Generates a sample HKID card using the current system date and a
     * thread-local random generator.
     *
     * @return a randomly generated card valid as of the current system date
     */
    public static HkidCard generateRandomCard() {
        return generateRandomCard(LocalDate.now());
    }

    /**
     * Generates a sample HKID card for a caller-controlled reference date using
     * a thread-local random generator.
     *
     * <p>Use this overload when the generated card should be valid at a specific
     * point in time but its random values do not need to be reproducible.</p>
     *
     * @param referenceDate date used to calculate age and registration ranges
     * @return a randomly generated card valid as of {@code referenceDate}
     * @throws IllegalArgumentException if {@code referenceDate} is null or precedes
     *         the first issuance of the current smart HKID
     */
    public static HkidCard generateRandomCard(LocalDate referenceDate) {
        return generateRandomCard(ThreadLocalRandom.current(), referenceDate);
    }

    /**
     * Generates a reproducible sample HKID card using caller-controlled random
     * state and reference date.
     *
     * <p>Supplying equivalent {@link Random} state and the same reference date
     * produces the same card within this Java implementation. This overload is
     * useful for deterministic tests, fixtures, and reproducing generated data.</p>
     *
     * @param random random generator used for every generated card value
     * @param referenceDate date used to calculate age and registration ranges
     * @return a generated card valid as of {@code referenceDate}
     * @throws IllegalArgumentException if {@code random} or {@code referenceDate} is null,
     *         or if {@code referenceDate} precedes the first issuance of the current smart HKID
     */
    public static HkidCard generateRandomCard(Random random, LocalDate referenceDate) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null");
        }
        if (referenceDate == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }
        if (referenceDate.isBefore(HkidCard.CURRENT_SMART_HKID_START_DATE)) {
            throw new IllegalArgumentException(
                    "Reference date cannot be before "
                            + HkidCard.CURRENT_SMART_HKID_START_DATE);
        }

        LocalDate dateOfBirth = generateRandomDateOfBirth(random, referenceDate);
        HkidSymbol ageSymbol = generateAgeSymbol(dateOfBirth, referenceDate);
        int minimumRegistrationAge = ageSymbol == HkidSymbol.ADULT_RE_ENTRY_PERMIT ? 18 : MIN_AGE;
        LocalDate earliestRegistrationDate = laterDate(
                referenceDate.minusYears(RECENT_CARD_YEARS), HkidCard.CURRENT_SMART_HKID_START_DATE);
        earliestRegistrationDate = laterDate(
                earliestRegistrationDate, dateOfBirth.plusYears(minimumRegistrationAge));
        LocalDate dateOfRegistration = generateRandomDateInRangeInclusive(
                earliestRegistrationDate, referenceDate, random);
        YearMonth earliestFirstRegistrationMonth = laterYearMonth(
                YearMonth.from(dateOfBirth.plusYears(MIN_AGE)), FIRST_HKID_ISSUE_MONTH);
        YearMonth firstRegistrationYearMonth = generateRandomYearMonthInRangeInclusive(
                earliestFirstRegistrationMonth,
                YearMonth.from(dateOfRegistration),
                random);
        LocalDate birthRegistrationDate = generateRandomDateInRangeInclusive(
                dateOfBirth,
                dateOfBirth.plusDays(HONG_KONG_BIRTH_REGISTRATION_DAYS),
                random);

        GeneratedName name = HkidNameUtil.generateRandomName(random);
        DefinedPrefix[] compatiblePrefixes = compatiblePrefixesFor(
                birthRegistrationDate, firstRegistrationYearMonth);

        return HkidCard.builder()
                .hkidNumber(HkidNumberUtil.generateRandomHkidNumber(random, compatiblePrefixes))
                .chineseName(name.getChineseName())
                .englishName(name.getEnglishName())
                .sex(generateRandomSex(random))
                .dateOfBirth(dateOfBirth)
                .symbols(HkidSymbols.of(
                        ageSymbol,
                        HkidSymbol.RIGHT_OF_ABODE,
                        HkidSymbol.BORN_IN_HONG_KONG))
                .dateOfRegistration(dateOfRegistration)
                .firstRegistrationYearMonth(firstRegistrationYearMonth)
                .build();
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

    private static Sex generateRandomSex(Random random) {
        Sex[] values = Sex.values();
        return values[random.nextInt(values.length)];
    }

    private static HkidSymbol generateAgeSymbol(LocalDate dateOfBirth, LocalDate referenceDate) {
        return Period.between(dateOfBirth, referenceDate).getYears() >= 18
                ? HkidSymbol.ADULT_RE_ENTRY_PERMIT
                : HkidSymbol.MINOR_RE_ENTRY_PERMIT;
    }

    private static DefinedPrefix[] compatiblePrefixesFor(
            LocalDate birthRegistrationDate, YearMonth firstRegistrationMonth) {
        DefinedPrefix exactBirthRegistrationPrefix = DefinedPrefix
                .fromHongKongBirthRegistrationDate(birthRegistrationDate)
                .orElse(null);
        if (exactBirthRegistrationPrefix != null) {
            return prefixes(exactBirthRegistrationPrefix);
        }

        DefinedPrefix[] compatiblePrefixes = DefinedPrefix
                .fromFirstIssueMonth(firstRegistrationMonth);
        if (compatiblePrefixes.length == 0) {
            throw new IllegalStateException(
                    "No HKID prefix supports first registration month " + firstRegistrationMonth);
        }
        return compatiblePrefixes;
    }

    private static DefinedPrefix[] prefixes(DefinedPrefix... prefixes) {
        return prefixes;
    }

    private static LocalDate generateRandomDateOfBirth(Random random, LocalDate referenceDate) {
        int age = MIN_AGE + random.nextInt(MAX_AGE - MIN_AGE + 1);
        LocalDate earliestDateOfBirth = referenceDate.minusYears(age + 1L).plusDays(1);
        LocalDate latestDateOfBirth = referenceDate.minusYears(age);
        return generateRandomDateInRangeInclusive(earliestDateOfBirth, latestDateOfBirth, random);
    }

    private static LocalDate generateRandomDateInRangeInclusive(
            LocalDate startDate, LocalDate endDate, Random random) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        int numberOfDays = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate)) + 1;
        return startDate.plusDays(random.nextInt(numberOfDays));
    }

    private static YearMonth generateRandomYearMonthInRangeInclusive(
            YearMonth startMonth, YearMonth endMonth, Random random) {
        if (startMonth.isAfter(endMonth)) {
            throw new IllegalArgumentException("Start month cannot be after end month");
        }

        int numberOfMonths = Math.toIntExact(ChronoUnit.MONTHS.between(startMonth, endMonth)) + 1;
        return startMonth.plusMonths(random.nextInt(numberOfMonths));
    }

    private static LocalDate laterDate(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private static YearMonth laterYearMonth(YearMonth first, YearMonth second) {
        return first.isAfter(second) ? first : second;
    }
}
