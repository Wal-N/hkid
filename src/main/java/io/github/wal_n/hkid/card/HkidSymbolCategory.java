package io.github.wal_n.hkid.card;

/**
 * Official categories used to group symbols printed on a current smart HKID card.
 */
public enum HkidSymbolCategory {
    /** Re-entry Permit eligibility; exactly one age-specific symbol is permitted. */
    RE_ENTRY_PERMIT_ELIGIBILITY(false),
    /** Residential status; only one status symbol is permitted. */
    RESIDENTIAL_STATUS(false),
    /** Reported place of birth; only one place symbol is permitted. */
    REPORTED_PLACE_OF_BIRTH(false),
    /** Other card information; multiple distinct symbols are permitted. */
    OTHER_INFORMATION(true);

    private final boolean allowsMultiple;

    HkidSymbolCategory(boolean allowsMultiple) {
        this.allowsMultiple = allowsMultiple;
    }

    /**
     * Returns whether a card may contain multiple distinct symbols in this category.
     *
     * @return {@code true} when multiple symbols are permitted
     */
    public boolean allowsMultiple() {
        return allowsMultiple;
    }
}
