package io.github.wal_n.hkid.name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generated Chinese name, matching commercial codes, romanisation, and English form.
 */
public final class GeneratedName {
    private final String chineseSurname;
    private final String chinesePersonalName;
    private final String chineseFullName;
    private final List<String> commercialCodes;
    private final String englishSurname;
    private final String englishPersonalName;
    private final String englishFullName;
    private final List<String> romanisationSyllables;

    GeneratedName(ChineseName chineseName, EnglishName englishName, List<String> romanisationSyllables) {
        if (chineseName == null) {
            throw new IllegalArgumentException("Chinese name cannot be null");
        }
        if (englishName == null) {
            throw new IllegalArgumentException("English name cannot be null");
        }
        if (romanisationSyllables == null || romanisationSyllables.isEmpty()) {
            throw new IllegalArgumentException("Romanisation syllables cannot be empty");
        }

        this.chineseSurname = chineseName.getSurname();
        this.chinesePersonalName = chineseName.getPersonalName();
        this.chineseFullName = chineseName.getFullName();
        this.commercialCodes = immutableCopy(chineseName.getCommercialCodes());
        this.englishSurname = englishName.getSurname();
        this.englishPersonalName = englishName.getPersonalName();
        this.englishFullName = englishName.getFullName();
        this.romanisationSyllables = immutableCopy(romanisationSyllables);
    }

    /**
     * Returns a mutable copy of the Chinese name in this snapshot.
     * Changes to the returned object do not affect this generated name.
     */
    public ChineseName getChineseName() {
        return new ChineseName(chineseSurname, chinesePersonalName, commercialCodes);
    }

    /**
     * Returns a mutable copy of the English name in this snapshot.
     * Changes to the returned object do not affect this generated name.
     */
    public EnglishName getEnglishName() {
        return new EnglishName(englishSurname, englishPersonalName);
    }

    public String getChineseFullName() {
        return chineseFullName;
    }

    public List<String> getCommercialCodes() {
        return commercialCodes;
    }

    public String getEnglishFullName() {
        return englishFullName;
    }

    public List<String> getRomanisationSyllables() {
        return romanisationSyllables;
    }

    public String getRomanisation() {
        return String.join(" ", romanisationSyllables);
    }

    private static List<String> immutableCopy(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
