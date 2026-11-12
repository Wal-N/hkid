package io.github.wal_n.hkid.name;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable value object representing one four-digit Chinese commercial code
 * with an optional one-digit extension.
 *
 * <p>Validation checks the format only; it does not verify that the code and
 * extension are assigned to a particular Chinese character or glyph.</p>
 *
 * <p>The fields follow the Hong Kong Government's
 * <a href="https://www.digitalpolicy.gov.hk/en/our_work/data_governance/policies_standards/interoperability_framework/common_schemas/person_chinese_name/index.html">Person Chinese Name common schema</a>.
 * Equality includes the extension, but the text representation contains only
 * the four-digit code.</p>
 */
public final class ChineseCommercialCode {
    private final String code;
    private final String extensionNumber;

    /**
     * Creates a Chinese commercial code without an extension, preserving any
     * leading zeros.
     *
     * @param code exactly four ASCII digits
     * @throws IllegalArgumentException if the code is null or is not four ASCII digits
     */
    public ChineseCommercialCode(String code) {
        this(code, null);
    }

    /**
     * Creates a Chinese commercial code with an optional extension, preserving
     * any leading zeros in the four-digit code.
     *
     * @param code exactly four ASCII digits
     * @param extensionNumber one ASCII digit, or {@code null} for no extension;
     *         {@code "0"} is distinct from no extension
     * @throws IllegalArgumentException if the code is null or is not four ASCII
     *         digits, or a non-null extension is not one ASCII digit
     */
    public ChineseCommercialCode(String code, String extensionNumber) {
        if (!ChineseNameUtil.isValidCommercialCode(code)) {
            throw new IllegalArgumentException("Commercial code must be four ASCII digits");
        }
        if (extensionNumber != null && (extensionNumber.length() != 1
                || extensionNumber.charAt(0) < '0' || extensionNumber.charAt(0) > '9')) {
            throw new IllegalArgumentException("Commercial code extension must be one ASCII digit");
        }
        this.code = code;
        this.extensionNumber = extensionNumber;
    }

    /**
     * Returns the four-digit code, including any leading zeros.
     *
     * @return the commercial code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the optional one-digit extension.
     *
     * @return the extension, or an empty optional when none was supplied
     */
    public Optional<String> getExtensionNumber() {
        return Optional.ofNullable(extensionNumber);
    }

    /**
     * Returns only the four-digit code for display.
     *
     * <p>This representation omits any extension and cannot reconstruct the
     * complete value. Use {@link #getCode()} and {@link #getExtensionNumber()}
     * when storing or exchanging the complete code. No physical-card extension
     * printing format is defined here.</p>
     *
     * @return the four-digit code
     */
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
        return code.equals(other.code)
                && Objects.equals(extensionNumber, other.extensionNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, extensionNumber);
    }
}
