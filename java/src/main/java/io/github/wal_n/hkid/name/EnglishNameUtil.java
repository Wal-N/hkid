package io.github.wal_n.hkid.name;

import java.util.regex.Pattern;

/**
 * Utility methods for English names printed on HKID cards.
 */
public final class EnglishNameUtil {
    private static final int MAX_LENGTH = 40;
    private static final Pattern PART_PATTERN = Pattern.compile("[A-Za-z]+(?:[ .'-][A-Za-z]+)*");

    private EnglishNameUtil() {
        throw new AssertionError("EnglishNameUtil cannot be instantiated");
    }

    /**
     * Tests whether a non-empty name part contains only supported Latin letters,
     * with spaces, periods, apostrophes, or hyphens between letter groups.
     *
     * <p>This checks character syntax only, without enforcing the HKID length limit.</p>
     *
     * @param value name part to inspect
     * @return {@code true} when the value is a valid English name part
     */
    public static boolean isValidNamePart(String value) {
        return value != null && PART_PATTERN.matcher(value).matches();
    }

    /**
     * Tests whether English surname and personal-name parts form a valid HKID name.
     *
     * <p>A surname is required; the personal name is optional. The formatted name
     * must not exceed 40 characters, including the comma and space between the
     * surname and a non-empty personal name.</p>
     *
     * @param surname required English surname
     * @param personalName English personal name, or {@code null} for an empty personal name
     * @return {@code true} when the surname is present, all non-empty parts have
     *         valid character syntax, and the formatted name fits the HKID limit
     */
    public static boolean isValid(String surname, String personalName) {
        if (surname == null || surname.isEmpty()) {
            return false;
        }

        long fullNameLength = surname.length();
        if (personalName != null && !personalName.isEmpty()) {
            fullNameLength += 2L + personalName.length();
        }
        return fullNameLength <= MAX_LENGTH
                && isValidNamePart(surname)
                && isValidOptionalNamePart(personalName);
    }

    static void validate(String surname, String personalName) {
        if (!isValidOptionalNamePart(surname)) {
            throw new IllegalArgumentException("Invalid English surname");
        }
        if (!isValidOptionalNamePart(personalName)) {
            throw new IllegalArgumentException("Invalid English personal name");
        }
    }

    private static boolean isValidOptionalNamePart(String value) {
        return value == null || value.isEmpty() || isValidNamePart(value);
    }
}
