package io.github.wal_n.hkid.name;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility methods for Chinese names printed on HKID cards.
 */
public final class ChineseNameUtil {
    private static final Pattern COMMERCIAL_CODE_PATTERN = Pattern.compile("\\d{4}");

    private ChineseNameUtil() {
        throw new AssertionError("ChineseNameUtil cannot be instantiated");
    }

    /**
     * Returns whether the value consists only of letters in the Han script,
     * according to the Unicode character data provided by the running Java
     * platform. This includes supplementary-plane and compatibility ideographs
     * supported by that runtime, but excludes Han-script symbols that are not
     * letters. The supported repertoire may therefore vary by Java version.
     *
     * @param value value to inspect
     * @return {@code true} when the value is non-empty and contains only supported
     *         unified ideographs
     */
    public static boolean isChinese(String value) {
        return value != null
                && !value.isEmpty()
                && value.codePoints().allMatch(ChineseNameUtil::isHanLetter);
    }

    /**
     * Tests whether Chinese surname and personal-name parts form a valid HKID name.
     *
     * @param surname Chinese surname, or {@code null} for an empty surname
     * @param personalName Chinese personal name, or {@code null} for an empty personal name
     * @return {@code true} when both parts contain only Chinese characters and
     *         their combined length does not exceed the HKID name-area limit
     */
    public static boolean isValid(String surname, String personalName) {
        return isValidOptionalChinesePart(surname)
                && isValidOptionalChinesePart(personalName)
                && lengthOf(surname) + lengthOf(personalName) <= ChineseName.MAX_LENGTH;
    }

    /**
     * Tests whether Chinese surname and personal-name parts and their commercial
     * codes form a valid HKID name.
     *
     * @param surname Chinese surname, or {@code null} for an empty surname
     * @param personalName Chinese personal name, or {@code null} for an empty personal name
     * @param commercialCodes four-digit codes in printed-name order, or {@code null} for none
     * @return {@code true} when the name is valid and any supplied commercial-code
     *         count matches the name length
     */
    public static boolean isValid(
            String surname, String personalName, List<String> commercialCodes) {
        return isValid(surname, personalName)
                && isValidChineseCommercialCodes(commercialCodes)
                && (commercialCodes == null
                || commercialCodes.isEmpty()
                || commercialCodes.size() == lengthOf(surname) + lengthOf(personalName));
    }

    /**
     * Tests whether a value is one four-digit Chinese commercial code.
     *
     * @param code code to inspect
     * @return {@code true} when {@code code} contains exactly four decimal digits
     */
    public static boolean isValidCommercialCode(String code) {
        return code != null && COMMERCIAL_CODE_PATTERN.matcher(code).matches();
    }

    /**
     * Tests a list of Chinese commercial codes against the HKID name-area limit.
     *
     * @param codes codes to inspect; {@code null} is treated as no codes
     * @return {@code true} when there are at most {@link ChineseName#MAX_LENGTH}
     *         entries and every entry is a valid four-digit code
     */
    public static boolean isValidChineseCommercialCodes(List<String> codes) {
        return codes == null
                || codes.isEmpty()
                || (codes.size() <= ChineseName.MAX_LENGTH
                && codes.stream().allMatch(ChineseNameUtil::isValidCommercialCode));
    }

    static void validate(
            String surname, String personalName, List<String> commercialCodes) {
        if (!isValidOptionalChinesePart(surname)) {
            throw new IllegalArgumentException("Invalid Chinese surname");
        }
        if (!isValidOptionalChinesePart(personalName)) {
            throw new IllegalArgumentException("Invalid Chinese personal name");
        }
        validateTotalLength(surname, personalName);
        validateCommercialCodes(commercialCodes);
        validateCommercialCodeCount(surname, personalName, commercialCodes);
    }

    static void validateTotalLength(String surname, String personalName) {
        int totalLength = lengthOf(surname) + lengthOf(personalName);
        if (totalLength > ChineseName.MAX_LENGTH) {
            throw new IllegalArgumentException("Chinese name longer than " + ChineseName.MAX_LENGTH + " characters");
        }
    }

    static void validateCommercialCodes(List<String> codes) {
        if (!isValidChineseCommercialCodes(codes)) {
            throw new IllegalArgumentException("Chinese commercial codes must be 4 digits each and contain at most "
                    + ChineseName.MAX_LENGTH + " entries");
        }
    }

    static void validateCommercialCodeCount(String surname, String personalName, List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return;
        }

        int nameLength = lengthOf(surname) + lengthOf(personalName);
        if (codes.size() != nameLength) {
            throw new IllegalArgumentException("Chinese commercial code count must match Chinese name length");
        }
    }

    static int lengthOf(String value) {
        return value != null ? value.codePointCount(0, value.length()) : 0;
    }

    private static boolean isValidOptionalChinesePart(String value) {
        return value == null || value.isEmpty() || isChinese(value);
    }

    private static boolean isHanLetter(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN
                && Character.isLetter(codePoint);
    }
}
