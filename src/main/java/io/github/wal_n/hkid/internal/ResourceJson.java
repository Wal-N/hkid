package io.github.wal_n.hkid.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Strict UTF-8 JSON resource reader and schema helper for bundled reference data.
 */
public final class ResourceJson {
    private ResourceJson() {
        throw new AssertionError("ResourceJson cannot be instantiated");
    }

    public static JsonObject readObject(String resourceName) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON resource name cannot be empty");
        }

        InputStream inputStream = ResourceJson.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IllegalStateException("JSON resource not found: " + resourceName);
        }

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setStrictness(Strictness.STRICT);
            JsonElement root = JsonParser.parseReader(jsonReader);
            if (!root.isJsonObject()) {
                throw new IllegalStateException("JSON resource root must be an object: " + resourceName);
            }
            return root.getAsJsonObject();
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Failed to load JSON resource: " + resourceName, e);
        }
    }

    public static void requireExactKeys(JsonObject object, String context, String... expectedKeys) {
        Set<String> expected = new HashSet<>(Arrays.asList(expectedKeys));
        if (expected.size() != expectedKeys.length) {
            throw new IllegalArgumentException("Expected JSON keys contain duplicates");
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!expected.contains(entry.getKey())) {
                throw new IllegalStateException(
                        "Unexpected field " + entry.getKey() + " in " + context);
            }
        }
        for (String expectedKey : expectedKeys) {
            if (!object.has(expectedKey)) {
                throw new IllegalStateException(
                        "Missing field " + expectedKey + " in " + context);
            }
        }
    }

    public static JsonObject requireObject(JsonObject parent, String name, String context) {
        JsonElement element = requireNonNull(parent, name, context);
        if (!element.isJsonObject()) {
            throw wrongType(name, context, "an object");
        }
        return element.getAsJsonObject();
    }

    public static JsonObject requireNullableObject(JsonObject parent, String name, String context) {
        JsonElement element = requirePresent(parent, name, context);
        if (element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonObject()) {
            throw wrongType(name, context, "an object or null");
        }
        return element.getAsJsonObject();
    }

    public static JsonArray requireArray(JsonObject parent, String name, String context) {
        JsonElement element = requireNonNull(parent, name, context);
        if (!element.isJsonArray()) {
            throw wrongType(name, context, "an array");
        }
        return element.getAsJsonArray();
    }

    public static String requireString(JsonObject parent, String name, String context) {
        JsonElement element = requireNonNull(parent, name, context);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw wrongType(name, context, "a string");
        }
        return element.getAsString();
    }

    public static String requireNullableString(JsonObject parent, String name, String context) {
        JsonElement element = requirePresent(parent, name, context);
        if (element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw wrongType(name, context, "a string or null");
        }
        return element.getAsString();
    }

    public static int requireInteger(JsonObject parent, String name, String context) {
        JsonElement element = requireNonNull(parent, name, context);
        if (!element.isJsonPrimitive()) {
            throw wrongType(name, context, "an integer");
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        String value = primitive.getAsString();
        if (!primitive.isNumber() || !value.matches("-?(0|[1-9]\\d*)")) {
            throw wrongType(name, context, "an integer");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Integer field " + name + " is out of range in " + context, e);
        }
    }

    private static JsonElement requireNonNull(JsonObject parent, String name, String context) {
        JsonElement element = requirePresent(parent, name, context);
        if (element.isJsonNull()) {
            throw wrongType(name, context, "a non-null value");
        }
        return element;
    }

    private static JsonElement requirePresent(JsonObject parent, String name, String context) {
        if (!parent.has(name)) {
            throw new IllegalStateException("Missing field " + name + " in " + context);
        }
        return parent.get(name);
    }

    private static IllegalStateException wrongType(String name, String context, String expectedType) {
        return new IllegalStateException(
                "Field " + name + " must be " + expectedType + " in " + context);
    }
}
