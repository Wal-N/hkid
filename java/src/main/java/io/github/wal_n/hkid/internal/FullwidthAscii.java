package io.github.wal_n.hkid.internal;

/**
 * Explicit fullwidth Latin letter and digit conversion for text parsing.
 */
public final class FullwidthAscii {
    private FullwidthAscii() {
        throw new AssertionError("FullwidthAscii cannot be instantiated");
    }

    /**
     * Maps U+FF10 through U+FF19, U+FF21 through U+FF3A and U+FF41 through
     * U+FF5A to their ASCII counterparts. All other characters are retained,
     * including parentheses, so parsers can reject mixed-width pairs.
     *
     * @param value non-null input string
     * @return the input with only fullwidth Latin letters and digits converted
     */
    public static String toAscii(String value) {
        char[] characters = value.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            char character = characters[i];
            if ((character >= '\uFF10' && character <= '\uFF19')
                    || (character >= '\uFF21' && character <= '\uFF3A')
                    || (character >= '\uFF41' && character <= '\uFF5A')) {
                characters[i] = (char) (character - 0xFEE0);
            }
        }
        return new String(characters);
    }
}
