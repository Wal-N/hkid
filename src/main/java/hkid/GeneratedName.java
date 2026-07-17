package hkid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generated Chinese name, matching commercial codes, romanisation, and English form.
 */
public final class GeneratedName {
    private final ChiName chiName;
    private final EngName engName;
    private final List<String> romanisationSyllables;

    GeneratedName(ChiName chiName, EngName engName, List<String> romanisationSyllables) {
        if (chiName == null) {
            throw new IllegalArgumentException("Chinese name cannot be null");
        }
        if (engName == null) {
            throw new IllegalArgumentException("English name cannot be null");
        }
        if (romanisationSyllables == null || romanisationSyllables.isEmpty()) {
            throw new IllegalArgumentException("Romanisation syllables cannot be empty");
        }

        this.chiName = chiName;
        this.engName = engName;
        this.romanisationSyllables = Collections.unmodifiableList(new ArrayList<>(romanisationSyllables));
    }

    public ChiName getChiName() {
        return chiName;
    }

    public EngName getEngName() {
        return engName;
    }

    public String getChiFullName() {
        return chiName.getFullName();
    }

    public List<String> getCommercialCodes() {
        return chiName.getCommercialCodes();
    }

    public String getEngFullName() {
        return engName.getFullName();
    }

    public List<String> getRomanisationSyllables() {
        return romanisationSyllables;
    }

    public String getRomanisation() {
        return String.join(" ", romanisationSyllables);
    }
}
