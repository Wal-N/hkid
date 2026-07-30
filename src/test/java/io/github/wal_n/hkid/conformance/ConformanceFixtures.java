package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class ConformanceFixtures {
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private ConformanceFixtures() {
        throw new AssertionError("ConformanceFixtures cannot be instantiated");
    }

    static Stream<JsonObject> cases(String resourceName, String collectionName) {
        JsonObject root = readObject(resourceName);
        JsonElement schemaVersion = root.get("schemaVersion");
        if (!isSupportedSchemaVersion(schemaVersion)) {
            throw new IllegalStateException(
                    "Unsupported or missing schemaVersion in " + resourceName);
        }

        JsonElement collection = root.get(collectionName);
        if (collection == null || !collection.isJsonArray()) {
            throw new IllegalStateException(
                    "Missing fixture array " + collectionName + " in " + resourceName);
        }
        return StreamSupport.stream(collection.getAsJsonArray().spliterator(), false)
                .map(element -> {
                    if (!element.isJsonObject()) {
                        throw new IllegalStateException(
                                "Fixture entries in " + collectionName + " must be objects");
                    }
                    return element.getAsJsonObject();
                });
    }

    private static boolean isSupportedSchemaVersion(JsonElement schemaVersion) {
        if (schemaVersion == null
                || !schemaVersion.isJsonPrimitive()
                || !schemaVersion.getAsJsonPrimitive().isNumber()) {
            return false;
        }

        try {
            return schemaVersion.getAsBigDecimal().intValueExact()
                    == SUPPORTED_SCHEMA_VERSION;
        } catch (ArithmeticException | NumberFormatException e) {
            return false;
        }
    }

    static List<String> strings(JsonObject object, String memberName) {
        JsonArray values = object.getAsJsonArray(memberName);
        if (values == null) {
            throw new IllegalStateException("Missing fixture array " + memberName);
        }

        List<String> result = new ArrayList<>();
        for (JsonElement value : values) {
            result.add(value.getAsString());
        }
        return result;
    }

    static String nullableString(JsonObject object, String memberName) {
        JsonElement value = object.get(memberName);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static JsonObject readObject(String resourceName) {
        InputStream inputStream = ConformanceFixtures.class.getClassLoader()
                .getResourceAsStream("conformance/" + resourceName);
        if (inputStream == null) {
            throw new IllegalStateException("Missing conformance fixture " + resourceName);
        }

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                throw new IllegalStateException(
                        "Conformance fixture root must be an object: " + resourceName);
            }
            return root.getAsJsonObject();
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException(
                    "Unable to read conformance fixture " + resourceName, e);
        }
    }
}
