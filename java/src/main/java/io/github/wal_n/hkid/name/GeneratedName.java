package io.github.wal_n.hkid.name;

import java.util.List;

/**
 * Generated Chinese name with matching commercial codes and English form.
 *
 * <p>Name getters return the same domain types as those on a card. Methods
 * named {@code getXString} return their text representation.</p>
 */
public final class GeneratedName {
    private final ChineseName chineseName;
    private final EnglishName englishName;

    GeneratedName(ChineseName chineseName, EnglishName englishName) {
        if (chineseName == null) {
            throw new IllegalArgumentException("Chinese name cannot be null");
        }
        if (englishName == null) {
            throw new IllegalArgumentException("English name cannot be null");
        }

        this.chineseName = chineseName;
        this.englishName = englishName;
    }

    /**
     * Returns the immutable Chinese name in this snapshot.
     *
     * @return the generated Chinese name
     */
    public ChineseName getChineseName() {
        return chineseName;
    }

    /**
     * Returns the immutable English name in this snapshot.
     *
     * @return the generated English name
     */
    public EnglishName getEnglishName() {
        return englishName;
    }

    /**
     * Returns the complete generated Chinese name.
     *
     * @return the surname and personal name with no separator
     */
    public String getChineseNameString() {
        return chineseName.getFullNameString();
    }

    /**
     * Returns the commercial codes matching the generated Chinese characters.
     *
     * @return an unmodifiable list of commercial codes in name order
     */
    public List<ChineseCommercialCode> getChineseCommercialCodes() {
        return chineseName.getCommercialCodes();
    }

    /**
     * Returns the complete generated English name.
     *
     * @return the formatted English name
     */
    public String getEnglishNameString() {
        return englishName.getFullNameString();
    }
}
