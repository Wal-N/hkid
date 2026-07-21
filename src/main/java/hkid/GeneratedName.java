package hkid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generated Chinese name, matching commercial codes, romanisation, and English form.
 */
public final class GeneratedName {
    private final String chiSurname;
    private final String chiPersonalName;
    private final String chiFullName;
    private final List<String> commercialCodes;
    private final String engSurname;
    private final String engPersonalName;
    private final String engFullName;
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

        this.chiSurname = chiName.getSurname();
        this.chiPersonalName = chiName.getPersonalName();
        this.chiFullName = chiName.getFullName();
        this.commercialCodes = immutableCopy(chiName.getCommercialCodes());
        this.engSurname = engName.getSurname();
        this.engPersonalName = engName.getPersonalName();
        this.engFullName = engName.getFullName();
        this.romanisationSyllables = immutableCopy(romanisationSyllables);
    }

    /**
     * Returns a mutable copy of the Chinese name in this snapshot.
     * Changes to the returned object do not affect this generated name.
     */
    public ChiName getChiName() {
        return new ChiName(chiSurname, chiPersonalName, commercialCodes);
    }

    /**
     * Returns a mutable copy of the English name in this snapshot.
     * Changes to the returned object do not affect this generated name.
     */
    public EngName getEngName() {
        return new EngName(engSurname, engPersonalName);
    }

    public String getChiFullName() {
        return chiFullName;
    }

    public List<String> getCommercialCodes() {
        return commercialCodes;
    }

    public String getEngFullName() {
        return engFullName;
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
