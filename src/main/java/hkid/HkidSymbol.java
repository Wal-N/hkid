package hkid;

import java.util.Locale;

/**
 * A symbol printed on the front of a current Hong Kong smart identity card.
 *
 * @see <a href="https://www.immd.gov.hk/pdforms/rop133.pdf">Immigration Department ROP133</a>
 */
public enum HkidSymbol {
    ADULT_RE_ENTRY_PERMIT("***", HkidSymbolCategory.RE_ENTRY_PERMIT_ELIGIBILITY,
            "The holder is aged 18 or over and is eligible for a HKSAR Re-entry Permit.",
            "持證人年齡為18歲或以上及有資格申領香港特別行政區回港證"),
    MINOR_RE_ENTRY_PERMIT("*", HkidSymbolCategory.RE_ENTRY_PERMIT_ELIGIBILITY,
            "The holder is aged between 11 and 17 and is eligible for a HKSAR Re-entry Permit.",
            "持證人年齡為11歲至17歲及有資格申領香港特別行政區回港證"),

    RIGHT_OF_ABODE("A", HkidSymbolCategory.RESIDENTIAL_STATUS,
            "The holder has the right of abode in the HKSAR.",
            "持證人擁有香港居留權"),
    STAY_LIMITED("C", HkidSymbolCategory.RESIDENTIAL_STATUS,
            "The holder's stay in the HKSAR was limited by the Director of Immigration at the time of registration of the card.",
            "持證人登記領證時在香港的居留受到入境事務處處長的限制"),
    RIGHT_TO_LAND("R", HkidSymbolCategory.RESIDENTIAL_STATUS,
            "The holder has the right to land in the HKSAR.",
            "持證人擁有香港入境權"),
    STAY_NOT_LIMITED("U", HkidSymbolCategory.RESIDENTIAL_STATUS,
            "The holder's stay in the HKSAR was not limited by the Director of Immigration at the time of registration of the card.",
            "持證人登記領證時在香港的居留不受入境事務處處長的限制"),

    BORN_IN_HONG_KONG("Z", HkidSymbolCategory.REPORTED_PLACE_OF_BIRTH,
            "Hong Kong", "香港"),
    BORN_IN_MAINLAND("X", HkidSymbolCategory.REPORTED_PLACE_OF_BIRTH,
            "Mainland", "內地"),
    BORN_IN_MACAO("W", HkidSymbolCategory.REPORTED_PLACE_OF_BIRTH,
            "Macao", "澳門"),
    BORN_ELSEWHERE("O", HkidSymbolCategory.REPORTED_PLACE_OF_BIRTH,
            "Elsewhere", "其他地區"),

    BIRTH_DETAILS_CHANGED("B", HkidSymbolCategory.OTHER_INFORMATION,
            "The holder's reported date of birth or place of birth has been changed since first registration.",
            "持證人所報稱的出生日期或地點自首次登記以後，曾作出更改"),
    NAME_CHANGED("N", HkidSymbolCategory.OTHER_INFORMATION,
            "The holder's reported name has been changed since first registration.",
            "持證人所報稱的姓名自首次登記以後，曾作出更改");

    private final String code;
    private final HkidSymbolCategory category;
    private final String description;
    private final String traditionalChineseDescription;

    HkidSymbol(String code,
               HkidSymbolCategory category,
               String description,
               String traditionalChineseDescription) {
        this.code = code;
        this.category = category;
        this.description = description;
        this.traditionalChineseDescription = traditionalChineseDescription;
    }

    public String getCode() {
        return code;
    }

    public HkidSymbolCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getTraditionalChineseDescription() {
        return traditionalChineseDescription;
    }

    public static HkidSymbol fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("HKID symbol code cannot be null");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        for (HkidSymbol symbol : values()) {
            if (symbol.code.equals(normalizedCode)) {
                return symbol;
            }
        }
        throw new IllegalArgumentException("Unsupported current smart HKID symbol: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}
