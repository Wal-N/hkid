package hkid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the Chinese name area of an HKID card.
 */
public class ChiName {
    private String surname;
    private String personalName;
    private List<String> commercialCodes = new ArrayList<>();

    public ChiName() {
    }

    public ChiName(String surname, String personalName) {
        this(surname, personalName, null);
    }

    public ChiName(String surname, String personalName, List<String> commercialCodes) {
        setSurname(surname);
        setPersonalName(personalName);
        setCommercialCodes(commercialCodes);
    }

    public String getFullName() {
        if (surname == null && personalName == null) {
            return null;
        }
        return nullToEmpty(surname) + nullToEmpty(personalName);
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        ChiNameUtil.validateChinesePart(surname, "Surname");
        ChiNameUtil.validateTotalLength(surname, this.personalName);
        ChiNameUtil.validateCommercialCodeCount(surname, this.personalName, commercialCodes);
        this.surname = surname;
    }

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        ChiNameUtil.validateChinesePart(personalName, "Personal name");
        ChiNameUtil.validateTotalLength(this.surname, personalName);
        ChiNameUtil.validateCommercialCodeCount(this.surname, personalName, commercialCodes);
        this.personalName = personalName;
    }

    public List<String> getCommercialCodes() {
        return Collections.unmodifiableList(commercialCodes);
    }

    /**
     * Each Chinese character on the card has a four-digit commercial code.
     */
    public void setCommercialCodes(List<String> commercialCodes) {
        ChiNameUtil.validateCommercialCodes(commercialCodes);
        ChiNameUtil.validateCommercialCodeCount(surname, personalName, commercialCodes);
        this.commercialCodes = commercialCodes == null
                ? new ArrayList<>()
                : new ArrayList<>(commercialCodes);
    }

    @Override
    public String toString() {
        String fullName = getFullName();
        return fullName != null ? fullName : "";
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
