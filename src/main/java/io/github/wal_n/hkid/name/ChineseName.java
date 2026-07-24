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

    public ChineseName() {
        this("", "", Collections.<String>emptyList());
    }

    public ChineseName(String surname, String personalName) {
        this(surname, personalName, Collections.<String>emptyList());
    }

    public ChineseName(String surname, String personalName, List<String> commercialCodes) {
        String normalizedSurname = Objects.toString(surname, "");
        String normalizedPersonalName = Objects.toString(personalName, "");
        List<String> normalizedCommercialCodes = commercialCodes == null
                ? Collections.<String>emptyList()
                : new ArrayList<>(commercialCodes);

        ChineseNameUtil.validateChinesePart(normalizedSurname, "Surname");
        ChineseNameUtil.validateChinesePart(normalizedPersonalName, "Personal name");
        ChineseNameUtil.validateTotalLength(normalizedSurname, normalizedPersonalName);
        ChineseNameUtil.validateCommercialCodes(normalizedCommercialCodes);
        ChineseNameUtil.validateCommercialCodeCount(
                normalizedSurname, normalizedPersonalName, normalizedCommercialCodes);

        this.surname = normalizedSurname;
        this.personalName = normalizedPersonalName;
        this.commercialCodes = Collections.unmodifiableList(normalizedCommercialCodes);
    }

    public String getFullName() {
        return surname + personalName;
    }

    public String getSurname() {
        return surname;
    }

    public String getPersonalName() {
        return personalName;
    }

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
