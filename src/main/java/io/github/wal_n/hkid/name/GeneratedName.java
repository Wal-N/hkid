package io.github.wal_n.hkid.name;

import java.util.List;

/**
 * Generated Chinese name with matching commercial codes and English form.
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
     */
    public ChineseName getChineseName() {
        return chineseName;
    }

    /**
     * Returns the immutable English name in this snapshot.
     */
    public EnglishName getEnglishName() {
        return englishName;
    }

    public String getChineseFullName() {
        return chineseName.getFullName();
    }

    public List<String> getCommercialCodes() {
        return chineseName.getCommercialCodes();
    }

    public String getEnglishFullName() {
        return englishName.getFullName();
    }
}
