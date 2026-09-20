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
    private final String chineseMarker;
    private final String englishMarker;

    Sex(String chineseMarker, String englishMarker) {
        this.chineseMarker = chineseMarker;
        this.englishMarker = englishMarker;
    }

    /**
     * Returns the Traditional Chinese marker printed on the card.
     *
     * @return {@code 男} or {@code 女}
     */
    public String getChineseMarker() {
        return chineseMarker;
    }

    /**
     * Returns the English marker printed on the card.
     *
     * @return {@code M} or {@code F}
     */
    public String getEnglishMarker() {
        return englishMarker;
    }

    /**
     * Parses the HKID card marker, accepting either upper or lower case text.
     *
     * @param englishMarker English marker to parse
     * @return the matching sex value
     * @throws IllegalArgumentException if the marker is null or is not {@code M} or {@code F}
     */
    public static Sex fromEnglishMarker(String englishMarker) {
        if (englishMarker == null) {
            throw new IllegalArgumentException("English sex marker cannot be null");
        }

        String normalizedMarker = englishMarker.trim().toUpperCase(Locale.ROOT);
        for (Sex sex : values()) {
            if (sex.englishMarker.equals(normalizedMarker)) {
                return sex;
            }
        }
        throw new IllegalArgumentException("English sex marker must be M or F");
    }

    /**
     * Returns the value as printed on the smart HKID card, for example {@code "男 M"}.
     *
     * @return the Chinese and English markers separated by a space
     */
    @Override
    public String toString() {
        return String.format(FORMAT, chineseMarker, englishMarker);
    }
}
