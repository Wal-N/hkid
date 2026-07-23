package io.github.wal_n.hkid.number;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Hong Kong Identity Card (HKID) number.
 * This class allows for generating, validating, and formatting HKID numbers according to the official specifications.
 * It supports creating HKID numbers with specific or random values and offers methods to retrieve or modify components of an HKID number.
 * <p>
 * Methods of this class can throw custom exceptions {@link InvalidHkidNumberFormatException} and {@link InvalidCheckDigitException}
 * to indicate problems with the provided HKID number format or check digit, respectively.
 */
public class HkidNumber {
    /**
     * The prefix of the HKID number, which can be one or two letters (A-Z).
     */
    private String prefix;

    /**
     * The six-digit numeral part of the HKID number.
     */
    private String numerals;

    private static final Pattern HKID_NUM_PATTERN = Pattern.compile("^([A-Z]{1,2})(\\d{6})(?:([\\dA])|\\(([\\dA])\\))?$");
    private static final String INVALID_HKID_NUM_FORMAT_MESSAGE = "Invalid format for HKID number.";
    private static final String INVALID_CHECK_DIGIT_MESSAGE = "Invalid check digit for HKID number.";
    private static final String INVALID_CHECK_DIGIT_FORMAT_MESSAGE = "Invalid check digit format.";
    private static final String INVALID_PREFIX_FORMAT_MESSAGE = "Invalid prefix format.";


    /**
     * Constructs a new {@code HkidNumber} instance by parsing the provided HKID number string.
     * The input string may include or exclude the check digit and parentheses around the check digit.
     *
     * @param hkidNumber The HKID number string to parse. Acceptable formats include "X123456(A)", "XX123456A", or "X123456".
     *                The check digit and parentheses are optional.
     * @throws InvalidHkidNumberFormatException If the input string is null, empty, does not match expected patterns,
     *                                    or contains characters that are not allowed.
     * @throws InvalidCheckDigitException If the provided check digit is incorrect.
     */
    public HkidNumber(String hkidNumber) {
        if (hkidNumber == null || hkidNumber.isEmpty()) {
            throw new InvalidHkidNumberFormatException("HKID number cannot be null or empty.");
        }
        hkidNumber = hkidNumber.trim().toUpperCase(Locale.ROOT);

        // Extract parts
        Matcher matcher = HKID_NUM_PATTERN.matcher(hkidNumber);
        if (matcher.matches()) {
            // Set Prefix, Numerals and generate Check Digit
            setPrefix(matcher.group(1));
            setNumerals(matcher.group(2));

            // Check input Check Digit is correct (if any)
            String inputCheckDigit = matcher.group(3) != null ? matcher.group(3) : (matcher.group(4) != null ? matcher.group(4) : null);
            if (inputCheckDigit != null && !inputCheckDigit.equals(getCheckDigit())) {
                throw new InvalidCheckDigitException(INVALID_CHECK_DIGIT_MESSAGE);
            }
        } else {
            throw new InvalidHkidNumberFormatException(INVALID_HKID_NUM_FORMAT_MESSAGE);
        }
    }

    /**
     * Constructs a new {@code HkidNumber} instance using the specified prefix and numerals.
     * This constructor automatically calculates and assigns the check digit based on the provided prefix and numerals.
     * The generated instance will represent a complete HKID number including a valid check digit.
     *
     * @param prefix The prefix part of the HKID number. It should consist of one or two alphabetical characters.
     * @param numerals The numerals part of the HKID number. It should consist of six digits.
     * @throws InvalidHkidNumberFormatException If either the prefix or numerals are null, empty, do not match expected patterns,
     *                                       or contain characters that are not allowed.
     * @throws InvalidCheckDigitException If the calculation of the check digit fails due to invalid input parameters.
     */
    public HkidNumber(String prefix, String numerals) {
        this(prefix, numerals, null);
    }

    /**
     * Constructs a new {@code HkidNumber} instance using the specified prefix, numerals, and check digit.
     * This constructor allows for the explicit specification of the check digit and verifies it against the calculated value.
     * It is useful for creating {@code HkidNumber} instances that need to match existing HKID numbers exactly, including their check digits.
     *
     * @param prefix The prefix part of the HKID number. It should consist of one or two alphabetical characters.
     * @param numerals The numerals part of the HKID number. It should consist of six digits.
     * @param checkDigit The check digit of the HKID number. It is a single digit or the letter 'A'. This parameter is optional;
     *                   if null or empty, the check digit will be automatically calculated.
     * @throws InvalidHkidNumberFormatException If either the prefix or numerals are null, empty, do not match expected patterns,
     *                                       or contain characters that are not allowed.
     * @throws InvalidCheckDigitException If the provided check digit is incorrect or if the calculation of the check digit fails due to invalid input parameters.
     */
    public HkidNumber(String prefix, String numerals, String checkDigit) {
        setPrefix(prefix);
        setNumerals(numerals);

        if (checkDigit == null || checkDigit.isEmpty()) {
            return;
        }

        String normalizedCheckDigit = checkDigit.toUpperCase(Locale.ROOT);
        if (!normalizedCheckDigit.matches("^[\\dA]$")) {
            throw new InvalidHkidNumberFormatException(INVALID_CHECK_DIGIT_FORMAT_MESSAGE);
        }
        if (!normalizedCheckDigit.equals(getCheckDigit())) {
            throw new InvalidCheckDigitException(INVALID_CHECK_DIGIT_MESSAGE);
        }
    }

    public static boolean validateCheckDigit(String hkidNumberWithoutCheckDigit, String checkDigit) {
        if (hkidNumberWithoutCheckDigit == null || checkDigit == null) {
            return false;
        }
        if (!hkidNumberWithoutCheckDigit.trim().matches("^[A-Za-z]{1,2}\\d{6}$")) {
            return false;
        }

        try {
            HkidNumber hkidNumber = new HkidNumber(hkidNumberWithoutCheckDigit);
            return hkidNumber.getCheckDigit().equals(checkDigit.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Calculates the check digit for the current prefix and numerals.
     *
     * @return The calculated check digit.
     */
    private char calculateCheckDigit() {
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
        int remainder = checkDigitSum % 11;
        switch (remainder) {
            case (0):
                return '0';
            case (1):
                // 11-1=10 (i.e. A)
                return 'A';
            default:
                return (char) ((11 - remainder) + '0');
        }
    }

    /**
     * Returns a string representation of the HKID number excluding the check digit.
     *
     * @return A string representation of the HKID number without the check digit.
     */
    @Override
    public String toString() {
        return toString(Format.WITHOUT_CHECK_DIGIT);
    }

    /**
     * Returns a string representation of the HKID number in the specified format.
     *
     * @param format The desired format of the HKID number string as defined by the {@link Format} enum.
     * @return A formatted string representation of the HKID number.
     */
    public String toString(Format format) {
        if (format == null) {
            format = Format.WITHOUT_CHECK_DIGIT;
        }
        return String.format(format.strFormat, this.prefix, this.numerals, calculateCheckDigit());
    }

    /**
     * Returns a string representation of the masked HKID number, leaving only the last three numerals visible.
     *
     * @return the masked HKID number
     */
    public String toMaskedString() {
        return HkidNumberUtil.maskHkidNumber(this);
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
     * The prefix must consist of one or two uppercase letters (A-Z).
     *
     * @param prefix The new prefix to set for the HKID number. Cannot be null.
     * @throws InvalidHkidNumberFormatException If the prefix is null, does not match the required format, or contains characters that are not allowed.
     */
    public void setPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            throw new InvalidHkidNumberFormatException("Prefix of HKID Number cannot be null or empty.");
        }
        prefix = prefix.toUpperCase(Locale.ROOT);
        if (!prefix.matches("^[A-Z]{1,2}$")) {
            throw new InvalidHkidNumberFormatException(INVALID_PREFIX_FORMAT_MESSAGE);
        }
        this.prefix = prefix;
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
     * The numerals must consist of exactly six digits.
     *
     * @param numerals The new numerals to set for the HKID number. Cannot be null.
     * @throws InvalidHkidNumberFormatException If the numerals is null or do not match the required format.
     */
    public void setNumerals(String numerals) {
        if (numerals == null || numerals.isEmpty()) {
            throw new InvalidHkidNumberFormatException("Numerals of HKID Number cannot be null or empty.");
        }
        if (!numerals.matches("^\\d{6}$")) {
            throw new InvalidHkidNumberFormatException("Numerals must be exactly 6 digits long.");
        }
        this.numerals = numerals;
    }

    /**
     * Returns the check digit of the HKID number.
     * The check digit is calculated based on the prefix and numerals and can be either a digit (0-9) or the letter 'A'.
     *
     * @return The check digit of the HKID number.
     */
    public String getCheckDigit() {
        return String.valueOf(calculateCheckDigit());
    }

    // Enums

    /**
     * Enum defining the format options for string representation of HKID numbers.
     */
    public enum Format {
        WITHOUT_CHECK_DIGIT("%s%s"),        // X123456 or XX123456
        WithoutParentheses("%s%s%c"),       // X123456A or XX123456A
        Complete("%s%s(%c)");               // X123456(A) or XX123456(A)

        private final String strFormat;
        Format(String strFormat) {
            this.strFormat = strFormat;
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
    public static class InvalidHkidNumberFormatException extends IllegalArgumentException {
        /**
         * Constructs an {@code InvalidHkidFormatException} with the specified detail message.
         * The message provides additional information about the invalid format encountered.
         *
         * @param message the detail message. The detail message is saved for later retrieval by the {@link Throwable#getMessage()} method.
         */
        public InvalidHkidNumberFormatException(String message) {
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
