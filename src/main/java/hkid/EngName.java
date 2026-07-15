package hkid;

/**
 * Represents the English name area of an HKID card.
 */
public class EngName {
    private String surname;
    private String personalName;

    public EngName() {
    }

    public EngName(String surname, String personalName) {
        setSurname(surname);
        setPersonalName(personalName);
    }

    public String getFullName() {
        if (surname == null && personalName == null) {
            return null;
        }
        if (surname == null) {
            return personalName;
        }
        if (personalName == null) {
            return surname;
        }
        return surname + ", " + personalName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        EngNameUtil.validateNamePart(surname, "Surname");
        this.surname = surname;
    }

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        EngNameUtil.validateNamePart(personalName, "Personal name");
        this.personalName = personalName;
    }

    @Override
    public String toString() {
        String fullName = getFullName();
        return fullName != null ? fullName : "";
    }
}
