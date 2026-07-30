package io.github.wal_n.hkid.number;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a Hong Kong Identity Card (HKID) number.
 * This immutable value object validates and formats HKID numbers according to the official specifications.
 * Create a new instance when a different prefix or numeral value is required.
 * <p>
 * Methods of this class can throw custom exceptions {@link InvalidHkidNumberFormatException} and {@link InvalidCheckDigitException}
 * to indicate problems with the provided HKID number format or check digit, respectively.
 */
public final class HkidNumber {
    /**
     * The prefix of the HKID number, which can be one or two letters (A-Z).
     */
    private final String prefix;

    /**
     * The six-digit numeral part of the HKID number.
     */
    private final String numerals;

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
        String[] parts = HkidNumberUtil.parse(hkidNumber);
        this.prefix = parts[0];
        this.numerals = parts[1];
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
        this.prefix = HkidNumberUtil.normalizePrefix(prefix);
        this.numerals = HkidNumberUtil.validateNumerals(numerals);
        HkidNumberUtil.validateOptionalCheckDigit(this.prefix, this.numerals, checkDigit);
    }

    /**
     * Checks a supplied check digit against an HKID prefix and six numerals.
     *
     * @param hkidNumberWithoutCheckDigit one- or two-letter prefix followed by six numerals
     * @param checkDigit check digit to test, as a decimal digit or {@code A}
     * @return {@code true} when both inputs are valid and the check digit matches
     */
    public static boolean validateCheckDigit(String hkidNumberWithoutCheckDigit, String checkDigit) {
        return HkidNumberUtil.validateCheckDigit(hkidNumberWithoutCheckDigit, checkDigit);
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
        return String.format(
                format.strFormat,
                this.prefix,
                this.numerals,
                HkidNumberUtil.calculateCheckDigit(this.prefix, this.numerals));
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
     * Returns the numerals part of the HKID number.
     * This part consists of exactly six digits.
     *
     * @return The numerals part of the HKID number.
     */
    public String getNumerals() {
        return numerals;
    }

    /**
     * Returns the check digit of the HKID number.
     * The check digit is calculated based on the prefix and numerals and can be either a digit (0-9) or the letter 'A'.
     *
     * @return The check digit of the HKID number.
     */
    public String getCheckDigit() {
        return String.valueOf(HkidNumberUtil.calculateCheckDigit(prefix, numerals));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof HkidNumber)) {
            return false;
        }
        HkidNumber other = (HkidNumber) object;
        return prefix.equals(other.prefix) && numerals.equals(other.numerals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, numerals);
    }

    // Enums

    /**
     * Enum defining the format options for string representation of HKID numbers.
     */
    public enum Format {
        /** Prefix and numerals only, for example {@code X123456}. */
        WITHOUT_CHECK_DIGIT("%s%s"),        // X123456 or XX123456
        /** Prefix, numerals, and an unparenthesised check digit, for example {@code X123456A}. */
        WITHOUT_PARENTHESES("%s%s%c"),       // X123456A or XX123456A
        /** Complete card format with a parenthesised check digit, for example {@code X123456(A)}. */
        COMPLETE("%s%s(%c)");               // X123456(A) or XX123456(A)

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
        private static final long serialVersionUID = 1L;

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
        private static final long serialVersionUID = 1L;

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
