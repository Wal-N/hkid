package io.github.wal_n.hkid.number;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.wal_n.hkid.internal.ResourceJson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Predefined HKID number prefixes and their associated holder metadata.
 * Reference from L/M (82) in RP 32/230/R of Registration of Persons Offices and Wikipedia.
 */
public enum DefinedPrefix {
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
    J,
    K,
    L,
    M,
    N,
    P,
    R,
    S,
    T,
    V,
    W,
    Y,
    Z,
    WX,
    XA,
    XB,
    XC,
    XD,
    XE,
    XG,
    XH;
    // U: Pseudo ID for neonatal born in public hospitals

    private static final String METADATA_RESOURCE =
            "io/github/wal_n/hkid/data/defined-prefixes.json";
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private enum DateBasis {
        BIRTH_REGISTRATION,
        FIRST_ISSUE
    }

    public String getDescription() {
        return metadata().description;
    }

    public String getTraditionalChineseDescription() {
        return metadata().traditionalChineseDescription;
    }

    /**
     * Returns whether this prefix has an exact Hong Kong birth-registration period
     * containing the supplied date. Approximate historical birth ranges are not
     * treated as exact metadata by this method.
     */
    public boolean supportsHongKongBirthRegistrationDate(LocalDate birthRegistrationDate) {
        PrefixMetadata metadata = metadata();
        return birthRegistrationDate != null
                && metadata.hongKongBirthRegistrationStartDate != null
                && !birthRegistrationDate.isBefore(metadata.hongKongBirthRegistrationStartDate)
                && (metadata.hongKongBirthRegistrationEndDateExclusive == null
                || birthRegistrationDate.isBefore(metadata.hongKongBirthRegistrationEndDateExclusive));
    }

    /**
     * Finds the prefix whose exact Hong Kong birth-registration period contains
     * the supplied date.
     */
    public static Optional<DefinedPrefix> fromHongKongBirthRegistrationDate(
            LocalDate birthRegistrationDate) {
        for (DefinedPrefix definedPrefix : values()) {
            if (definedPrefix.supportsHongKongBirthRegistrationDate(birthRegistrationDate)) {
                return Optional.of(definedPrefix);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether at least one day in the supplied month falls within this
     * prefix's exact first-issue period.
     */
    public boolean supportsFirstIssueMonth(YearMonth firstIssueMonth) {
        PrefixMetadata metadata = metadata();
        return firstIssueMonth != null
                && metadata.firstIssueStartDate != null
                && !firstIssueMonth.atEndOfMonth().isBefore(metadata.firstIssueStartDate)
                && (metadata.firstIssueEndDateExclusive == null
                || firstIssueMonth.atDay(1).isBefore(metadata.firstIssueEndDateExclusive));
    }

    /**
     * Finds all standard prefixes whose exact first-issue period overlaps the
     * supplied month.
     */
    public static DefinedPrefix[] fromFirstIssueMonth(YearMonth firstIssueMonth) {
        return Arrays.stream(values())
                .filter(prefix -> prefix.supportsFirstIssueMonth(firstIssueMonth))
                .toArray(DefinedPrefix[]::new);
    }

    /**
     * Looks up predefined metadata for a prefix.
     *
     * @param prefix A one- or two-letter HKID prefix.
     * @return The matching predefined prefix, or an empty optional when no metadata is defined.
     */
    public static Optional<DefinedPrefix> fromPrefix(String prefix) {
        if (prefix == null) {
            return Optional.empty();
        }

        String normalizedPrefix = prefix.trim().toUpperCase(Locale.ROOT);
        for (DefinedPrefix definedPrefix : values()) {
            if (definedPrefix.name().equals(normalizedPrefix)) {
                return Optional.of(definedPrefix);
            }
        }
        return Optional.empty();
    }

    private PrefixMetadata metadata() {
        return MetadataHolder.BY_PREFIX.get(this);
    }

    private static Map<DefinedPrefix, PrefixMetadata> loadMetadata() {
        Map<DefinedPrefix, PrefixMetadata> metadataByPrefix =
                new EnumMap<>(DefinedPrefix.class);
        JsonObject root = ResourceJson.readObject(METADATA_RESOURCE);
        String rootContext = "root of " + METADATA_RESOURCE;
        ResourceJson.requireExactKeys(root, rootContext, "schemaVersion", "prefixes");
        int schemaVersion = ResourceJson.requireInteger(root, "schemaVersion", rootContext);
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "Unsupported schemaVersion " + schemaVersion + " in " + METADATA_RESOURCE);
        }

        JsonArray entries = ResourceJson.requireArray(root, "prefixes", rootContext);
        for (int i = 0; i < entries.size(); i++) {
            JsonElement element = entries.get(i);
            String context = "prefixes[" + i + "] in " + METADATA_RESOURCE;
            if (!element.isJsonObject()) {
                throw new IllegalStateException(context + " must be an object");
            }
            JsonObject entry = element.getAsJsonObject();
            ResourceJson.requireExactKeys(entry, context, "id", "descriptions", "period");

            String id = ResourceJson.requireString(entry, "id", context);
            DefinedPrefix prefix;
            try {
                prefix = DefinedPrefix.valueOf(id);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Unknown defined-prefix key in " + METADATA_RESOURCE + ": " + id, e);
            }

            PrefixMetadata previous = metadataByPrefix.put(
                    prefix, PrefixMetadata.from(entry, context));
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate defined-prefix key in " + METADATA_RESOURCE + ": " + prefix);
            }
        }

        for (DefinedPrefix prefix : values()) {
            if (!metadataByPrefix.containsKey(prefix)) {
                throw new IllegalStateException(
                        "Missing metadata for defined prefix " + prefix + " in " + METADATA_RESOURCE);
            }
        }
        return Collections.unmodifiableMap(metadataByPrefix);
    }

    private static final class MetadataHolder {
        private static final Map<DefinedPrefix, PrefixMetadata> BY_PREFIX = loadMetadata();
    }

    private static final class PrefixMetadata {
        private final String description;
        private final String traditionalChineseDescription;
        private final LocalDate hongKongBirthRegistrationStartDate;
        private final LocalDate hongKongBirthRegistrationEndDateExclusive;
        private final LocalDate firstIssueStartDate;
        private final LocalDate firstIssueEndDateExclusive;

        private PrefixMetadata(String description,
                               String traditionalChineseDescription,
                               DateBasis dateBasis,
                               LocalDate startDate,
                               LocalDate endDateExclusive) {
            if (description.isEmpty() || traditionalChineseDescription.isEmpty()) {
                throw new IllegalStateException("Prefix descriptions cannot be empty");
            }
            if (dateBasis == null && (startDate != null || endDateExclusive != null)) {
                throw new IllegalStateException("Undated prefix metadata cannot contain dates");
            }
            if (dateBasis != null && startDate == null) {
                throw new IllegalStateException("Dated prefix metadata requires a start date");
            }
            if (endDateExclusive != null && !endDateExclusive.isAfter(startDate)) {
                throw new IllegalStateException("Prefix metadata end date must be after start date");
            }

            this.description = description;
            this.traditionalChineseDescription = traditionalChineseDescription;
            this.hongKongBirthRegistrationStartDate = dateBasis == DateBasis.BIRTH_REGISTRATION
                    ? startDate : null;
            this.hongKongBirthRegistrationEndDateExclusive = dateBasis == DateBasis.BIRTH_REGISTRATION
                    ? endDateExclusive : null;
            this.firstIssueStartDate = dateBasis == DateBasis.FIRST_ISSUE ? startDate : null;
            this.firstIssueEndDateExclusive = dateBasis == DateBasis.FIRST_ISSUE
                    ? endDateExclusive : null;
        }

        private static PrefixMetadata from(JsonObject entry, String context) {
            JsonObject descriptions =
                    ResourceJson.requireObject(entry, "descriptions", context);
            String descriptionsContext = "descriptions in " + context;
            ResourceJson.requireExactKeys(
                    descriptions, descriptionsContext, "en", "zh-Hant");
            String description =
                    ResourceJson.requireString(descriptions, "en", descriptionsContext);
            String traditionalChineseDescription =
                    ResourceJson.requireString(descriptions, "zh-Hant", descriptionsContext);

            JsonObject period = ResourceJson.requireNullableObject(entry, "period", context);
            if (period == null) {
                return new PrefixMetadata(
                        description, traditionalChineseDescription, null, null, null);
            }

            String periodContext = "period in " + context;
            ResourceJson.requireExactKeys(
                    period, periodContext, "basis", "start", "endExclusive");
            String dateBasisValue =
                    ResourceJson.requireString(period, "basis", periodContext);
            DateBasis dateBasis;
            try {
                dateBasis = DateBasis.valueOf(dateBasisValue);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Unknown period basis in " + periodContext + ": " + dateBasisValue, e);
            }
            LocalDate startDate = parseDate(
                    ResourceJson.requireString(period, "start", periodContext),
                    "start",
                    periodContext);
            String endDateValue =
                    ResourceJson.requireNullableString(period, "endExclusive", periodContext);
            LocalDate endDateExclusive = endDateValue == null
                    ? null
                    : parseDate(endDateValue, "endExclusive", periodContext);
            return new PrefixMetadata(
                    description,
                    traditionalChineseDescription,
                    dateBasis,
                    startDate,
                    endDateExclusive);
        }

        private static LocalDate parseDate(String value, String fieldName, String context) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                throw new IllegalStateException(
                        "Invalid ISO date in field " + fieldName + " in " + context + ": " + value,
                        e);
            }
        }
    }
}
