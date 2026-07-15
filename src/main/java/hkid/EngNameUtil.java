package hkid;

import java.util.regex.Pattern;

/**
 * Utility methods for English names printed on HKID cards.
 */
public final class EngNameUtil {
    private static final Pattern ENGLISH_NAME_PART_PATTERN = Pattern.compile("[A-Za-z][A-Za-z .'-]*");

    private EngNameUtil() {
        throw new AssertionError("EngNameUtil cannot be instantiated");
    }

    public static boolean isValidNamePart(String value) {
        return value != null && ENGLISH_NAME_PART_PATTERN.matcher(value).matches();
    }

    static void validateNamePart(String value, String fieldName) {
        if (value != null && !isValidNamePart(value)) {
            throw new IllegalArgumentException(fieldName + " is not a valid English name part");
        }
    }
}
