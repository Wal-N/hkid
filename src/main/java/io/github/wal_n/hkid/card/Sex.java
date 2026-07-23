package io.github.wal_n.hkid.card;

import java.util.Locale;

/**
 * Sex marker printed on an HKID card.
 */
public enum Sex {
    MALE("男", "M"),
    FEMALE("女", "F");

    private final String chiMarker;
    private final String engMarker;

    Sex(String chiMarker, String engMarker) {
        this.chiMarker = chiMarker;
        this.engMarker = engMarker;
    }

    public String getChiMarker() {
        return chiMarker;
    }

    public String getEngMarker() {
        return engMarker;
    }

    /**
     * Returns the value as printed on the smart HKID card, for example "男 M".
     */
    public String getPrintedValue() {
        return chiMarker + " " + engMarker;
    }

    /**
     * Parses the HKID card marker, accepting either upper or lower case text.
     */
    public static Sex fromEngMarker(String engMarker) {
        if (engMarker == null) {
            throw new IllegalArgumentException("English sex marker cannot be null");
        }

        String normalizedMarker = engMarker.trim().toUpperCase(Locale.ROOT);
        for (Sex sex : values()) {
            if (sex.engMarker.equals(normalizedMarker)) {
                return sex;
            }
        }
        throw new IllegalArgumentException("English sex marker must be M or F");
    }

    /**
     * Keeps human-readable output aligned with the value printed on the card.
     */
    @Override
    public String toString() {
        return getPrintedValue();
    }
}
