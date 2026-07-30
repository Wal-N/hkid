package io.github.wal_n.hkid.name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable value object representing the Chinese name area of an HKID card.
 */
public final class ChineseName {
    /**
     * HKID cards reserve up to six printed Chinese characters for the Chinese name.
     */
    public static final int MAX_LENGTH = 6;

    private final String surname;
    private final String personalName;
    private final List<String> commercialCodes;

    /**
     * Creates an empty Chinese name with no commercial codes.
     */
    public ChineseName() {
        this("", "", Collections.emptyList());
    }

    /**
     * Creates a Chinese name without commercial codes.
     *
     * @param surname Chinese surname, or {@code null} for an empty surname
     * @param personalName Chinese personal name, or {@code null} for an empty personal name
     * @throws IllegalArgumentException if either part contains a non-Chinese character
     *         or the combined name exceeds {@link #MAX_LENGTH}
     */
    public ChineseName(String surname, String personalName) {
        this(surname, personalName, Collections.emptyList());
    }

    /**
     * Creates a Chinese name and its matching Chinese commercial codes.
     *
     * @param surname Chinese surname, or {@code null} for an empty surname
     * @param personalName Chinese personal name, or {@code null} for an empty personal name
     * @param commercialCodes four-digit codes in printed-name order, or {@code null} for none
     * @throws IllegalArgumentException if a name part, total length, commercial code,
     *         or code count is invalid
     */
    public ChineseName(String surname, String personalName, List<String> commercialCodes) {
        String normalizedSurname = Objects.toString(surname, "");
        String normalizedPersonalName = Objects.toString(personalName, "");
        List<String> normalizedCommercialCodes = commercialCodes == null
                ? Collections.emptyList()
                : new ArrayList<>(commercialCodes);

        ChineseNameUtil.validate(
                normalizedSurname, normalizedPersonalName, normalizedCommercialCodes);

        this.surname = normalizedSurname;
        this.personalName = normalizedPersonalName;
        this.commercialCodes = Collections.unmodifiableList(normalizedCommercialCodes);
    }

    /**
     * Returns the surname followed immediately by the personal name.
     *
     * @return the complete Chinese name
     */
    public String getFullName() {
        return surname + personalName;
    }

    /**
     * Returns the Chinese surname.
     *
     * @return the surname, possibly empty
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Returns the Chinese personal name.
     *
     * @return the personal name, possibly empty
     */
    public String getPersonalName() {
        return personalName;
    }

    /**
     * Returns the Chinese commercial codes in printed-name order.
     *
     * @return an unmodifiable list of four-digit commercial codes
     */
    public List<String> getCommercialCodes() {
        return commercialCodes;
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ChineseName)) {
            return false;
        }
        ChineseName other = (ChineseName) object;
        return surname.equals(other.surname)
                && personalName.equals(other.personalName)
                && commercialCodes.equals(other.commercialCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(surname, personalName, commercialCodes);
    }
}
