package io.github.wal_n.hkid.name;

import java.util.Objects;

/**
 * Immutable value object representing the English name area of an HKID card.
 */
public final class EnglishName {
    private static final String FORMAT = "%s, %s";

    private final String surname;
    private final String personalName;

    /**
     * Creates an empty English name.
     */
    public EnglishName() {
        this("", "");
    }

    /**
     * Creates an English name from its surname and personal-name portions.
     *
     * @param surname English surname, or {@code null} for an empty surname
     * @param personalName English personal name, or {@code null} for an empty personal name
     * @throws IllegalArgumentException if either non-empty part contains unsupported characters
     */
    public EnglishName(String surname, String personalName) {
        String normalizedSurname = Objects.toString(surname, "");
        String normalizedPersonalName = Objects.toString(personalName, "");
        EnglishNameUtil.validate(normalizedSurname, normalizedPersonalName);
        this.surname = normalizedSurname;
        this.personalName = normalizedPersonalName;
    }

    /**
     * Formats the name as {@code "surname, personal name"}, omitting empty portions.
     *
     * @return the complete English name
     */
    public String getFullName() {
        if (surname.isEmpty()) {
            return personalName;
        }
        if (personalName.isEmpty()) {
            return surname;
        }
        return String.format(FORMAT, surname, personalName);
    }

    /**
     * Returns the English surname.
     *
     * @return the surname, possibly empty
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Returns the English personal name.
     *
     * @return the personal name, possibly empty
     */
    public String getPersonalName() {
        return personalName;
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
        if (!(object instanceof EnglishName)) {
            return false;
        }
        EnglishName other = (EnglishName) object;
        return surname.equals(other.surname) && personalName.equals(other.personalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(surname, personalName);
    }
}
