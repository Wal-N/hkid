package io.github.wal_n.hkid.name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the Chinese name area of an HKID card.
 */
public class ChineseName {
    /**
     * HKID cards reserve up to six printed Chinese characters for the Chinese name.
     */
    public static final int MAX_LENGTH = 6;

    private String surname = "";
    private String personalName = "";
    private List<String> commercialCodes = new ArrayList<>();

    public ChineseName() {
    }

    public ChineseName(String surname, String personalName) {
        this(surname, personalName, null);
    }

    public ChineseName(String surname, String personalName, List<String> commercialCodes) {
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
        ChineseNameUtil.validateChinesePart(value, "Surname");
        ChineseNameUtil.validateTotalLength(value, this.personalName);
        ChineseNameUtil.validateCommercialCodeCount(value, this.personalName, commercialCodes);
        this.surname = value;
    }

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        String value = Objects.toString(personalName, "");
        ChineseNameUtil.validateChinesePart(value, "Personal name");
        ChineseNameUtil.validateTotalLength(this.surname, value);
        ChineseNameUtil.validateCommercialCodeCount(this.surname, value, commercialCodes);
        this.personalName = value;
    }

    public List<String> getCommercialCodes() {
        return Collections.unmodifiableList(commercialCodes);
    }

    /**
     * Each Chinese character on the card has a four-digit commercial code.
     */
    public void setCommercialCodes(List<String> commercialCodes) {
        ChineseNameUtil.validateCommercialCodes(commercialCodes);
        ChineseNameUtil.validateCommercialCodeCount(surname, personalName, commercialCodes);
        this.commercialCodes = commercialCodes == null
                ? new ArrayList<>()
                : new ArrayList<>(commercialCodes);
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
