package hkid;

import java.util.Objects;

/**
 * Represents the English name area of an HKID card.
 */
public class EngName {
    private static final String FORMAT = "%s, %s";

    private String surname = "";
    private String personalName = "";

    public EngName() {
    }

    public EngName(String surname, String personalName) {
        setSurname(surname);
        setPersonalName(personalName);
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

    public void setSurname(String surname) {
        String value = Objects.toString(surname, "");
        EngNameUtil.validateNamePart(value, "Surname");
        this.surname = value;
    }

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        String value = Objects.toString(personalName, "");
        EngNameUtil.validateNamePart(value, "Personal name");
        this.personalName = value;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
