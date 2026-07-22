package hkid;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Hong Kong Identity Card (HKID) number.
 * This class allows for generating, validating, and formatting HKID numbers according to the official specifications.
 * It supports creating HKID numbers with specific or random values and offers methods to retrieve or modify components of an HKID number.
 * <p>
 * Methods of this class can throw custom exceptions {@link InvalidHkidNumFormatException} and {@link InvalidCheckDigitException}
 * to indicate problems with the provided HKID number format or check digit, respectively.
 */
public class HkidNum {
    /**
     * The prefix of the HKID number, which can be one or two letters (A-Z).
     */
    private String prefix;

    /**
     * The six-digit numeral part of the HKID number.
     */
    private String numerals;

    /**
     * The check digit of the HKID number, which can be a digit (0-9) or the letter 'A'.
     */
    private Character checkDigit;

    private static final Pattern HKID_NUM_PATTERN = Pattern.compile("^([A-Z]{1,2})(\\d{6})(?:([\\dA])|\\(([\\dA])\\))?$");
    private static final String INVALID_HKID_NUM_FORMAT_MESSAGE = "Invalid format for HKID number.";
    private static final String INVALID_CHECK_DIGIT_MESSAGE = "Invalid check digit for HKID number.";
    private static final String INVALID_CHECK_DIGIT_FORMAT_MESSAGE = "Invalid check digit format.";
    private static final String INVALID_PREFIX_FORMAT_MESSAGE = "Invalid prefix format.";


    /**
     * Constructs a new {@code HkidNum} instance by parsing the provided HKID number string.
     * The input string may include or exclude the check digit and parentheses around the check digit.
     *
     * @param hkidNum The HKID number string to parse. Acceptable formats include "X123456(A)", "XX123456A", or "X123456".
     *                The check digit and parentheses are optional.
     * @throws InvalidHkidNumFormatException If the input string is null, empty, does not match expected patterns,
     *                                    or contains characters that are not allowed.
     * @throws InvalidCheckDigitException If the provided check digit is incorrect.
     */
    public HkidNum(String hkidNum) {
        if (hkidNum == null || hkidNum.isEmpty()) {
            throw new InvalidHkidNumFormatException("HKID number cannot be null or empty.");
        }
        hkidNum = hkidNum.trim().toUpperCase(Locale.ROOT);

        // Extract parts
        Matcher matcher = HKID_NUM_PATTERN.matcher(hkidNum);
        if (matcher.matches()) {
            // Set Prefix, Numerals and generate Check Digit
            setPrefix(matcher.group(1));
            setNumerals(matcher.group(2));

            // Check input Check Digit is correct (if any)
            String inputCheckDigit = matcher.group(3) != null ? matcher.group(3) : (matcher.group(4) != null ? matcher.group(4) : null);
            if (inputCheckDigit != null && !inputCheckDigit.equals(this.checkDigit.toString())) {
                throw new InvalidCheckDigitException(INVALID_CHECK_DIGIT_MESSAGE);
            }
        } else {
            throw new InvalidHkidNumFormatException(INVALID_HKID_NUM_FORMAT_MESSAGE);
        }
    }

    /**
     * Constructs a new {@code HkidNum} instance using the specified prefix and numerals.
     * This constructor automatically calculates and assigns the check digit based on the provided prefix and numerals.
     * The generated instance will represent a complete HKID number including a valid check digit.
     *
     * @param prefix The prefix part of the HKID number. It should consist of one or two alphabetical characters.
     * @param numerals The numerals part of the HKID number. It should consist of six digits.
     * @throws InvalidHkidNumFormatException If either the prefix or numerals are null, empty, do not match expected patterns,
     *                                       or contain characters that are not allowed.
     * @throws InvalidCheckDigitException If the calculation of the check digit fails due to invalid input parameters.
     */
    public HkidNum(String prefix, String numerals) {
        this(prefix, numerals, null);
    }

    /**
     * Constructs a new {@code HkidNum} instance using the specified prefix, numerals, and check digit.
     * This constructor allows for the explicit specification of the check digit and verifies it against the calculated value.
     * It is useful for creating {@code HkidNum} instances that need to match existing HKID numbers exactly, including their check digits.
     *
     * @param prefix The prefix part of the HKID number. It should consist of one or two alphabetical characters.
     * @param numerals The numerals part of the HKID number. It should consist of six digits.
     * @param checkDigit The check digit of the HKID number. It is a single digit or the letter 'A'. This parameter is optional;
     *                   if null or empty, the check digit will be automatically calculated.
     * @throws InvalidHkidNumFormatException If either the prefix or numerals are null, empty, do not match expected patterns,
     *                                       or contain characters that are not allowed.
     * @throws InvalidCheckDigitException If the provided check digit is incorrect or if the calculation of the check digit fails due to invalid input parameters.
     */
    public HkidNum(String prefix, String numerals, String checkDigit) {
        setPrefix(prefix);
        setNumerals(numerals);

        if (checkDigit == null || checkDigit.isEmpty()) {
            return;
        }

        String normalizedCheckDigit = checkDigit.toUpperCase(Locale.ROOT);
        if (!normalizedCheckDigit.matches("^[\\dA]$")) {
            throw new InvalidHkidNumFormatException(INVALID_CHECK_DIGIT_FORMAT_MESSAGE);
        }
        if (!normalizedCheckDigit.equals(this.checkDigit.toString())) {
            throw new InvalidCheckDigitException(INVALID_CHECK_DIGIT_MESSAGE);
        }
    }

    public static boolean validateCheckDigit(String hkidNumWithoutCheckDigit, String checkDigit) {
        if (hkidNumWithoutCheckDigit == null || checkDigit == null) {
            return false;
        }
        if (!hkidNumWithoutCheckDigit.trim().matches("^[A-Za-z]{1,2}\\d{6}$")) {
            return false;
        }

        try {
            HkidNum hkidNum = new HkidNum(hkidNumWithoutCheckDigit);
            return hkidNum.getCheckDigit().equals(checkDigit.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Generates the check digit for the HKID number based on the current prefix and numerals.
     * This method is called internally whenever the prefix or numerals are set or modified.
     */
    private void genCheckDigit() {
        // Calculate Check Digit only if both Prefix and Numerals are set
        if (this.prefix == null || this.numerals == null) {
            return;
        }

        int checkDigitSum = 0;
        int prefixIdx = 0;
        int numericIdx = 0;
        for (int coefficient = 9; coefficient >= 2; coefficient--) {
            if (coefficient >= 8) {
                // Check Digit contribution of Prefix
                if (coefficient == 9 && this.prefix.length() == 1) {
                    // One letter Prefix, just add (36 % 11) * 9 % 11 (i.e. 5)
                    checkDigitSum += 5;
                    continue;
                }
                // Convert letter to number with 'A' as 10
                checkDigitSum += ((this.prefix.charAt(prefixIdx) - 'A' + 10) % 11 * coefficient) % 11;
                prefixIdx++;
            } else {
                // Check Digit contribution of Numerals
                checkDigitSum += ((this.numerals.charAt(numericIdx) - '0') % 11 * coefficient) % 11;
                numericIdx++;
            }
        }

        // Finalize check digit
        switch (checkDigitSum %= 11) {
            case (0):
                this.checkDigit = '0';
                break;
            case (1):
                // 11-1=10 (i.e. A)
                this.checkDigit = 'A';
                break;
            default:
                this.checkDigit = (char) ((11 - checkDigitSum) + '0');
                break;
        }
    }

    /**
     * Returns a string representation of the HKID number excluding the check digit.
     *
     * @return A string representation of the HKID number without the check digit.
     */
    @Override
    public String toString() {
        return toString(Format.WithoutCheckDigit);
    }

    /**
     * Returns a string representation of the HKID number in the specified format.
     *
     * @param format The desired format of the HKID number string as defined by the {@link Format} enum.
     * @return A formatted string representation of the HKID number.
     */
    public String toString(Format format) {
        if (format == null) {
            format = Format.WithoutCheckDigit;
        }
        return String.format(format.strFormat, this.prefix, this.numerals, this.checkDigit);
    }

    /**
     * Returns a string representation of the masked HKID number, leaving only the last three numerals visible.
     *
     * @return the masked HKID number, or {@code null} when {@code hkidNum} is null
     */
    public String getHMaskedStr() {
        return HkidNumUtil.maskHkidNum(this);
    }

    /**
     * Provides a descriptive text for the prefix of the HKID number based on predefined categories.
     *
     * @return A description of the prefix, or a fallback message when the prefix has no predefined category.
     */
    public String getPrefixDescription() {
        return getDefinedPrefix()
                .map(DefinedPrefix::getDescription)
                .orElse(String.format("No predefined description is available for prefix %s.", prefix));
    }

    /**
     * Provides a Traditional Chinese description for the prefix of the HKID number.
     *
     * @return A Traditional Chinese description of the prefix, or a fallback message when the prefix has no
     *         predefined category.
     */
    public String getPrefixTraditionalChineseDescription() {
        return getDefinedPrefix()
                .map(DefinedPrefix::getTraditionalChineseDescription)
                .orElse(String.format("字頭 %s 沒有預定義說明。", prefix));
    }

    /**
     * Returns the predefined metadata for this prefix, if any.
     *
     * @return The matching predefined prefix, or an empty optional for a valid but undefined prefix.
     */
    public Optional<DefinedPrefix> getDefinedPrefix() {
        return DefinedPrefix.fromPrefix(prefix);
    }

    // Getters and Setters

    /**
     * Returns the prefix of the HKID number.
     * The prefix consists of one or two uppercase letters.
     *
     * @return The prefix of the HKID number.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the prefix of the HKID number.
     * The prefix must consist of one or two uppercase letters (A-Z). This method also recalculates the check digit.
     *
     * @param prefix The new prefix to set for the HKID number. Cannot be null.
     * @throws InvalidHkidNumFormatException If the prefix is null, does not match the required format, or contains characters that are not allowed.
     */
    public void setPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            throw new InvalidHkidNumFormatException("Prefix of HKID Number cannot be null or empty.");
        }
        prefix = prefix.toUpperCase(Locale.ROOT);
        if (!prefix.matches("^[A-Z]{1,2}$")) {
            throw new InvalidHkidNumFormatException(INVALID_PREFIX_FORMAT_MESSAGE);
        }
        this.prefix = prefix;

        // Recalculate Check Digit
        genCheckDigit();
    }

    /**
     * Returns the numerals part of the HKID number.
     * This part consists of exactly six digits.
     *
     * @return The numerals part of the HKID number.
     */
    public String getNumerals() {
        return numerals;
    }

    /**
     * Sets the numerals part of the HKID number.
     * The numerals must consist of exactly six digits. This method also recalculates the check digit.
     *
     * @param numerals The new numerals to set for the HKID number. Cannot be null.
     * @throws InvalidHkidNumFormatException If the numerals is null or do not match the required format.
     */
    public void setNumerals(String numerals) {
        if (numerals == null || numerals.isEmpty()) {
            throw new InvalidHkidNumFormatException("Numerals of HKID Number cannot be null or empty.");
        }
        if (!numerals.matches("^\\d{6}$")) {
            throw new InvalidHkidNumFormatException("Numerals must be exactly 6 digits long.");
        }
        this.numerals = numerals;

        // Recalculate Check Digit
        genCheckDigit();
    }

    /**
     * Returns the check digit of the HKID number.
     * The check digit is calculated based on the prefix and numerals and can be either a digit (0-9) or the letter 'A'.
     *
     * @return The check digit of the HKID number.
     */
    public String getCheckDigit() {
        return checkDigit.toString();
    }

    // Enums

    /**
     * Enum defining the format options for string representation of HKID numbers.
     */
    public enum Format {
        WithoutCheckDigit("%s%s"),          // X123456 or XX123456
        WithoutParentheses("%s%s%c"),       // X123456A or XX123456A
        Complete("%s%s(%c)");               // X123456(A) or XX123456(A)

        private final String strFormat;
        Format(String strFormat) {
            this.strFormat = strFormat;
        }
    }

    /**
     * Enum for predefined HKID number prefixes, each associated with a specific category of HKID holders.
     * Reference from L/M (82) in RP 32/230/R of Registration of Persons Offices and Wikipedia
     */
    public enum DefinedPrefix {
        A("Original ID cards, issued between 1949 and 1962, most holders were born before 1950",
                "首批身份證，1949-1962年間在簽發，大部份人在1950年代之前出生。",
                DateBasis.FIRST_ISSUE, LocalDate.of(1949, 1, 1), LocalDate.of(1963, 1, 1)),
        B("Issued between 1955 and 1960 in city offices",
                "1955-1960年間在市區辦事處簽發。",
                DateBasis.FIRST_ISSUE, LocalDate.of(1955, 1, 1), LocalDate.of(1961, 1, 1)),
        C("Issued between 1960 and 1983 in NT offices, if a child most born between 1946 and 1971, principally HK born",
                "1960-1983年間在新界辦事處簽發，如小童申請人多於1946-1971年間出生，以香港出生者為主。",
                DateBasis.FIRST_ISSUE, LocalDate.of(1960, 1, 1), LocalDate.of(1984, 1, 1)),
        D("Issued between 1960 and 1983 at HK Island office, if a child most born between, principally HK born",
                "1960-1983年間在港島辦事處簽發，如小童申請人多於1946-1971年間出生，以香港出生者為主。",
                DateBasis.FIRST_ISSUE, LocalDate.of(1960, 1, 1), LocalDate.of(1984, 1, 1)),
        E("Issued between 1955 and 1969 in Kowloon offices, if a child most born between 1946 and 1962, principally HK born",
                "1955-1969年間在九龍辦事處簽發，如小童申請人多於1946-1962年間出生，以香港出生者為主。",
                DateBasis.FIRST_ISSUE, LocalDate.of(1955, 1, 1), LocalDate.of(1970, 1, 1)),
        F("First issue of a card commencing from 24 February 2020",
                "2020年2月24日起首次獲簽發身份證的人士。",
                DateBasis.FIRST_ISSUE, LocalDate.of(2020, 2, 24), null),
        G("Issued between 1967 and 1983 in Kowloon offices, if a child most born between 1956 and 1971",
                "1967-1983年間在九龍辦事處簽發，如小童申請人多於1956-1971年間出生。",
                DateBasis.FIRST_ISSUE, LocalDate.of(1967, 1, 1), LocalDate.of(1984, 1, 1)),
        H("Issued between 1979 and 1983 in HK Island offices, if a child most born between 1968 and 1971, principally HK born",
                "1979-1983年間在港島辦事處簽發，如小童申請人多於1968-1971年間出生，以香港以外出生者為主。",
                DateBasis.FIRST_ISSUE, LocalDate.of(1979, 1, 1), LocalDate.of(1984, 1, 1)),
        J("Consular officers after 23 October 1991",
                "1991年10月23日開始簽發予領事館僱員。"),
        K("First issue of an ID card between 28 March 1983 and 31 July 1990, if a child most born between 1972 and 1979",
                "1983年3月28日至1990年7月31日首次獲簽發身份證的人士，如小童申請人多於1972年至1979年6月在香港出生。",
                DateBasis.FIRST_ISSUE, LocalDate.of(1983, 3, 28), LocalDate.of(1990, 8, 1)),
        L("Issued between 1983 and 2003, used when computer system malfunctioned",
                "1983-2003年間簽發，電腦系統故障時使用的備用號碼。2003年6月23日起停用。"),
        M("First issue of ID card between 1 August 2011 and 23 February 2020",
                "2011年8月1日至2020年2月23日首次獲簽發身份證的人士，如小童申請人多於2000年起在香港以外出生。",
                DateBasis.FIRST_ISSUE, LocalDate.of(2011, 8, 1), LocalDate.of(2020, 2, 24)),
        N("Birth registered in Hong Kong after 1 June 2019",
                "2019年6月1日起於香港登記出生的人士。",
                DateBasis.BIRTH_REGISTRATION, LocalDate.of(2019, 6, 1), null),
        P("First issue of an ID card between 1 August 1990 and 27 December 2000, if a child most born between July and December 1979",
                "1990年8月1日至2000年12月27日首次獲簽發身份證的人士，如小童申請人多於1979年7月至12月在香港出生，或1980年代在香港以外出生。",
                DateBasis.FIRST_ISSUE, LocalDate.of(1990, 8, 1), LocalDate.of(2000, 12, 28)),
        R("First issue of an ID card between 28 December 2000 and 31 July 2011",
                "2000年12月28日至2011年7月31日首次獲簽發身份證的人士，以香港以外出生者為主。",
                DateBasis.FIRST_ISSUE, LocalDate.of(2000, 12, 28), LocalDate.of(2011, 8, 1)),
        S("Birth registered in Hong Kong between 1 April 2005 and 31 May 2019",
                "2005年4月1日至2019年5月31日於香港登記出生的人士。",
                DateBasis.BIRTH_REGISTRATION, LocalDate.of(2005, 4, 1), LocalDate.of(2019, 6, 1)),
        T("Issued between 1983 and 1997, used when computer system malfunctioned",
                "1983-1997年間簽發，電腦系統故障時使用的備用號碼。1997年7月1日起停用。"),
        V("Child under 11 issued with a \"Document of Identity for Visa Purposes\" between 28 March 1983 and 31 August 2003",
                "1983年3月28日至2003年8月31日獲簽發簽證身份書的11歲以下兒童。"),
        W("First issue to a foreign labourer or foreign domestic helper between 10 November 1989 and 1 January 2009",
                "1989年11月10日至2009年1月1日首次獲簽發身份證的外籍勞工及外籍家庭傭工。"),
        Y("Birth registered in Hong Kong between 1 January 1989 and 31 March 2005",
                "1989年1月1日至2005年3月31日於香港登記出生的人士。",
                DateBasis.BIRTH_REGISTRATION, LocalDate.of(1989, 1, 1), LocalDate.of(2005, 4, 1)),
        Z("Birth registered in Hong Kong between 1 January 1980 and 31 December 1988",
                "1980年1月1日至1988年12月31日於香港登記出生的人士。",
                DateBasis.BIRTH_REGISTRATION, LocalDate.of(1980, 1, 1), LocalDate.of(1989, 1, 1)),
        WX("First issue to a foreign labourer or foreign domestic helper since 2 January 2009",
                "2009年1月2日起首次獲簽發身份證的外籍勞工及外籍家庭傭工。"),
        XA("ID card issues to person without a Chinese name before 27 March 1983",
                "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
        XB("ID card issues to person without a Chinese name before 27 March 1983",
                "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
        XC("ID card issues to person without a Chinese name before 27 March 1983",
                "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
        XD("ID card issues to person without a Chinese name before 27 March 1983",
                "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
        XE("ID card issues to person without a Chinese name before 27 March 1983",
                "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
        XG("ID card issues to person without a Chinese name before 27 March 1983",
                "1983年3月27日前沒有中文姓名的新登記身份證人士。"),
        XH("ID card issues to person without a Chinese name before 27 March 1983",
                "1983年3月27日前沒有中文姓名的新登記身份證人士。");
        //U: Pseudo ID for neonatal born in public hospitals

        private enum DateBasis {
            BIRTH_REGISTRATION,
            FIRST_ISSUE
        }

        private final String description;       // Description in English
        private final String tcDescription;     // Description in Traditional Chinese
        private final LocalDate hongKongBirthRegistrationStartDate;
        private final LocalDate hongKongBirthRegistrationEndDateExclusive;
        private final LocalDate firstIssueStartDate;
        private final LocalDate firstIssueEndDateExclusive;

        DefinedPrefix(String description, String tcDescription) {
            this.description = description;
            this.tcDescription = tcDescription;
            this.hongKongBirthRegistrationStartDate = null;
            this.hongKongBirthRegistrationEndDateExclusive = null;
            this.firstIssueStartDate = null;
            this.firstIssueEndDateExclusive = null;
        }

        DefinedPrefix(String description,
                      String tcDescription,
                      DateBasis dateBasis,
                      LocalDate startDate,
                      LocalDate endDateExclusive) {
            if (dateBasis == null || startDate == null) {
                throw new IllegalArgumentException("Dated prefix metadata requires a basis and start date");
            }
            if (endDateExclusive != null && !endDateExclusive.isAfter(startDate)) {
                throw new IllegalArgumentException("Prefix metadata end date must be after start date");
            }
            this.description = description;
            this.tcDescription = tcDescription;
            this.hongKongBirthRegistrationStartDate = dateBasis == DateBasis.BIRTH_REGISTRATION
                    ? startDate : null;
            this.hongKongBirthRegistrationEndDateExclusive = dateBasis == DateBasis.BIRTH_REGISTRATION
                    ? endDateExclusive : null;
            this.firstIssueStartDate = dateBasis == DateBasis.FIRST_ISSUE ? startDate : null;
            this.firstIssueEndDateExclusive = dateBasis == DateBasis.FIRST_ISSUE ? endDateExclusive : null;
        }

        public String getDescription() {
            return description;
        }

        public String getTraditionalChineseDescription() {
            return tcDescription;
        }

        /**
         * Returns whether this prefix has an exact Hong Kong birth-registration period
         * containing the supplied date. Approximate historical birth ranges are not
         * treated as exact metadata by this method.
         */
        public boolean supportsHongKongBirthRegistrationDate(LocalDate birthRegistrationDate) {
            return birthRegistrationDate != null
                    && hongKongBirthRegistrationStartDate != null
                    && !birthRegistrationDate.isBefore(hongKongBirthRegistrationStartDate)
                    && (hongKongBirthRegistrationEndDateExclusive == null
                    || birthRegistrationDate.isBefore(hongKongBirthRegistrationEndDateExclusive));
        }

        /**
         * Finds the prefix whose exact Hong Kong birth-registration period contains
         * the supplied date.
         */
        public static Optional<DefinedPrefix> fromHongKongBirthRegistrationDate(
                LocalDate birthRegistrationDate) {
            for (DefinedPrefix definedPrefix : values()) {
                if (definedPrefix.supportsHongKongBirthRegistrationDate(birthRegistrationDate)) {
                    return Optional.of(definedPrefix);
                }
            }
            return Optional.empty();
        }

        /**
         * Returns whether at least one day in the supplied month falls within this
         * prefix's exact first-issue period.
         */
        public boolean supportsFirstIssueMonth(YearMonth firstIssueMonth) {
            return firstIssueMonth != null
                    && firstIssueStartDate != null
                    && !firstIssueMonth.atEndOfMonth().isBefore(firstIssueStartDate)
                    && (firstIssueEndDateExclusive == null
                    || firstIssueMonth.atDay(1).isBefore(firstIssueEndDateExclusive));
        }

        /**
         * Finds all standard prefixes whose exact first-issue period overlaps the
         * supplied month.
         */
        public static DefinedPrefix[] fromFirstIssueMonth(YearMonth firstIssueMonth) {
            return Arrays.stream(values())
                    .filter(prefix -> prefix.supportsFirstIssueMonth(firstIssueMonth))
                    .toArray(DefinedPrefix[]::new);
        }

        /**
         * Looks up predefined metadata for a prefix.
         *
         * @param prefix A one- or two-letter HKID prefix.
         * @return The matching predefined prefix, or an empty optional when no metadata is defined.
         */
        public static Optional<DefinedPrefix> fromPrefix(String prefix) {
            if (prefix == null) {
                return Optional.empty();
            }

            String normalizedPrefix = prefix.trim().toUpperCase(Locale.ROOT);
            for (DefinedPrefix definedPrefix : values()) {
                if (definedPrefix.name().equals(normalizedPrefix)) {
                    return Optional.of(definedPrefix);
                }
            }
            return Optional.empty();
        }
    }

    // Custom Exceptions

    /**
     * Exception thrown when an input HKID number does not conform to the expected format.
     * This includes scenarios where the HKID number is null, empty, lacks the correct number of digits,
     * or includes invalid characters in the prefix or numerals.
     * <p>
     * This exception is used to signal validation errors specifically related to the format of the HKID number,
     * allowing calling code to catch and handle format-specific issues distinctly from other types of input errors.
     */
    public static class InvalidHkidNumFormatException extends IllegalArgumentException {
        /**
         * Constructs an {@code InvalidHkidFormatException} with the specified detail message.
         * The message provides additional information about the invalid format encountered.
         *
         * @param message the detail message. The detail message is saved for later retrieval by the {@link Throwable#getMessage()} method.
         */
        public InvalidHkidNumFormatException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when the check digit of an HKID number is invalid.
     * This exception is raised during validation when the calculated check digit does not match
     * the check digit provided as part of the HKID number, indicating either a typo or incorrect
     * input data.
     * <p>
     * The use of this exception makes it possible for calling code to differentiate between general
     * format errors and specific check digit inaccuracies, facilitating more precise error handling
     * and feedback to users or calling processes.
     */
    public static class InvalidCheckDigitException extends IllegalArgumentException {
        /**
         * Constructs an {@code InvalidCheckDigitException} with the specified detail message.
         * The message contains information about the discrepancy between the expected and actual check digits.
         *
         * @param message the detail message. The detail message is saved for later retrieval by the {@link Throwable#getMessage()} method.
         */
        public InvalidCheckDigitException(String message) {
            super(message);
        }
    }
}
