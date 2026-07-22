package hkid;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility methods for Chinese names printed on HKID cards.
 */
public final class ChiNameUtil {
    private static final Pattern COMMERCIAL_CODE_PATTERN = Pattern.compile("\\d{4}");

    /**
     * Inclusive ranges from Unicode 15.1's Unified_Ideograph property. Keeping
     * this table in the application makes validation independent of the Unicode
     * version bundled with the JVM.
     */
    private static final int[] UNIFIED_IDEOGRAPH_RANGES = {
            0x3400, 0x4DBF,   // CJK Unified Ideographs Extension A (㐀–䶿)
            0x4E00, 0x9FFF,   // CJK Unified Ideographs (一–鿿)

            // Twelve compatibility ideographs explicitly included in
            // Unicode 15.1's Unified_Ideograph property.
            0xFA0E, 0xFA0F,
            0xFA11, 0xFA11,
            0xFA13, 0xFA14,
            0xFA1F, 0xFA1F,
            0xFA21, 0xFA21,
            0xFA23, 0xFA24,
            0xFA27, 0xFA29,

            0x20000, 0x2A6DF, // CJK Unified Ideographs Extension B
            0x2A700, 0x2B739, // CJK Unified Ideographs Extension C
            0x2B740, 0x2B81D, // CJK Unified Ideographs Extension D
            0x2B820, 0x2CEA1, // CJK Unified Ideographs Extension E
            0x2CEB0, 0x2EBE0, // CJK Unified Ideographs Extension F (includes 𭉝)
            0x2EBF0, 0x2EE5D, // CJK Unified Ideographs Extension I
            0x30000, 0x3134A, // CJK Unified Ideographs Extension G
            0x31350, 0x323AF  // CJK Unified Ideographs Extension H
    };

    private ChiNameUtil() {
        throw new AssertionError("ChiNameUtil cannot be instantiated");
    }

    /**
     * Returns whether the value consists only of code points in Unicode 15.1's
     * Unified_Ideograph repertoire. This includes supplementary-plane and HKSCS
     * ideographs, but excludes compatibility forms and Han-script symbols that
     * are not members of that property.
     */
    public static boolean isChinese(String value) {
        return value != null
                && !value.isEmpty()
                && value.codePoints().allMatch(ChiNameUtil::isUnifiedIdeograph);
    }

    public static boolean isValidCommercialCode(String code) {
        return code != null && COMMERCIAL_CODE_PATTERN.matcher(code).matches();
    }

    public static boolean isValidChiCommercialCode(List<String> codes) {
        return codes == null
                || codes.isEmpty()
                || (codes.size() <= ChiName.MAX_LENGTH
                && codes.stream().allMatch(ChiNameUtil::isValidCommercialCode));
    }

    static void validateChinesePart(String value, String fieldName) {
        if (value != null && !value.isEmpty() && !isChinese(value)) {
            throw new IllegalArgumentException(fieldName + " is not Chinese");
        }
    }

    static void validateTotalLength(String surname, String personalName) {
        int totalLength = lengthOf(surname) + lengthOf(personalName);
        if (totalLength > ChiName.MAX_LENGTH) {
            throw new IllegalArgumentException("Chinese name longer than " + ChiName.MAX_LENGTH + " characters");
        }
    }

    static void validateCommercialCodes(List<String> codes) {
        if (!isValidChiCommercialCode(codes)) {
            throw new IllegalArgumentException("Chinese commercial codes must be 4 digits each and contain at most "
                    + ChiName.MAX_LENGTH + " entries");
        }
    }

    static void validateCommercialCodeCount(String surname, String personalName, List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return;
        }

        int nameLength = lengthOf(surname) + lengthOf(personalName);
        if (nameLength > 0 && codes.size() != nameLength) {
            throw new IllegalArgumentException("Chinese commercial code count must match Chinese name length");
        }
    }

    static int lengthOf(String value) {
        return value != null ? value.codePointCount(0, value.length()) : 0;
    }

    private static boolean isUnifiedIdeograph(int codePoint) {
        for (int i = 0; i < UNIFIED_IDEOGRAPH_RANGES.length; i += 2) {
            if (codePoint >= UNIFIED_IDEOGRAPH_RANGES[i]
                    && codePoint <= UNIFIED_IDEOGRAPH_RANGES[i + 1]) {
                return true;
            }
        }
        return false;
    }
}
