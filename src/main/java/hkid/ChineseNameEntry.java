package hkid;

/**
 * One seed row used for random Chinese name generation.
 */
public final class ChineseNameEntry {
    private final String commercialCode;
    private final String character;
    private final String romanisation;
    private final boolean commonSurname;
    private final int weight;

    ChineseNameEntry(String commercialCode,
                     String character,
                     String romanisation,
                     boolean commonSurname,
                     int weight) {
        if (!ChiNameUtil.isValidCommercialCode(commercialCode)) {
            throw new IllegalArgumentException("Commercial code must be four digits");
        }
        if (character == null || ChiNameUtil.lengthOf(character) != 1 || !ChiNameUtil.isChinese(character)) {
            throw new IllegalArgumentException("Name seed character must be one Chinese character");
        }
        if (romanisation == null || !romanisation.trim().matches("[A-Za-z]+")) {
            throw new IllegalArgumentException("Romanisation must contain letters only");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }

        this.commercialCode = commercialCode;
        this.character = character;
        this.romanisation = normaliseRomanisation(romanisation);
        this.commonSurname = commonSurname;
        this.weight = weight;
    }

    public String getCommercialCode() {
        return commercialCode;
    }

    public String getCharacter() {
        return character;
    }

    public String getRomanisation() {
        return romanisation;
    }

    public boolean isCommonSurname() {
        return commonSurname;
    }

    public int getWeight() {
        return weight;
    }

    private static String normaliseRomanisation(String romanisation) {
        String value = romanisation.trim().toLowerCase();
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
