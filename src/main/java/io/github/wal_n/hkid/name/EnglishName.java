package io.github.wal_n.hkid.name;

import java.util.Objects;

/**
 * Immutable value object representing the English name area of an HKID card.
 */
public final class EnglishName {
    private static final String FORMAT = "%s, %s";

    private final String surname;
    private final String personalName;

    public EnglishName() {
        this("", "");
    }

    public EnglishName(String surname, String personalName) {
        String normalizedSurname = Objects.toString(surname, "");
        String normalizedPersonalName = Objects.toString(personalName, "");
        EnglishNameUtil.validateNamePart(normalizedSurname, "Surname");
        EnglishNameUtil.validateNamePart(normalizedPersonalName, "Personal name");
        this.surname = normalizedSurname;
        this.personalName = normalizedPersonalName;
    }

    public String getFullName() {
        if (surname.isEmpty()) {
            return personalName;
        }
        if (personalName.isEmpty()) {
            return surname;
        }
        return String.format(FORMAT, surname, personalName);
    }

    public String getSurname() {
        return surname;
    }

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
