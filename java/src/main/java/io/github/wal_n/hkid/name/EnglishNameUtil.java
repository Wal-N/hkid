package io.github.wal_n.hkid.name;

/**
 * Utility methods for English names printed on HKID cards.
 */
public final class EnglishNameUtil {
    private static final int MAX_LENGTH = 40;

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
        if (value == null || value.isEmpty()) {
            return false;
        }

        // Scan iteratively so long inputs do not grow the call stack.
        boolean previousWasLetter = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if ((character >= 'A' && character <= 'Z')
                    || (character >= 'a' && character <= 'z')) {
                previousWasLetter = true;
            } else if (previousWasLetter
                    && (character == ' ' || character == '.' || character == '\'' || character == '-')) {
                previousWasLetter = false;
            } else {
                return false;
            }
        }
        return previousWasLetter;
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

        return fullNameLength(surname, personalName) <= MAX_LENGTH
                && isValidNamePart(surname)
                && isValidOptionalNamePart(personalName);
    }

    static void validate(String surname, String personalName) {
        if (fullNameLength(surname, personalName) > MAX_LENGTH) {
            throw new IllegalArgumentException("English name longer than " + MAX_LENGTH + " characters");
        }
        if (!isValidOptionalNamePart(surname)) {
            throw new IllegalArgumentException("Invalid English surname");
        }
        if (!isValidOptionalNamePart(personalName)) {
            throw new IllegalArgumentException("Invalid English personal name");
        }
    }

    private static long fullNameLength(String surname, String personalName) {
        int surnameLength = surname != null ? surname.length() : 0;
        int personalNameLength = personalName != null ? personalName.length() : 0;
        return (long) surnameLength + personalNameLength
                + (surnameLength > 0 && personalNameLength > 0 ? 2 : 0);
    }

    private static boolean isValidOptionalNamePart(String value) {
        return value == null || value.isEmpty() || isValidNamePart(value);
    }
}
