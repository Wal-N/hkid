package hkid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Chinese name area of an HKID card.
 */
public class ChiName {
    /**
     * HKID cards reserve up to six printed Chinese characters for the Chinese name.
     */
    public static final int MAX_LENGTH = 6;

    private String surname = "";
    private String personalName = "";
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
        return surname + personalName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        String value = Objects.toString(surname, "");
        ChiNameUtil.validateChinesePart(value, "Surname");
        ChiNameUtil.validateTotalLength(value, this.personalName);
        ChiNameUtil.validateCommercialCodeCount(value, this.personalName, commercialCodes);
        this.surname = value;
    }

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        String value = Objects.toString(personalName, "");
        ChiNameUtil.validateChinesePart(value, "Personal name");
        ChiNameUtil.validateTotalLength(this.surname, value);
        ChiNameUtil.validateCommercialCodeCount(this.surname, value, commercialCodes);
        this.personalName = value;
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
        return getFullName();
    }
}
