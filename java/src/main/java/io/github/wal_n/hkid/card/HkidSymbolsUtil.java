package io.github.wal_n.hkid.card;

/**
 * Utility methods for current smart-HKID symbol codes.
 */
public final class HkidSymbolsUtil {
    private HkidSymbolsUtil() {
        throw new AssertionError("HkidSymbolsUtil cannot be instantiated");
    }

    /**
     * Tests whether a concatenated current smart-HKID symbol string can be parsed.
     *
     * @param value printed symbol string to test
     * @return {@code true} when the string contains only supported, non-duplicate,
     *         non-conflicting symbols
     */
    public static boolean isValid(String value) {
        try {
            HkidSymbols.parse(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
