package hkid;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility methods for Chinese names printed on HKID cards.
 */
public final class ChiNameUtil {
    /**
     * HKID cards reserve up to six printed Chinese characters for the Chinese name.
     */
    public static final int MAX_NAME_LENGTH = 6;
    private static final Pattern TEXT_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");
    private static final Pattern COMMERCIAL_CODE_PATTERN = Pattern.compile("\\d{4}");

    private ChiNameUtil() {
        throw new AssertionError("ChiNameUtil cannot be instantiated");
    }

    public static boolean isChinese(String value) {
        return value != null && TEXT_PATTERN.matcher(value).matches();
    }

    public static boolean isValidCommercialCode(String code) {
        return code != null && COMMERCIAL_CODE_PATTERN.matcher(code).matches();
    }

    public static boolean isValidChiCommercialCode(List<String> codes) {
        return codes == null
                || codes.isEmpty()
                || (codes.size() <= MAX_NAME_LENGTH
                && codes.stream().allMatch(ChiNameUtil::isValidCommercialCode));
    }

    static void validateChinesePart(String value, String fieldName) {
        if (value != null && !isChinese(value)) {
            throw new IllegalArgumentException(fieldName + " is not Chinese");
        }
    }

    static void validateTotalLength(String surname, String personalName) {
        int totalLength = lengthOf(surname) + lengthOf(personalName);
        if (totalLength > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Chinese name longer than " + MAX_NAME_LENGTH + " characters");
        }
    }

    static void validateCommercialCodes(List<String> codes) {
        if (!isValidChiCommercialCode(codes)) {
            throw new IllegalArgumentException("Chinese commercial codes must be 4 digits each and contain at most "
                    + MAX_NAME_LENGTH + " entries");
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

    private static int lengthOf(String value) {
        return value != null ? value.length() : 0;
    }
}
