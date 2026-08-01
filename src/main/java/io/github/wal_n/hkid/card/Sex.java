package io.github.wal_n.hkid.card;

import java.util.Locale;

/**
 * Sex marker printed on an HKID card.
 */
public enum Sex {
    /** Male marker printed as Chinese {@code 男} and English {@code M}. */
    MALE("男", "M"),
    /** Female marker printed as Chinese {@code 女} and English {@code F}. */
    FEMALE("女", "F");

    private static final String FORMAT = "%s %s";
    private final String chiMarker;
    private final String engMarker;

    Sex(String chiMarker, String engMarker) {
        this.chiMarker = chiMarker;
        this.engMarker = engMarker;
    }

    /**
     * Returns the Traditional Chinese marker printed on the card.
     *
     * @return {@code 男} or {@code 女}
     */
    public String getChiMarker() {
        return chiMarker;
    }

    /**
     * Returns the English marker printed on the card.
     *
     * @return {@code M} or {@code F}
     */
    public String getEngMarker() {
        return engMarker;
    }

    /**
     * Returns the value as printed on the smart HKID card, for example {@code "男 M"}.
     *
     * @return the Chinese and English markers separated by a space
     */
    public String getPrintedValue() {
        return String.format(FORMAT, chiMarker, engMarker);
    }

    /**
     * Parses the HKID card marker, accepting either upper or lower case text.
     *
     * @param engMarker English marker to parse
     * @return the matching sex value
     * @throws IllegalArgumentException if the marker is null or is not {@code M} or {@code F}
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
