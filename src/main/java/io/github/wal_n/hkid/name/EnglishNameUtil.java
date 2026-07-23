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

    public static boolean isValidNamePart(String value) {
        return value != null && PART_PATTERN.matcher(value).matches();
    }

    static void validateNamePart(String value, String fieldName) {
        if (value != null && !value.isEmpty() && !isValidNamePart(value)) {
            throw new IllegalArgumentException(fieldName + " is not a valid English name part");
        }
    }
}
