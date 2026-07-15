package hkid;

import java.util.Locale;

/**
 * Sex marker printed on an HKID card.
 */
public enum Sex {
    MALE("M", "Male"),
    FEMALE("F", "Female");

    private final String code;
    private final String description;

    Sex(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parses the HKID card marker, accepting either upper or lower case text.
     */
    public static Sex fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Sex code cannot be null");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        for (Sex sex : values()) {
            if (sex.code.equals(normalizedCode)) {
                return sex;
            }
        }
        throw new IllegalArgumentException("Sex code must be M or F");
    }

    /**
     * Keeps log and string output aligned with the printed card marker.
     */
    @Override
    public String toString() {
        return code;
    }
}
