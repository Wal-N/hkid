package io.github.wal_n.hkid.number;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for working with HKID numbers.
 */
public final class HkidNumberUtil {
    private static final Pattern HKID_NUMBER_PATTERN =
            Pattern.compile("^([A-Z]{1,2})(\\d{6})(?:([\\dA])|\\(([\\dA])\\))?$");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[A-Z]{1,2}$");
    private static final Pattern NUMERALS_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern CHECK_DIGIT_PATTERN = Pattern.compile("^[\\dA]$");
    private static final Pattern WITHOUT_CHECK_DIGIT_PATTERN =
            Pattern.compile("^([A-Za-z]{1,2})(\\d{6})$");

    private static final String INVALID_HKID_NUMBER_FORMAT_MESSAGE =
            "Invalid format for HKID number.";
    private static final String INVALID_CHECK_DIGIT_MESSAGE =
            "Invalid check digit for HKID number.";
    private static final String INVALID_CHECK_DIGIT_FORMAT_MESSAGE =
            "Invalid check digit format.";
    private static final String INVALID_PREFIX_FORMAT_MESSAGE =
            "Invalid prefix format.";

    private HkidNumberUtil() {
        throw new AssertionError("HkidNumberUtil cannot be instantiated");
    }

    /**
     * Tests whether a complete or check-digit-free HKID number can be parsed.
     *
     * @param hkidNumber HKID number in a format accepted by {@link HkidNumber}
     * @return {@code true} when the format and any supplied check digit are valid
     */
    public static boolean isValid(String hkidNumber) {
        try {
            parse(hkidNumber);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks a supplied check digit against an HKID prefix and six numerals.
     *
     * @param hkidNumberWithoutCheckDigit one- or two-letter prefix followed by six numerals
     * @param checkDigit check digit to test, as a decimal digit or {@code A}
     * @return {@code true} when both inputs are valid and the check digit matches
     */
    public static boolean validateCheckDigit(
            String hkidNumberWithoutCheckDigit, String checkDigit) {
        if (hkidNumberWithoutCheckDigit == null || checkDigit == null) {
            return false;
        }

        Matcher matcher =
                WITHOUT_CHECK_DIGIT_PATTERN.matcher(hkidNumberWithoutCheckDigit.trim());
        if (!matcher.matches()) {
            return false;
        }

        String normalizedCheckDigit = checkDigit.trim().toUpperCase(Locale.ROOT);
        return CHECK_DIGIT_PATTERN.matcher(normalizedCheckDigit).matches()
                && normalizedCheckDigit.equals(String.valueOf(calculateCheckDigit(
                        matcher.group(1).toUpperCase(Locale.ROOT), matcher.group(2))));
    }

    /**
     * Generates an HKID number using a predefined prefix.
     *
     * @return a random HKID number with a predefined prefix
     */
    public static HkidNumber generateRandomHkidNumber() {
        return generateRandomHkidNumber(true);
    }

    /**
     * Generates an HKID number, optionally restricted to predefined prefixes.
     *
     * @param onlyDefinedPrefix whether to restrict selection to {@link DefinedPrefix} values
     * @return a random HKID number
     */
    public static HkidNumber generateRandomHkidNumber(boolean onlyDefinedPrefix) {
        return generateRandomHkidNumber(onlyDefinedPrefix, ThreadLocalRandom.current());
    }

    /**
     * Masks an HKID number, leaving only the last three numerals visible.
     * The prefix, first three numerals, and check digit are replaced by asterisks.
     *
     * @param hkidNumber HKID number to mask
     * @return the masked HKID number, or {@code null} when {@code hkidNumber} is null
     */
    public static String maskHkidNumber(HkidNumber hkidNumber) {
        if (hkidNumber == null) {
            return null;
        }
        String prefixAndLeadingNumeralsMask = hkidNumber.getPrefix().length() == 2 ? "*****" : "****";
        return prefixAndLeadingNumeralsMask + hkidNumber.getNumerals().substring(3) + "(*)";
    }

    /**
     * Generates an HKID number from caller-controlled random state and allowed prefixes.
     *
     * @param random random generator used to select the prefix and numerals
     * @param allowedPrefixes non-empty set of prefixes eligible for selection
     * @return a random HKID number using one of {@code allowedPrefixes}
     * @throws IllegalArgumentException if the random generator or prefix set is null,
     *         the prefix set is empty, or it contains {@code null}
     */
    public static HkidNumber generateRandomHkidNumber(
            Random random, DefinedPrefix... allowedPrefixes) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null");
        }
        if (allowedPrefixes == null || allowedPrefixes.length == 0) {
            throw new IllegalArgumentException("At least one allowed prefix is required");
        }
        for (DefinedPrefix allowedPrefix : allowedPrefixes) {
            if (allowedPrefix == null) {
                throw new IllegalArgumentException("Allowed prefixes cannot contain null");
            }
        }

        DefinedPrefix prefix = allowedPrefixes[random.nextInt(allowedPrefixes.length)];
        return buildRandomHkidNumber(prefix.name(), random);
    }

    private static HkidNumber generateRandomHkidNumber(boolean onlyDefinedPrefix, Random random) {
        String prefix;

        if (onlyDefinedPrefix) {
            DefinedPrefix[] prefixes = DefinedPrefix.values();
            prefix = prefixes[random.nextInt(prefixes.length)].name();
        } else {
            int prefixLength = random.nextInt(2) + 1;
            StringBuilder builder = new StringBuilder(prefixLength);
            for (int i = 0; i < prefixLength; i++) {
                builder.append((char) ('A' + random.nextInt('Z' - 'A' + 1)));
            }
            prefix = builder.toString();
        }

        return buildRandomHkidNumber(prefix, random);
    }

    private static HkidNumber buildRandomHkidNumber(String prefix, Random random) {
        String numerals = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        return new HkidNumber(prefix, numerals);
    }

    static String[] parse(String hkidNumber) {
        if (hkidNumber == null || hkidNumber.isEmpty()) {
            throw new HkidNumber.InvalidHkidNumberFormatException(
                    "HKID number cannot be null or empty.");
        }

        Matcher matcher =
                HKID_NUMBER_PATTERN.matcher(hkidNumber.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new HkidNumber.InvalidHkidNumberFormatException(
                    INVALID_HKID_NUMBER_FORMAT_MESSAGE);
        }

        String prefix = normalizePrefix(matcher.group(1));
        String numerals = validateNumerals(matcher.group(2));
        String checkDigit = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
        validateOptionalCheckDigit(prefix, numerals, checkDigit);
        return new String[]{prefix, numerals};
    }

    static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            throw new HkidNumber.InvalidHkidNumberFormatException(
                    "Prefix of HKID Number cannot be null or empty.");
        }

        String normalizedPrefix = prefix.toUpperCase(Locale.ROOT);
        if (!PREFIX_PATTERN.matcher(normalizedPrefix).matches()) {
            throw new HkidNumber.InvalidHkidNumberFormatException(
                    INVALID_PREFIX_FORMAT_MESSAGE);
        }
        return normalizedPrefix;
    }

    static String validateNumerals(String numerals) {
        if (numerals == null || numerals.isEmpty()) {
            throw new HkidNumber.InvalidHkidNumberFormatException(
                    "Numerals of HKID Number cannot be null or empty.");
        }
        if (!NUMERALS_PATTERN.matcher(numerals).matches()) {
            throw new HkidNumber.InvalidHkidNumberFormatException(
                    "Numerals must be exactly 6 digits long.");
        }
        return numerals;
    }

    static void validateOptionalCheckDigit(
            String prefix, String numerals, String checkDigit) {
        if (checkDigit == null || checkDigit.isEmpty()) {
            return;
        }

        String normalizedCheckDigit = checkDigit.toUpperCase(Locale.ROOT);
        if (!CHECK_DIGIT_PATTERN.matcher(normalizedCheckDigit).matches()) {
            throw new HkidNumber.InvalidHkidNumberFormatException(
                    INVALID_CHECK_DIGIT_FORMAT_MESSAGE);
        }
        if (normalizedCheckDigit.charAt(0) != calculateCheckDigit(prefix, numerals)) {
            throw new HkidNumber.InvalidCheckDigitException(INVALID_CHECK_DIGIT_MESSAGE);
        }
    }

    static char calculateCheckDigit(String prefix, String numerals) {
        int checkDigitSum = 0;
        int prefixIndex = 0;
        int numeralIndex = 0;
        for (int coefficient = 9; coefficient >= 2; coefficient--) {
            if (coefficient >= 8) {
                if (coefficient == 9 && prefix.length() == 1) {
                    checkDigitSum += 5;
                    continue;
                }
                checkDigitSum +=
                        ((prefix.charAt(prefixIndex) - 'A' + 10) % 11 * coefficient) % 11;
                prefixIndex++;
            } else {
                checkDigitSum +=
                        ((numerals.charAt(numeralIndex) - '0') % 11 * coefficient) % 11;
                numeralIndex++;
            }
        }

        int remainder = checkDigitSum % 11;
        switch (remainder) {
            case 0:
                return '0';
            case 1:
                return 'A';
            default:
                return (char) ((11 - remainder) + '0');
        }
    }
}
