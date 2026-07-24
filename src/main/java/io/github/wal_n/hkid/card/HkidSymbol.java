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
 *
 * @see <a href="https://www.immd.gov.hk/pdforms/rop133.pdf">Immigration Department ROP133</a>
 */
public enum HkidSymbol {
    ADULT_RE_ENTRY_PERMIT,
    MINOR_RE_ENTRY_PERMIT,

    RIGHT_OF_ABODE,
    STAY_LIMITED,
    RIGHT_TO_LAND,
    STAY_NOT_LIMITED,

    BORN_IN_HONG_KONG,
    BORN_IN_MAINLAND,
    BORN_IN_MACAO,
    BORN_ELSEWHERE,

    BIRTH_DETAILS_CHANGED,
    NAME_CHANGED;

    private static final String METADATA_RESOURCE =
            "io/github/wal_n/hkid/data/hkid-symbols.json";
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    public String getCode() {
        return metadata().code;
    }

    public HkidSymbolCategory getCategory() {
        return metadata().category;
    }

    public String getDescription() {
        return metadata().description;
    }

    public String getTraditionalChineseDescription() {
        return metadata().traditionalChineseDescription;
    }

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
