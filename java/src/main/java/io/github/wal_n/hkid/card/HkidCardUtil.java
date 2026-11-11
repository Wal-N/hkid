package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.GeneratedName;
import io.github.wal_n.hkid.name.HkidNameUtil;
import io.github.wal_n.hkid.number.DefinedPrefix;
import io.github.wal_n.hkid.number.HkidNumberUtil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for generating HKID card data, describing holders, and finding
 * suspicious field combinations.
 */
public final class HkidCardUtil {
    private static final int MIN_AGE = 11;
    private static final int MAX_AGE = 100;
    private static final int RECENT_CARD_YEARS = 10;
    private static final int HONG_KONG_BIRTH_REGISTRATION_DAYS = 42;
    private static final YearMonth FIRST_HKID_ISSUE_MONTH = YearMonth.of(1949, 1);
    private static final ZoneId HONG_KONG_ZONE = ZoneId.of("Asia/Hong_Kong");

    private HkidCardUtil() {
        throw new AssertionError("HkidCardUtil cannot be instantiated");
    }

    /**
     * Describes a holder using today's date in Hong Kong and the JVM's current
     * default locale. The default locale is read on every call.
     *
     * @param card card whose holder is described
     * @return the summary, or an empty string when no describable information is present
     * @throws IllegalArgumentException if {@code card} is null or its birth date is in the future
     * @see #describeHolder(HkidCard, Locale, LocalDate)
     */
    public static String describeHolder(HkidCard card) {
        return describeHolder(card, null);
    }

    /**
     * Describes a holder using today's date in Hong Kong.
     *
     * @param card card whose holder is described
     * @param locale output locale; {@code null} uses {@link Locale#getDefault()} on this call
     * @return the summary in Traditional Chinese for {@code zh} locales, or English otherwise;
     *         empty when no describable information is present
     * @throws IllegalArgumentException if {@code card} is null or its birth date is in the future
     * @see #describeHolder(HkidCard, Locale, LocalDate)
     */
    public static String describeHolder(HkidCard card, Locale locale) {
        return describeHolder(card, locale, LocalDate.now(HONG_KONG_ZONE));
    }

    /**
     * Describes available age, sex, reported birthplace, and supported card symbols.
     * Chinese locales ({@code zh}, including {@code zh-CN}) produce Traditional Chinese;
     * all other locales produce English. A null locale resolves the JVM's default
     * locale on this call.
     *
     * <p>Birth date is used only to calculate age via {@link HkidCard#getAge(LocalDate)}.
     * Names, commercial codes, exact birth date, HKID number, and registration dates
     * are excluded. Missing information is omitted, and no facts are inferred from
     * the HKID prefix or missing symbols.</p>
     *
     * <p>Symbols describe the card's recorded information: age-specific eligibility
     * and stay restrictions retain their registration-time meaning. Symbols are
     * not revalidated against the holder's age on {@code referenceDate}, and
     * registration dates do not affect this summary.</p>
     *
     * @param card card whose holder is described
     * @param locale output locale; {@code null} uses {@link Locale#getDefault()} on this call
     * @param referenceDate date used only to calculate the holder's age
     * @return the summary, or an empty string when no describable information is present
     * @throws IllegalArgumentException if {@code card} or {@code referenceDate} is null,
     *         or the birth date is after {@code referenceDate}
     */
    public static String describeHolder(HkidCard card, Locale locale, LocalDate referenceDate) {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null");
        }
        Integer age = card.getAge(referenceDate).orElse(null);
        Locale effectiveLocale = locale != null ? locale : Locale.getDefault();
        boolean chinese = "zh".equals(effectiveLocale.getLanguage());
        HkidSymbols symbols = card.getSymbols();
        String[] sections = {
                describeBasicDetails(age, card.getSex(), symbols, chinese),
                describeStatusAndEligibility(symbols, chinese),
                describeAmendments(symbols, chinese)
        };
        List<String> sentences = new ArrayList<>();
        for (String section : sections) {
            if (!section.isEmpty()) {
                sentences.add(section);
            }
        }
        return String.join(chinese ? "" : " ", sentences);
    }

    private static String describeBasicDetails(
            Integer age, Sex sex, HkidSymbols symbols, boolean chinese) {
        String demographics = "";
        if (chinese) {
            if (sex != null) {
                demographics = "為" + (age != null ? age + "歲" : "")
                        + sex.getChineseMarker() + "性";
            } else if (age != null) {
                demographics = "現年" + age + "歲";
            }
        } else if (sex != null) {
            demographics = "is " + (sex == Sex.MALE ? "male" : "female")
                    + (age != null ? ", aged " + age : "");
        } else if (age != null) {
            demographics = "is " + age + (age == 1 ? " year old" : " years old");
        }
        String birthplace = describeBirthplace(symbols, chinese);
        if (demographics.isEmpty() && birthplace.isEmpty()) {
            return "";
        }
        String separator = "";
        if (!demographics.isEmpty() && !birthplace.isEmpty()) {
            separator = chinese ? "，" : (sex != null && age != null ? ", and " : " and ");
        }
        return (chinese ? "持有人" : "The holder ") + demographics + separator
                + birthplace + (chinese ? "。" : ".");
    }

    private static String describeBirthplace(HkidSymbols symbols, boolean chinese) {
        HkidSymbol symbol = symbolInCategory(symbols, HkidSymbolCategory.REPORTED_PLACE_OF_BIRTH);
        if (symbol == null) {
            return "";
        }
        switch (symbol) {
            case BORN_IN_HONG_KONG:
                return chinese ? "於香港出生" : "was born in Hong Kong";
            case BORN_IN_MAINLAND:
                return chinese ? "於內地出生" : "was born in the Chinese Mainland";
            case BORN_IN_MACAO:
                return chinese ? "於澳門出生" : "was born in Macao";
            case BORN_ELSEWHERE:
                return chinese ? "於其他地區出生" : "was born elsewhere";
            default:
                throw new IllegalArgumentException("Unsupported birthplace symbol: " + symbol.name());
        }
    }

    private static String describeStatusAndEligibility(HkidSymbols symbols, boolean chinese) {
        HkidSymbol status = symbolInCategory(symbols, HkidSymbolCategory.RESIDENTIAL_STATUS);
        HkidSymbol eligibility = symbolInCategory(
                symbols, HkidSymbolCategory.RE_ENTRY_PERMIT_ELIGIBILITY);
        if (status == null && eligibility == null) {
            return "";
        }
        boolean registrationStatus = status == HkidSymbol.STAY_LIMITED
                || status == HkidSymbol.STAY_NOT_LIMITED;
        String statusClause = describeResidentialStatus(status, chinese);
        String eligibilityClause = describeEligibility(eligibility, chinese);
        if (chinese) {
            String time = registrationStatus || status == null ? "於領證登記時" : "";
            String separator = "";
            if (status != null && eligibility != null) {
                separator = registrationStatus ? "，" : "，於領證登記時";
            }
            return "持有人" + time + statusClause + separator + eligibilityClause + "。";
        }
        String subject = registrationStatus || status == null
                ? "At registration, the holder " : "The holder ";
        String separator = "";
        if (status != null && eligibility != null) {
            separator = registrationStatus ? " and " : " and, at registration, ";
        }
        return subject + statusClause + separator + eligibilityClause + ".";
    }

    private static String describeResidentialStatus(HkidSymbol symbol, boolean chinese) {
        if (symbol == null) {
            return "";
        }
        switch (symbol) {
            case RIGHT_OF_ABODE:
                return chinese ? "享有香港居留權" : "has the right of abode in Hong Kong";
            case STAY_LIMITED:
                return chinese ? "有逗留限制" : "had a stay restriction";
            case RIGHT_TO_LAND:
                return chinese ? "享有香港入境權" : "has the right to land in Hong Kong";
            case STAY_NOT_LIMITED:
                return chinese ? "沒有逗留限制" : "had no stay restriction";
            default:
                throw new IllegalArgumentException("Unsupported residential status: " + symbol.name());
        }
    }

    private static String describeEligibility(HkidSymbol symbol, boolean chinese) {
        if (symbol == null) {
            return "";
        }
        switch (symbol) {
            case ADULT_RE_ENTRY_PERMIT:
                return chinese ? "年滿18歲，並符合回港證申領資格"
                        : "was aged 18 or over and eligible for a Re-entry Permit";
            case MINOR_RE_ENTRY_PERMIT:
                return chinese ? "年齡為11至17歲，並符合回港證申領資格"
                        : "was aged 11 to 17 and eligible for a Re-entry Permit";
            default:
                throw new IllegalArgumentException("Unsupported eligibility symbol: " + symbol.name());
        }
    }

    private static String describeAmendments(HkidSymbols symbols, boolean chinese) {
        boolean birthChanged = symbols.contains(HkidSymbol.BIRTH_DETAILS_CHANGED);
        boolean nameChanged = symbols.contains(HkidSymbol.NAME_CHANGED);
        if (birthChanged && nameChanged) {
            return chinese ? "持有人的出生日期或出生地曾更改，姓名亦曾更改。"
                    : "The holder's date or place of birth has been amended, as has their name.";
        }
        if (birthChanged) {
            return chinese ? "持有人的出生日期或出生地曾更改。"
                    : "The holder's date or place of birth has been amended.";
        }
        if (nameChanged) {
            return chinese ? "持有人的姓名曾更改。" : "The holder's name has been amended.";
        }
        return "";
    }

    private static HkidSymbol symbolInCategory(HkidSymbols symbols, HkidSymbolCategory category) {
        List<HkidSymbol> matching = symbols.getByCategory(category);
        return matching.isEmpty() ? null : matching.get(0);
    }

    /**
     * Tests whether any supplied fields form a suspicious combination according to
     * {@link #findSuspicions(HkidCard)}.
     *
     * <p>Missing fields and unavailable prefix metadata do not count as suspicious.
     * A {@code false} result means that no supported check found a suspicion; it
     * does not establish completeness or authenticity.</p>
     *
     * @param card card whose supplied fields are checked
     * @return {@code true} when at least one suspicion is found
     * @throws IllegalArgumentException if {@code card} is null
     */
    public static boolean isSuspicious(HkidCard card) {
        return !findSuspicions(card).isEmpty();
    }

    /**
     * Finds potentially inconsistent combinations of supplied card fields using
     * the bundled, non-authoritative prefix metadata.
     *
     * <p>Each check runs only when its required fields and metadata are present.
     * An empty card therefore has no suspicions. Results are advisory and do not
     * prevent construction of a card.</p>
     *
     * <p>For Hong Kong birth-registration prefixes, a birth on or after the
     * period's exclusive end is suspicious, as is an explicitly non-Hong-Kong
     * birthplace symbol. Births before a period starts are allowed because birth
     * registration may occur later; no maximum delay is assumed. The
     * {@link HkidSymbol#BIRTH_DETAILS_CHANGED} symbol suppresses both birth checks.</p>
     *
     * <p>For first-issue prefixes, the first-registration month must overlap the
     * known period. The current card's registration date is not used. Prefix
     * {@link DefinedPrefix#L} is excluded because the bundled period omits its
     * historical reserve use. Birth-detail changes do not suppress this check.</p>
     *
     * @param card card whose supplied fields are checked
     * @return an unmodifiable list of distinct reasons in {@link HkidSuspicion}
     *         declaration order, empty when no suspicion is found
     * @throws IllegalArgumentException if {@code card} is null
     */
    public static List<HkidSuspicion> findSuspicions(HkidCard card) {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null");
        }
        if (card.getHkidNumber() == null) {
            return Collections.emptyList();
        }
        DefinedPrefix prefix = card.getHkidNumber().getDefinedPrefix().orElse(null);
        if (prefix == null) {
            return Collections.emptyList();
        }

        List<HkidSuspicion> suspicions = new ArrayList<>();
        HkidSymbols symbols = card.getSymbols();
        if (prefix.hasHongKongBirthRegistrationPeriod()
                && !symbols.contains(HkidSymbol.BIRTH_DETAILS_CHANGED)) {
            LocalDate birthRegistrationEnd = prefix
                    .getHongKongBirthRegistrationEndDateExclusive().orElse(null);
            LocalDate dateOfBirth = card.getDateOfBirth();
            if (dateOfBirth != null && birthRegistrationEnd != null
                    && !dateOfBirth.isBefore(birthRegistrationEnd)) {
                suspicions.add(HkidSuspicion.PREFIX_BIRTH_DATE_MISMATCH);
            }
            if (!symbols.getByCategory(HkidSymbolCategory.REPORTED_PLACE_OF_BIRTH).isEmpty()
                    && !symbols.contains(HkidSymbol.BORN_IN_HONG_KONG)) {
                suspicions.add(HkidSuspicion.PREFIX_BIRTH_PLACE_MISMATCH);
            }
        }

        YearMonth firstRegistrationMonth = card.getFirstRegistrationYearMonth();
        if (firstRegistrationMonth != null && prefix != DefinedPrefix.L
                && prefix.hasFirstIssuePeriod()
                && !prefix.supportsFirstIssueMonth(firstRegistrationMonth)) {
            suspicions.add(HkidSuspicion.PREFIX_FIRST_REGISTRATION_MISMATCH);
        }
        return Collections.unmodifiableList(suspicions);
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
     * Generates a sample HKID card with the requested sex using the current
     * system date and a thread-local random generator.
     *
     * @param sex requested card sex and generated-name association
     * @return a randomly generated card with {@code sex}, valid as of the current system date
     * @throws IllegalArgumentException if {@code sex} is null
     */
    public static HkidCard generateRandomCard(Sex sex) {
        return generateRandomCard(LocalDate.now(), sex);
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
     * Generates a sample HKID card with the requested sex for a
     * caller-controlled reference date using a thread-local random generator.
     *
     * @param referenceDate date used to calculate age and registration ranges
     * @param sex requested card sex and generated-name association
     * @return a randomly generated card with {@code sex}, valid as of {@code referenceDate}
     * @throws IllegalArgumentException if either argument is null, or if
     *         {@code referenceDate} precedes the first issuance of the current smart HKID
     */
    public static HkidCard generateRandomCard(LocalDate referenceDate, Sex sex) {
        return generateRandomCard(ThreadLocalRandom.current(), referenceDate, sex);
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
        return generateRandomCardInternal(random, referenceDate, null);
    }

    /**
     * Generates a reproducible sample HKID card with the requested sex using
     * caller-controlled random state and reference date.
     *
     * @param random random generator used for every generated card value
     * @param referenceDate date used to calculate age and registration ranges
     * @param sex requested card sex and generated-name association
     * @return a generated card with {@code sex}, valid as of {@code referenceDate}
     * @throws IllegalArgumentException if any argument is null, or if
     *         {@code referenceDate} precedes the first issuance of the current smart HKID
     */
    public static HkidCard generateRandomCard(
            Random random, LocalDate referenceDate, Sex sex) {
        if (sex == null) {
            throw new IllegalArgumentException("Sex cannot be null");
        }
        return generateRandomCardInternal(random, referenceDate, sex);
    }

    private static HkidCard generateRandomCardInternal(
            Random random, LocalDate referenceDate, Sex requestedSex) {
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
                earliestRegistrationDate,
                HkidCard.ageAnniversary(dateOfBirth, minimumRegistrationAge));
        LocalDate dateOfRegistration = generateRandomDateInRangeInclusive(
                random, earliestRegistrationDate, referenceDate);
        YearMonth earliestFirstRegistrationMonth = laterYearMonth(
                YearMonth.from(HkidCard.ageAnniversary(dateOfBirth, MIN_AGE)),
                FIRST_HKID_ISSUE_MONTH);
        YearMonth firstRegistrationYearMonth = generateRandomYearMonthInRangeInclusive(
                random,
                earliestFirstRegistrationMonth,
                YearMonth.from(dateOfRegistration));
        LocalDate birthRegistrationDate = generateRandomDateInRangeInclusive(
                random,
                dateOfBirth,
                dateOfBirth.plusDays(HONG_KONG_BIRTH_REGISTRATION_DAYS));

        Sex sex = requestedSex != null ? requestedSex : generateRandomSex(random);
        GeneratedName name = HkidNameUtil.generateRandomName(random, sex);
        DefinedPrefix[] compatiblePrefixes = compatiblePrefixesFor(
                birthRegistrationDate, firstRegistrationYearMonth, dateOfRegistration);

        return HkidCard.builder()
                .hkidNumber(HkidNumberUtil.generateRandomHkidNumber(random, compatiblePrefixes))
                .chineseName(name.getChineseName())
                .englishName(name.getEnglishName())
                .sex(sex)
                .dateOfBirth(dateOfBirth)
                .symbols(HkidSymbols.of(
                        ageSymbol,
                        HkidSymbol.RIGHT_OF_ABODE,
                        HkidSymbol.BORN_IN_HONG_KONG))
                .dateOfRegistration(dateOfRegistration)
                .firstRegistrationYearMonth(firstRegistrationYearMonth)
                .build();
    }

    /**
     * Generates a random date from {@code startDate} (inclusive) to the current
     * system date (exclusive).
     *
     * @param startDate inclusive start of the range
     * @return a random date before the current system date
     * @throws IllegalArgumentException if the start date is null or is not before today
     */
    public static LocalDate generateRandomDateInRange(LocalDate startDate) {
        return generateRandomDateInRange(startDate, LocalDate.now());
    }

    /**
     * Generates a random date in a half-open range.
     *
     * @param startDate inclusive start of the range
     * @param endDate exclusive end of the range
     * @return a random date on or after {@code startDate} and before {@code endDate}
     * @throws IllegalArgumentException if either date is null or the start is not
     *         before the end
     */
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
        return HkidCard.ageOn(dateOfBirth, referenceDate) >= 18
                ? HkidSymbol.ADULT_RE_ENTRY_PERMIT
                : HkidSymbol.MINOR_RE_ENTRY_PERMIT;
    }

    private static DefinedPrefix[] compatiblePrefixesFor(
            LocalDate birthRegistrationDate,
            YearMonth firstRegistrationMonth,
            LocalDate dateOfRegistration) {
        DefinedPrefix exactBirthRegistrationPrefix = DefinedPrefix
                .fromHongKongBirthRegistrationDate(birthRegistrationDate)
                .orElse(null);
        if (exactBirthRegistrationPrefix != null) {
            return prefixes(exactBirthRegistrationPrefix);
        }

        DefinedPrefix[] compatiblePrefixes = DefinedPrefix
                .fromFirstIssueMonth(firstRegistrationMonth, dateOfRegistration);
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
        return generateRandomDateInRangeInclusive(random, earliestDateOfBirth, latestDateOfBirth);
    }

    private static LocalDate generateRandomDateInRangeInclusive(
            Random random, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        int numberOfDays = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate)) + 1;
        return startDate.plusDays(random.nextInt(numberOfDays));
    }

    private static YearMonth generateRandomYearMonthInRangeInclusive(
            Random random, YearMonth startMonth, YearMonth endMonth) {
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
