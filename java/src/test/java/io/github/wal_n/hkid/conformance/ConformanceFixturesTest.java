package io.github.wal_n.hkid.conformance;

import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConformanceFixturesTest {
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
