package io.github.wal_n.hkid.name;

import java.util.regex.Pattern;

/**
 * Utility methods for English names printed on HKID cards.
 */
public final class EnglishNameUtil {
    private static final Pattern PART_PATTERN = Pattern.compile("[A-Za-z]+(?:[ .'-][A-Za-z]+)*");

    private EnglishNameUtil() {
        throw new AssertionError("EnglishNameUtil cannot be instantiated");
    }

    /**
     * Tests whether a non-empty name part contains only supported Latin letters,
     * with spaces, periods, apostrophes, or hyphens between letter groups.
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
     * @param surname English surname, or {@code null} for an empty surname
     * @param personalName English personal name, or {@code null} for an empty personal name
     * @return {@code true} when both non-empty parts contain only supported characters
     */
    public static boolean isValid(String surname, String personalName) {
        return isValidOptionalNamePart(surname) && isValidOptionalNamePart(personalName);
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
