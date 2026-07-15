package hkid;

/**
 * Reference from https://www.immd.gov.hk/eng/services/hkid/smartid.html
 */
public enum DefinedSymbol {
    // Holder's eligibility for a HKSAR Re-entry Permit
    ThreeStars("***",
            "The holder is aged 18 or over and is eligible for a Hong Kong Re-entry Permit.",
            "持證人年齡為18歲或以上及有資格申領香港特別行政區回港證"),
    OneStar("*",
            "The holder is aged between 11 and 17 and is eligible for a Hong Kong Re-entry Permit.",
            "持證人年齡為11歲至17歲及有資格申領香港特別行政區回港證"),

    // Holder’s residential status in the HKSAR
    A("A",
            "The holder has the right of abode in the HKSAR.",
            "持證人擁有香港居留權"),
    C("C",
            "The holder’s stay in the HKSAR is limited by the Director of Immigration at the time of registration of the card.",
            "持證人登記領證時在香港的居留受到入境事務處處長的限制"),
    R("R",
            "The holder has the right to land in the HKSAR.",
            "持證人擁有香港入境權"),
    U("U",
            "The holder’s stay in the HKSAR is not limited by the Director of Immigration at the time of registration of the card.",
            "持證人登記領證時在香港的居留不受入境事務處處長的限制"),

    // Holder’s reported place of birth
    Z("Z", "Hong Kong", "香港"),
    X("X", "Mainland", "內地"),
    W("W", "Macao", "澳門"),
    O("O", "Elsewhere", "其他地區"),

    // Other information
    B("B",
            "The holder’s reported date of birth or place of birth has been changed since first registration.",
            "持證人所報稱的出生日期或地點自首次登記以後，曾作出更改"),
    N("N",
            "The holder’s reported name has been changed since first registration.",
            "持證人所報稱的姓名自首次登記以後，曾作出更改");

    private final String str;
    private final String description;
    private final String tcDescription;
    DefinedSymbol(String str, String description, String tcDescription) {
        this.str = str;
        this.description = description;
        this.tcDescription = tcDescription;
    }

    public String getStr() {
        return str;
    }

    public String getDescription() {
        return description;
    }

    public String getTcDescription() {
        return tcDescription;
    }

    @Override
    public String toString() {
        return str;
    }
}
