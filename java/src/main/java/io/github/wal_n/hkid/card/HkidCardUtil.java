package io.github.wal_n.hkid.card;

import io.github.wal_n.hkid.name.GeneratedName;
import io.github.wal_n.hkid.name.HkidNameUtil;
import io.github.wal_n.hkid.number.DefinedPrefix;
import io.github.wal_n.hkid.number.HkidNumberUtil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for generating HKID card data, describing holders, and finding
 * suspicious field combinations.
 */
public final class HkidCardUtil {
    private static final int MIN_AGE = 11;
    private static final int DEFAULT_MAX_AGE = 85;
    private static final int MAX_AGE = 100;
    private static final int RECENT_CARD_YEARS = 10;
    private static final int HONG_KONG_BIRTH_REGISTRATION_DAYS = 42;
    private static final LocalDate FIRST_HKID_ISSUE_DATE = LocalDate.of(1949, 1, 1);
    private static final int CENSUS_2021_HONG_KONG_BORN = 4_574_834;
    private static final int CENSUS_2021_MAINLAND_BORN = 2_144_126;
    private static final int CENSUS_2021_MACAO_BORN = 51_975;
    private static final int CENSUS_2021_ELSEWHERE_BORN = 17_761 + 624_374; // Taiwan and all other places
    private static final int CENSUS_2021_POPULATION = CENSUS_2021_HONG_KONG_BORN + CENSUS_2021_MAINLAND_BORN
            + CENSUS_2021_MACAO_BORN + CENSUS_2021_ELSEWHERE_BORN;

    private HkidCardUtil() {
        throw new AssertionError("HkidCardUtil cannot be instantiated");
    }

    /**
     * Describes a holder using the current system date and the JVM's current
     * default locale. The default time zone and locale are read on every call.
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
     * Describes a holder using the current date in the JVM's default time zone,
     * resolved on every call.
     *
     * @param card card whose holder is described
     * @param locale output locale; {@code null} uses {@link Locale#getDefault()} on this call
     * @return the summary in Traditional Chinese for {@code zh} locales, or English otherwise;
     *         empty when no describable information is present
     * @throws IllegalArgumentException if {@code card} is null or its birth date is in the future
     * @see #describeHolder(HkidCard, Locale, LocalDate)
     */
    public static String describeHolder(HkidCard card, Locale locale) {
        return describeHolder(card, locale, LocalDate.now());
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
        Sex sex = card.getSex();
        HolderReference reference = HolderReference.forSex(sex);
        String[] sections = {
                describeBasicDetails(age, sex, symbols, chinese),
                describeStatusAndEligibility(symbols, chinese, reference),
                describeAmendments(symbols, chinese, reference)
        };
        List<String> sentences = new ArrayList<>();
        for (String section : sections) {
            if (!section.isEmpty()) {
                sentences.add(section);
            }
        }
        return String.join(chinese ? "" : " ", sentences);
    }

    private enum HolderReference {
        HOLDER("持有人", "The holder", "The holder's", "their"),
        MALE("他", "He", "His", "his"),
        FEMALE("她", "She", "Her", "her");

        final String chineseSubject;
        final String englishSubject;
        final String englishPossessive;
        final String englishPossessivePronoun;

        HolderReference(String chineseSubject, String englishSubject,
                        String englishPossessive, String englishPossessivePronoun) {
            this.chineseSubject = chineseSubject;
            this.englishSubject = englishSubject;
            this.englishPossessive = englishPossessive;
            this.englishPossessivePronoun = englishPossessivePronoun;
        }

        static HolderReference forSex(Sex sex) {
            if (sex == null) {
                return HOLDER;
            }
            return sex == Sex.FEMALE ? FEMALE : MALE;
        }
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

    private static String describeStatusAndEligibility(
            HkidSymbols symbols, boolean chinese, HolderReference reference) {
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
            return reference.chineseSubject + time + statusClause + separator + eligibilityClause + "。";
        }
        String subject = registrationStatus || status == null
                ? "At registration, " + reference.englishSubject.toLowerCase(Locale.ROOT)
                : reference.englishSubject;
        String separator = "";
        if (status != null && eligibility != null) {
            separator = registrationStatus ? " and " : " and, at registration, ";
        }
        return subject + " " + statusClause + separator + eligibilityClause + ".";
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

    private static String describeAmendments(
            HkidSymbols symbols, boolean chinese, HolderReference reference) {
        boolean birthChanged = symbols.contains(HkidSymbol.BIRTH_DETAILS_CHANGED);
        boolean nameChanged = symbols.contains(HkidSymbol.NAME_CHANGED);
        String owner = chinese ? reference.chineseSubject + "的" : reference.englishPossessive;
        if (birthChanged && nameChanged) {
            return chinese ? owner + "出生日期或出生地曾更改，姓名亦曾更改。"
                    : owner + " date or place of birth has been amended, as has "
                            + reference.englishPossessivePronoun + " name.";
        }
        if (birthChanged) {
            return chinese ? owner + "出生日期或出生地曾更改。"
                    : owner + " date or place of birth has been amended.";
        }
        if (nameChanged) {
            return chinese ? owner + "姓名曾更改。" : owner + " name has been amended.";
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
     * Generates a sample HKID card for a holder aged 11 through 85 inclusive
     * as of today. Birthplace is sampled using the 2021 Hong Kong Population Census.
     * The JVM's default time zone is read on every call.
     *
     * @return a randomly generated card valid as of the current system date
     */
    public static HkidCard generateRandomCard() {
        return generateRandomCard(LocalDate.now());
    }

    /**
     * Generates a sample HKID card with the requested sex using the same age range,
     * census birthplace distribution and local date as {@link #generateRandomCard()}.
     *
     * @param sex requested card sex and generated-name association
     * @return a randomly generated card with {@code sex}, valid as of the current system date
     * @throws IllegalArgumentException if {@code sex} is null
     */
    public static HkidCard generateRandomCard(Sex sex) {
        return generateRandomCard(LocalDate.now(), sex);
    }

    /**
     * Generates a sample HKID card using the age range and census birthplace
     * distribution of {@link #generateRandomCard()} on the supplied reference date.
     *
     * @param referenceDate date used to calculate age and registration ranges
     * @return a randomly generated card valid as of {@code referenceDate}
     * @throws IllegalArgumentException if {@code referenceDate} is null or precedes
     *         the first issuance of the current smart HKID
     */
    public static HkidCard generateRandomCard(LocalDate referenceDate) {
        return generateRandomCardByBirthplaceDistribution(referenceDate, MIN_AGE, DEFAULT_MAX_AGE);
    }

    /**
     * Generates a sample HKID card with the requested sex using the age range and census
     * birthplace distribution of {@link #generateRandomCard()} on the supplied reference date.
     *
     * @param referenceDate date used to calculate age and registration ranges
     * @param sex requested card sex and generated-name association
     * @return a randomly generated card with {@code sex}, valid as of {@code referenceDate}
     * @throws IllegalArgumentException if either argument is null, or if
     *         {@code referenceDate} precedes the first issuance of the current smart HKID
     */
    public static HkidCard generateRandomCard(LocalDate referenceDate, Sex sex) {
        return generateRandomCard(referenceDate, sex, MIN_AGE, DEFAULT_MAX_AGE, sampleCensusBirthplace());
    }

    /**
     * Generates a complete card for a holder in the requested age range as of today.
     *
     * @param minAge minimum age in completed years, inclusive
     * @param maxAge maximum age in completed years, inclusive
     * @param birthplace reported place-of-birth symbol
     * @return a card with the requested age range and birthplace
     * @throws IllegalArgumentException if the range is outside 11 to 100 or reversed,
     *         or the birthplace is null or not a place-of-birth symbol
     */
    public static HkidCard generateRandomCard(int minAge, int maxAge, HkidSymbol birthplace) {
        return generateRandomCard(LocalDate.now(), minAge, maxAge, birthplace);
    }

    /**
     * Generates a complete card for a holder in the requested age range on the reference date.
     * Equal bounds select an exact age. For a holder born outside Hong Kong the
     * number prefix is selected from the first-issue period.
     *
     * @param referenceDate date on which the holder's age is measured, on or after 2018-11-26
     * @param minAge minimum age in completed years, inclusive
     * @param maxAge maximum age in completed years, inclusive
     * @param birthplace reported place-of-birth symbol
     * @return a card with the requested age range and birthplace
     * @throws IllegalArgumentException if an argument is null, the reference date is too early,
     *         the range is outside 11 to 100 or reversed, or the symbol is not a birthplace
     */
    public static HkidCard generateRandomCard(
            LocalDate referenceDate, int minAge, int maxAge, HkidSymbol birthplace) {
        return generateRandomCardInternal(referenceDate, null, minAge, maxAge, birthplace);
    }

    /**
     * Generates a complete card with the requested sex, age range and birthplace.
     *
     * @param referenceDate date on which the holder's age is measured, on or after 2018-11-26
     * @param sex requested card sex and generated-name association
     * @param minAge minimum age in completed years, inclusive
     * @param maxAge maximum age in completed years, inclusive
     * @param birthplace symbol in {@link HkidSymbolCategory#REPORTED_PLACE_OF_BIRTH}
     * @return a card with the requested sex, age range and birthplace
     * @throws IllegalArgumentException if an argument is null, the reference date is too early,
     *         the range is outside 11 to 100 or reversed, or the symbol is not a birthplace
     */
    public static HkidCard generateRandomCard(
            LocalDate referenceDate, Sex sex, int minAge, int maxAge, HkidSymbol birthplace) {
        if (sex == null) {
            throw new IllegalArgumentException("Sex cannot be null");
        }
        return generateRandomCardInternal(referenceDate, sex, minAge, maxAge, birthplace);
    }

    /**
     * Generates a complete card with a birthplace sampled from the 2021 Hong Kong
     * Population Census. Each call uses the same all-ages weights, independently of
     * the requested age range or reference date. Taiwan is included in elsewhere.
     *
     * @param referenceDate date on which the holder's age is measured, on or after 2018-11-26
     * @param minAge minimum age in completed years, inclusive
     * @param maxAge maximum age in completed years, inclusive
     * @return a card with the sampled birthplace and an age in the requested range
     * @throws IllegalArgumentException if an argument is null, the reference date is too early,
     *         or the range is outside 11 to 100 or reversed
     * @see <a href="https://idds.census2021.gov.hk/api/query?cv.BORNPL=01,11,12,13,31-89&amp;sv.RP=RP_NPER&amp;period=2021&amp;lang=en">2021 Census birthplace counts</a>
     */
    public static HkidCard generateRandomCardByBirthplaceDistribution(
            LocalDate referenceDate, int minAge, int maxAge) {
        return generateRandomCard(referenceDate, minAge, maxAge, sampleCensusBirthplace());
    }

    private static HkidSymbol sampleCensusBirthplace() {
        return censusBirthplaceForTicket(ThreadLocalRandom.current().nextInt(CENSUS_2021_POPULATION));
    }

    static HkidSymbol censusBirthplaceForTicket(int ticket) {
        if (ticket < 0 || ticket >= CENSUS_2021_POPULATION) {
            throw new IllegalArgumentException("Ticket must be within the 2021 census population");
        }
        if (ticket < CENSUS_2021_HONG_KONG_BORN) {
            return HkidSymbol.BORN_IN_HONG_KONG;
        }
        if (ticket < CENSUS_2021_HONG_KONG_BORN + CENSUS_2021_MAINLAND_BORN) {
            return HkidSymbol.BORN_IN_MAINLAND;
        }
        if (ticket < CENSUS_2021_HONG_KONG_BORN + CENSUS_2021_MAINLAND_BORN + CENSUS_2021_MACAO_BORN) {
            return HkidSymbol.BORN_IN_MACAO;
        }
        return HkidSymbol.BORN_ELSEWHERE;
    }

    private static HkidCard generateRandomCardInternal(
            LocalDate referenceDate, Sex requestedSex, int minAge, int maxAge, HkidSymbol birthplace) {
        if (referenceDate == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }
        if (referenceDate.isBefore(HkidCard.CURRENT_SMART_HKID_START_DATE)) {
            throw new IllegalArgumentException(
                    "Reference date cannot be before "
                            + HkidCard.CURRENT_SMART_HKID_START_DATE);
        }

        if (minAge < MIN_AGE || maxAge > MAX_AGE || minAge > maxAge) {
            throw new IllegalArgumentException("Age range must be between " + MIN_AGE + " and " + MAX_AGE
                    + " with minimum age no greater than maximum age");
        }
        if (birthplace == null || birthplace.getCategory() != HkidSymbolCategory.REPORTED_PLACE_OF_BIRTH) {
            throw new IllegalArgumentException("Expected a reported place-of-birth symbol");
        }
        LocalDate dateOfBirth = generateRandomDateOfBirth(referenceDate, minAge, maxAge);
        return generateCompleteCard(referenceDate, requestedSex, dateOfBirth, birthplace);
    }

    static HkidCard generateCompleteCard(
            LocalDate referenceDate, Sex requestedSex, LocalDate dateOfBirth, HkidSymbol birthplace) {
        HkidSymbol ageSymbol = generateAgeSymbol(dateOfBirth, referenceDate);
        int minimumRegistrationAge = ageSymbol == HkidSymbol.ADULT_RE_ENTRY_PERMIT ? 18 : MIN_AGE;
        LocalDate earliestRegistrationDate = laterDate(
                referenceDate.minusYears(RECENT_CARD_YEARS), HkidCard.CURRENT_SMART_HKID_START_DATE);
        earliestRegistrationDate = laterDate(
                earliestRegistrationDate,
                HkidCard.ageAnniversary(dateOfBirth, minimumRegistrationAge));
        LocalDate dateOfRegistration = generateRandomDateInRangeInclusive(
                earliestRegistrationDate, referenceDate);
        LocalDate earliestFirstRegistrationDate = laterDate(
                HkidCard.ageAnniversary(dateOfBirth, MIN_AGE), FIRST_HKID_ISSUE_DATE);
        YearMonth firstRegistrationYearMonth = generateRandomYearMonthInRangeInclusive(
                YearMonth.from(earliestFirstRegistrationDate),
                YearMonth.from(dateOfRegistration));
        LocalDate birthRegistrationDate = birthplace == HkidSymbol.BORN_IN_HONG_KONG
                ? generateRandomDateInRangeInclusive(dateOfBirth, dateOfBirth.plusDays(HONG_KONG_BIRTH_REGISTRATION_DAYS))
                : null;
        DefinedPrefix[] compatiblePrefixes = compatiblePrefixesFor(birthRegistrationDate,
                firstRegistrationYearMonth, earliestFirstRegistrationDate, dateOfRegistration);

        Sex sex = requestedSex != null ? requestedSex : generateRandomSex();
        GeneratedName name = HkidNameUtil.generateRandomName(sex);

        return HkidCard.builder()
                .hkidNumber(HkidNumberUtil.generateRandomHkidNumber(compatiblePrefixes))
                .chineseName(name.getChineseName())
                .englishName(name.getEnglishName())
                .sex(sex)
                .dateOfBirth(dateOfBirth)
                .symbols(HkidSymbols.of(ageSymbol, HkidSymbol.RIGHT_OF_ABODE, birthplace))
                .dateOfRegistration(dateOfRegistration)
                .firstRegistrationYearMonth(firstRegistrationYearMonth)
                .build();
    }

    /**
     * Generates a random date from {@code startDate} (inclusive) to the current
     * system date (exclusive). The JVM's default time zone is read on every call.
     *
     * @param startDate inclusive start of the range
     * @return a random date before the current system date
     * @throws IllegalArgumentException if the start date is null or is not before
     *         the current system date
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

    private static Sex generateRandomSex() {
        Sex[] values = Sex.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private static HkidSymbol generateAgeSymbol(LocalDate dateOfBirth, LocalDate referenceDate) {
        return HkidCard.ageOn(dateOfBirth, referenceDate) >= 18
                ? HkidSymbol.ADULT_RE_ENTRY_PERMIT
                : HkidSymbol.MINOR_RE_ENTRY_PERMIT;
    }

    static DefinedPrefix[] compatiblePrefixesFor(
            LocalDate hongKongBirthRegistrationDate,
            YearMonth firstRegistrationMonth,
            LocalDate earliestFirstRegistrationDate,
            LocalDate dateOfRegistration) {
        DefinedPrefix birthPrefix = DefinedPrefix.fromHongKongBirthRegistrationDate(hongKongBirthRegistrationDate)
                .orElse(null);
        if (birthPrefix != null) {
            return new DefinedPrefix[] {birthPrefix};
        }

        DefinedPrefix[] compatiblePrefixes = DefinedPrefix
                .fromFirstIssueMonth(firstRegistrationMonth, earliestFirstRegistrationDate, dateOfRegistration);
        if (compatiblePrefixes.length == 0) {
            throw new IllegalStateException(
                    "No HKID prefix supports first registration month " + firstRegistrationMonth);
        }
        return compatiblePrefixes;
    }

    private static LocalDate generateRandomDateOfBirth(LocalDate referenceDate, int minAge, int maxAge) {
        int age = ThreadLocalRandom.current().nextInt(minAge, maxAge + 1);
        LocalDate earliestDateOfBirth = referenceDate.minusYears(age + 1L).plusDays(1);
        LocalDate latestDateOfBirth = referenceDate.minusYears(age);
        return generateRandomDateInRangeInclusive(earliestDateOfBirth, latestDateOfBirth);
    }

    private static LocalDate generateRandomDateInRangeInclusive(
            LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        int numberOfDays = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate)) + 1;
        return startDate.plusDays(ThreadLocalRandom.current().nextInt(numberOfDays));
    }

    private static YearMonth generateRandomYearMonthInRangeInclusive(
            YearMonth startMonth, YearMonth endMonth) {
        if (startMonth.isAfter(endMonth)) {
            throw new IllegalArgumentException("Start month cannot be after end month");
        }

        int numberOfMonths = Math.toIntExact(ChronoUnit.MONTHS.between(startMonth, endMonth)) + 1;
        return startMonth.plusMonths(ThreadLocalRandom.current().nextInt(numberOfMonths));
    }

    private static LocalDate laterDate(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }
}
