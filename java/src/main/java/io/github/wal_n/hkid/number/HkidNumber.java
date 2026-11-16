package io.github.wal_n.hkid.number;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable HKID number with a one- or two-letter ASCII prefix and six ASCII digits.
 * Prefixes are stored in uppercase; the check digit is derived when requested.
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
     * Parses a prefix and six numerals, optionally followed by a check digit.
     * Fullwidth digits (U+FF10 through U+FF19) and Latin letters (U+FF21 through
     * U+FF3A and U+FF41 through U+FF5A) are converted to ASCII before parsing.
     * ASCII and fullwidth letters and digits may be mixed. Other Unicode
     * letters, digits and punctuation are not converted.
     * The check digit may be an ASCII digit or {@code A}/{@code a}, with or without
     * parentheses. Leading and trailing U+0009, U+000A, U+000D, U+0020, U+00A0
     * and U+3000 are removed. Zero or more U+0020, U+00A0 or U+3000 may separate
     * the prefix, six numerals and check digit. Each component must remain intact,
     * including any check-digit parentheses; internal tabs and line breaks are
     * rejected. Lowercase ASCII letters are normalized to uppercase.
     * Parentheses must be a matching ASCII U+0028/U+0029 pair or a matching
     * fullwidth U+FF08/U+FF09 pair. Mixed pairs are rejected, and formatted
     * output always uses ASCII parentheses.
     *
     * @param hkidNumber number such as {@code A123456}, {@code AB1234569}, or {@code A 123456 (3)}
     * @throws InvalidHkidNumberFormatException if the input is null, empty after trimming,
     *         or does not match the supported format
     * @throws InvalidCheckDigitException if a supplied check digit does not match
     */
    public HkidNumber(String hkidNumber) {
        String[] parts = HkidNumberUtil.parse(hkidNumber);
        this.prefix = parts[0];
        this.numerals = parts[1];
    }

    /**
     * Creates a number from its prefix and numerals. Neither argument is trimmed
     * or converted from fullwidth characters.
     *
     * @param prefix one or two ASCII letters, normalized to uppercase
     * @param numerals exactly six ASCII digits, including any leading zeros
     * @throws InvalidHkidNumberFormatException if either argument is null or has an invalid format
     */
    public HkidNumber(String prefix, String numerals) {
        this(prefix, numerals, null);
    }

    /**
     * Creates a number and validates any supplied check digit. Arguments are not
     * trimmed or converted from fullwidth characters.
     *
     * @param prefix one or two ASCII letters, normalized to uppercase
     * @param numerals exactly six ASCII digits, including any leading zeros
     * @param checkDigit one ASCII digit or {@code A}/{@code a}, without parentheses;
     *                   null or empty skips check-digit validation
     * @throws InvalidHkidNumberFormatException if the prefix or numerals are null or have
     *         an invalid format, or a non-empty check digit has an invalid format
     * @throws InvalidCheckDigitException if a supplied check digit does not match
     */
    public HkidNumber(String prefix, String numerals, String checkDigit) {
        this.prefix = HkidNumberUtil.normalizePrefix(prefix);
        this.numerals = HkidNumberUtil.validateNumerals(numerals);
        HkidNumberUtil.validateOptionalCheckDigit(this.prefix, this.numerals, checkDigit);
    }

    /**
     * Checks a supplied check digit against an HKID prefix and six numerals.
     * Both inputs have leading and trailing U+0009, U+000A, U+000D, U+0020,
     * U+00A0 and U+3000 removed. Zero or more U+0020, U+00A0 or U+3000 may
     * separate the prefix from the six numerals. Letter case is ignored.
     * Fullwidth digits (U+FF10 through U+FF19) and Latin letters (U+FF21 through
     * U+FF3A and U+FF41 through U+FF5A) are converted to ASCII in both inputs.
     * Other Unicode letters, digits and punctuation are not converted.
     *
     * @param hkidNumberWithoutCheckDigit one or two Latin letters followed by six digits,
     *                                   in ASCII, fullwidth or a mixture of both
     * @param checkDigit one ASCII digit or {@code A}/{@code a}, or its fullwidth form,
     *                   without parentheses
     * @return {@code true} when the check digit matches; {@code false} for null or invalid inputs
     */
    public static boolean isValidCheckDigit(String hkidNumberWithoutCheckDigit, String checkDigit) {
        return HkidNumberUtil.isValidCheckDigit(hkidNumberWithoutCheckDigit, checkDigit);
    }

    /**
     * Formats the number as shown on the card, including its check digit in ASCII parentheses.
     *
     * @return the number in {@link Format#COMPLETE} format
     */
    @Override
    public String toString() {
        return toString(null);
    }

    /**
     * Formats the number with an optional check digit and parentheses.
     *
     * @param format output format; {@code null} selects {@link Format#COMPLETE}
     * @return the formatted number
     */
    public String toString(Format format) {
        if (format == null) {
            format = Format.COMPLETE;
        }
        return String.format(
                format.strFormat,
                this.prefix,
                this.numerals,
                HkidNumberUtil.calculateCheckDigit(this.prefix, this.numerals));
    }

    /**
     * Masks the prefix, first three numerals, and check digit with asterisks.
     *
     * @return the masked HKID number
     */
    public String toMaskedString() {
        return HkidNumberUtil.toMaskedString(this);
    }

    /**
     * Returns the bundled English description of the prefix.
     *
     * @return the non-authoritative description, or a fallback message for an undefined prefix
     */
    public String getPrefixDescription() {
        return getDefinedPrefix()
                .map(DefinedPrefix::getDescription)
                .orElse(String.format("No predefined description is available for prefix %s.", prefix));
    }

    /**
     * Returns the bundled Traditional Chinese description of the prefix.
     *
     * @return the non-authoritative description, or a fallback message for an undefined prefix
     */
    public String getPrefixTraditionalChineseDescription() {
        return getDefinedPrefix()
                .map(DefinedPrefix::getTraditionalChineseDescription)
                .orElse(String.format("字頭 %s 沒有預定義說明。", prefix));
    }

    /**
     * Returns the predefined metadata for this prefix, if any.
     *
     * @return the matching predefined prefix, or an empty optional for an undefined prefix
     */
    public Optional<DefinedPrefix> getDefinedPrefix() {
        return DefinedPrefix.fromPrefix(prefix);
    }

    /**
     * Returns the normalized prefix.
     *
     * @return one or two uppercase ASCII letters
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the numeral portion, preserving leading zeros.
     *
     * @return exactly six ASCII digits
     */
    public String getNumerals() {
        return numerals;
    }

    /**
     * Calculates the check digit from the prefix and numerals.
     *
     * @return one ASCII digit or {@code A}
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

    /**
     * Output formats for HKID numbers.
     */
    public enum Format {
        /** Prefix and numerals only, for example {@code A123456}. */
        WITHOUT_CHECK_DIGIT("%s%s"),
        /** Prefix, numerals, and an unparenthesised check digit, for example {@code A1234563}. */
        WITHOUT_PARENTHESES("%s%s%c"),
        /** Complete card format with a parenthesised check digit, for example {@code A123456(3)}. */
        COMPLETE("%s%s(%c)");

        private final String strFormat;
        Format(String strFormat) {
            this.strFormat = strFormat;
        }
    }

    /**
     * Indicates a missing required value or invalid HKID input syntax, including
     * malformed check digits. A well-formed but incorrect check digit instead causes
     * {@link InvalidCheckDigitException}.
     */
    public static class InvalidHkidNumberFormatException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        /**
         * Creates a format exception.
         *
         * @param message detail message, or {@code null} for none
         */
        public InvalidHkidNumberFormatException(String message) {
            super(message);
        }
    }

    /**
     * Indicates that a well-formed supplied check digit does not match the calculated value.
     */
    public static class InvalidCheckDigitException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        /**
         * Creates a check-digit mismatch exception.
         *
         * @param message detail message, or {@code null} for none
         */
        public InvalidCheckDigitException(String message) {
            super(message);
        }
    }
}
