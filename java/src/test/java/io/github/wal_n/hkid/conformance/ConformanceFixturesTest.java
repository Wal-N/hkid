package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConformanceFixturesTest {
    @ParameterizedTest
    @ValueSource(strings = {"input", "checkDigit"})
    void rejectsMissingOrMisspelledRequiredInputFields(String memberName) {
        JsonObject fixture = new JsonObject();
        fixture.addProperty("input".equals(memberName) ? "checkDigit" : "input", "3");
        for (String misspelledName : new String[]{"", memberName + "Typo"}) {
            if (!misspelledName.isEmpty()) {
                fixture.addProperty(misspelledName, "A123456(3)");
            }
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> ConformanceFixtures.requireNullableString(fixture, memberName));
            assertEquals("Missing field " + memberName + " in conformance fixture",
                    exception.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"3", "3.5", "true", "false", "{}", "[]", "[\"3\"]", "[null]"})
    void rejectsNonStringRequiredInputFieldsWithoutCoercion(String json) {
        for (String memberName : new String[]{"input", "checkDigit"}) {
            JsonObject fixture = new JsonObject();
            fixture.add(memberName, JsonParser.parseString(json));
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> ConformanceFixtures.requireNullableString(fixture, memberName));
            assertEquals("Field " + memberName + " must be a string or null in conformance fixture",
                    exception.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"input", "checkDigit"})
    void acceptsExplicitNullInRequiredInputFields(String memberName) {
        JsonObject fixture = new JsonObject();
        fixture.add(memberName, JsonNull.INSTANCE);
        assertNull(ConformanceFixtures.requireNullableString(fixture, memberName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"input", "checkDigit"})
    void preservesStringsInRequiredInputFieldsWithoutNormalization(String memberName) {
        for (String value : new String[]{"", " ", "\t\n \u00a0\u3000", "3", "Ａ123456（３）"}) {
            JsonObject fixture = new JsonObject();
            fixture.addProperty(memberName, value);
            assertEquals(value, ConformanceFixtures.requireNullableString(fixture, memberName));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "fractional-schema-version.json",
            "unsupported-schema-version.json",
            "missing-schema-version.json",
            "string-schema-version.json"
    })
    void rejectsUnsupportedOrMalformedSchemaVersion(String resourceName) {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ConformanceFixtures.cases(
                        resourceName,
                        "cases"));

        assertEquals(
                "Unsupported or missing schemaVersion in " + resourceName,
                exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "\"0123\"", "1234", "true", "[]", "{}",
            "{\"code\":1234}",
            "{\"code\":\"0123\",\"extensionNumber\":0}",
            "{\"code\":\"0123\",\"extensionNumber\":false}",
            "{\"code\":\"0123\",\"extension\":\"2\"}"
    })
    void rejectsMalformedCommercialCodeFixturesInsteadOfCoercingOrDroppingValues(String json) {
        assertThrows(IllegalStateException.class,
                () -> ConformanceFixtures.commercialCode(JsonParser.parseString(json)));
    }
}
