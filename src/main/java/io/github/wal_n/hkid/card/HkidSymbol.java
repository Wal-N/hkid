package io.github.wal_n.hkid.card;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.wal_n.hkid.internal.ResourceJson;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A symbol printed on the front of a current Hong Kong smart identity card.
 */
public enum HkidSymbol {
    /** Three-asterisk Re-entry Permit eligibility symbol for holders aged 18 or older. */
    ADULT_RE_ENTRY_PERMIT,
    /** One-asterisk Re-entry Permit eligibility symbol for holders aged 11 to 17. */
    MINOR_RE_ENTRY_PERMIT,

    /** Residential-status symbol indicating HKSAR right of abode. */
    RIGHT_OF_ABODE,
    /** Residential-status symbol indicating a stay restriction at registration. */
    STAY_LIMITED,
    /** Residential-status symbol indicating HKSAR right to land. */
    RIGHT_TO_LAND,
    /** Residential-status symbol indicating no stay restriction at registration. */
    STAY_NOT_LIMITED,

    /** Reported-place-of-birth symbol for Hong Kong. */
    BORN_IN_HONG_KONG,
    /** Reported-place-of-birth symbol for the Chinese Mainland. */
    BORN_IN_MAINLAND,
    /** Reported-place-of-birth symbol for Macao. */
    BORN_IN_MACAO,
    /** Reported-place-of-birth symbol for elsewhere. */
    BORN_ELSEWHERE,

    /** Other-information symbol indicating updated birth details. */
    BIRTH_DETAILS_CHANGED,
    /** Other-information symbol indicating an updated name. */
    NAME_CHANGED;

    private static final String METADATA_RESOURCE =
            "io/github/wal_n/hkid/data/hkid-symbols.json";
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    /**
     * Returns the code printed on the card for this symbol.
     *
     * @return the symbol code
     */
    public String getCode() {
        return metadata().code;
    }

    /**
     * Returns the category that constrains this symbol.
     *
     * @return the symbol category
     */
    public HkidSymbolCategory getCategory() {
        return metadata().category;
    }

    /**
     * Returns the bundled English description of this symbol.
     *
     * @return the English description
     */
    public String getDescription() {
        return metadata().description;
    }

    /**
     * Returns the bundled Traditional Chinese description of this symbol.
     *
     * @return the Traditional Chinese description
     */
    public String getTraditionalChineseDescription() {
        return metadata().traditionalChineseDescription;
    }

    /**
     * Parses one current smart-HKID symbol code, ignoring surrounding whitespace
     * and letter case.
     *
     * @param code printed symbol code to parse
     * @return the matching symbol
     * @throws IllegalArgumentException if {@code code} is null or unsupported
     */
    public static HkidSymbol fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("HKID symbol code cannot be null");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        for (HkidSymbol symbol : values()) {
            if (symbol.getCode().equals(normalizedCode)) {
                return symbol;
            }
        }
        throw new IllegalArgumentException("Unsupported current smart HKID symbol: " + code);
    }

    @Override
    public String toString() {
        return getCode();
    }

    private SymbolMetadata metadata() {
        return MetadataHolder.BY_SYMBOL.get(this);
    }

    private static Map<HkidSymbol, SymbolMetadata> loadMetadata() {
        Map<HkidSymbol, SymbolMetadata> metadataBySymbol = new EnumMap<>(HkidSymbol.class);
        Set<String> codes = new HashSet<>();
        JsonObject root = ResourceJson.readObject(METADATA_RESOURCE);
        String rootContext = "root of " + METADATA_RESOURCE;
        ResourceJson.requireExactKeys(root, rootContext, "schemaVersion", "symbols");
        int schemaVersion = ResourceJson.requireInteger(root, "schemaVersion", rootContext);
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "Unsupported schemaVersion " + schemaVersion + " in " + METADATA_RESOURCE);
        }

        JsonArray entries = ResourceJson.requireArray(root, "symbols", rootContext);
        for (int i = 0; i < entries.size(); i++) {
            JsonElement element = entries.get(i);
            String context = "symbols[" + i + "] in " + METADATA_RESOURCE;
            if (!element.isJsonObject()) {
                throw new IllegalStateException(context + " must be an object");
            }
            JsonObject entry = element.getAsJsonObject();
            ResourceJson.requireExactKeys(
                    entry, context, "id", "code", "category", "descriptions");

            String id = ResourceJson.requireString(entry, "id", context);
            HkidSymbol symbol;
            try {
                symbol = HkidSymbol.valueOf(id);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Unknown HKID symbol key in " + METADATA_RESOURCE + ": " + id, e);
            }

            SymbolMetadata metadata = SymbolMetadata.from(entry, context);
            if (metadataBySymbol.put(symbol, metadata) != null) {
                throw new IllegalStateException(
                        "Duplicate HKID symbol key in " + METADATA_RESOURCE + ": " + symbol.name());
            }
            if (!codes.add(metadata.code)) {
                throw new IllegalStateException(
                        "Duplicate HKID symbol code in " + METADATA_RESOURCE + ": " + metadata.code);
            }
        }

        for (HkidSymbol symbol : values()) {
            if (!metadataBySymbol.containsKey(symbol)) {
                throw new IllegalStateException(
                        "Missing metadata for HKID symbol " + symbol.name() + " in " + METADATA_RESOURCE);
            }
        }
        return Collections.unmodifiableMap(metadataBySymbol);
    }

    private static final class MetadataHolder {
        private static final Map<HkidSymbol, SymbolMetadata> BY_SYMBOL = loadMetadata();
    }

    private static final class SymbolMetadata {
        private final String code;
        private final HkidSymbolCategory category;
        private final String description;
        private final String traditionalChineseDescription;

        private SymbolMetadata(String code,
                               HkidSymbolCategory category,
                               String description,
                               String traditionalChineseDescription) {
            if (code.isEmpty() || description.isEmpty() || traditionalChineseDescription.isEmpty()) {
                throw new IllegalStateException("HKID symbol metadata fields cannot be empty");
            }
            this.code = code;
            this.category = category;
            this.description = description;
            this.traditionalChineseDescription = traditionalChineseDescription;
        }

        private static SymbolMetadata from(JsonObject entry, String context) {
            JsonObject descriptions =
                    ResourceJson.requireObject(entry, "descriptions", context);
            String descriptionsContext = "descriptions in " + context;
            ResourceJson.requireExactKeys(
                    descriptions, descriptionsContext, "en", "zh-Hant");

            String categoryValue =
                    ResourceJson.requireString(entry, "category", context);
            HkidSymbolCategory category;
            try {
                category = HkidSymbolCategory.valueOf(categoryValue);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Unknown HKID symbol category in " + context + ": " + categoryValue, e);
            }
            return new SymbolMetadata(
                    ResourceJson.requireString(entry, "code", context),
                    category,
                    ResourceJson.requireString(descriptions, "en", descriptionsContext),
                    ResourceJson.requireString(
                            descriptions, "zh-Hant", descriptionsContext));
        }
    }
}
