package io.github.wal_n.hkid.name;

/**
 * Immutable value object representing one four-digit Chinese commercial code.
 *
 * <p>Validation checks the format only; it does not verify that the code is
 * assigned to a particular Chinese character.</p>
 */
public final class ChineseCommercialCode {
    private final String code;

    /**
     * Creates a Chinese commercial code, preserving any leading zeros.
     *
     * @param code exactly four ASCII digits
     * @throws IllegalArgumentException if the code is null or is not four ASCII digits
     */
    public ChineseCommercialCode(String code) {
        if (!ChineseNameUtil.isValidCommercialCode(code)) {
            throw new IllegalArgumentException("Commercial code must be four ASCII digits");
        }
        this.code = code;
    }

    /**
     * Returns the four-digit code, including any leading zeros.
     *
     * @return the commercial code
     */
    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ChineseCommercialCode)) {
            return false;
        }
        ChineseCommercialCode other = (ChineseCommercialCode) object;
        return code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
