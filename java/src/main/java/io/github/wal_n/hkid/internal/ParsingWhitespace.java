package io.github.wal_n.hkid.internal;

/**
 * Explicit whitespace rules for HKID number and symbol text input.
 */
public final class ParsingWhitespace {
    /** Regex character class for a space between complete input components. */
    public static final String COMPONENT_SEPARATOR_REGEX = "[ \u00A0\u3000]";

    private ParsingWhitespace() {
        throw new AssertionError("ParsingWhitespace cannot be instantiated");
    }

    /**
     * Removes only U+0009, U+000A, U+000D, U+0020, U+00A0 and U+3000
     * from the beginning and end of an input string.
     *
     * @param value non-null input string
     * @return the input with the specified outer characters removed
     */
    public static String trimOuter(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isOuterWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isOuterWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    /**
     * Tests whether a character may separate complete input components.
     *
     * @param value character to test
     * @return true only for U+0020, U+00A0 or U+3000
     */
    public static boolean isComponentSeparator(char value) {
        return value == ' ' || value == '\u00A0' || value == '\u3000';
    }

    private static boolean isOuterWhitespace(char value) {
        return isComponentSeparator(value) || value == '\t' || value == '\n' || value == '\r';
    }
}
