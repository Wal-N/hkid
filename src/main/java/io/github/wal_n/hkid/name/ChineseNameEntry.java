package io.github.wal_n.hkid.name;

import io.github.wal_n.hkid.card.Sex;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * One seed row used for random Chinese name generation.
 */
public final class ChineseNameEntry {
    private final String commercialCode;
    private final String character;
    private final String romanisation;
    private final boolean commonSurname;
    private final Set<Sex> supportedSexes;
    private final int weight;

    ChineseNameEntry(String commercialCode,
                     String character,
                     String romanisation,
                     boolean commonSurname,
                     Set<Sex> supportedSexes,
                     int weight) {
        if (!ChineseNameUtil.isValidCommercialCode(commercialCode)) {
            throw new IllegalArgumentException("Commercial code must be four digits");
        }
        if (character == null || ChineseNameUtil.lengthOf(character) != 1 || !ChineseNameUtil.isChinese(character)) {
            throw new IllegalArgumentException("Name seed character must be one Chinese character");
        }
        if (romanisation == null || !romanisation.trim().matches("[A-Za-z]+")) {
            throw new IllegalArgumentException("Romanisation must contain letters only");
        }
        if (supportedSexes == null || supportedSexes.isEmpty()) {
            throw new IllegalArgumentException("Supported sexes cannot be empty");
        }
        if (supportedSexes.contains(null)) {
            throw new IllegalArgumentException("Supported sexes cannot contain null");
        }
        if (commonSurname && !supportedSexes.containsAll(EnumSet.allOf(Sex.class))) {
            throw new IllegalArgumentException("Common surnames must support every sex");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }

        this.commercialCode = commercialCode;
        this.character = character;
        this.romanisation = normaliseRomanisation(romanisation);
        this.commonSurname = commonSurname;
        this.supportedSexes = Collections.unmodifiableSet(EnumSet.copyOf(supportedSexes));
        this.weight = weight;
    }

    /**
     * Returns the four-digit Chinese commercial code for this character.
     *
     * @return the commercial code
     */
    public String getCommercialCode() {
        return commercialCode;
    }

    /**
     * Returns the Chinese character represented by this entry.
     *
     * @return one Chinese character
     */
    public String getCharacter() {
        return character;
    }

    /**
     * Returns the title-cased romanisation used when generating English names.
     *
     * @return the character's romanisation
     */
    public String getRomanisation() {
        return romanisation;
    }

    /**
     * Returns whether this entry may be selected as a generated surname.
     *
     * @return {@code true} for a common-surname entry
     */
    public boolean isCommonSurname() {
        return commonSurname;
    }

    /**
     * Returns the card sex values for which this entry may be selected.
     * Unisex entries and surnames support both values.
     *
     * @return an unmodifiable, non-empty set of supported sex values
     */
    public Set<Sex> getSupportedSexes() {
        return supportedSexes;
    }

    /**
     * Tests whether this entry may be selected for the requested sex.
     *
     * @param requestedSex requested generated-name sex
     * @return {@code true} when this entry supports the requested sex
     * @throws IllegalArgumentException if {@code requestedSex} is null
     */
    public boolean isCompatibleWith(Sex requestedSex) {
        if (requestedSex == null) {
            throw new IllegalArgumentException("Requested sex cannot be null");
        }
        return supportedSexes.contains(requestedSex);
    }

    /**
     * Returns the relative selection weight used by the name generator.
     *
     * @return a positive selection weight
     */
    public int getWeight() {
        return weight;
    }

    private static String normaliseRomanisation(String romanisation) {
        String value = romanisation.trim().toLowerCase();
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
