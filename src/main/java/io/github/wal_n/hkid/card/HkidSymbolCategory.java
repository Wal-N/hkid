package io.github.wal_n.hkid.card;

/**
 * Official categories used to group symbols printed on a current smart HKID card.
 */
public enum HkidSymbolCategory {
    RE_ENTRY_PERMIT_ELIGIBILITY(false),
    RESIDENTIAL_STATUS(false),
    REPORTED_PLACE_OF_BIRTH(false),
    OTHER_INFORMATION(true);

    private final boolean allowsMultiple;

    HkidSymbolCategory(boolean allowsMultiple) {
        this.allowsMultiple = allowsMultiple;
    }

    public boolean allowsMultiple() {
        return allowsMultiple;
    }
}
